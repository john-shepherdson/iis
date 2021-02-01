package eu.dnetlib.iis.wf.referenceextraction.patent;

import java.util.Map;

import eu.dnetlib.iis.wf.referenceextraction.FacadeContentRetriever;
import eu.dnetlib.iis.wf.referenceextraction.FacadeContentRetrieverResponse;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.Optional;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import eu.dnetlib.iis.audit.schemas.Fault;
import eu.dnetlib.iis.common.cache.CacheMetadataManagingProcess;
import eu.dnetlib.iis.common.cache.DocumentTextCacheStorageUtils;
import eu.dnetlib.iis.common.cache.DocumentTextCacheStorageUtils.CacheRecordType;
import eu.dnetlib.iis.common.cache.DocumentTextCacheStorageUtils.CachedStorageJobParameters;
import eu.dnetlib.iis.common.cache.DocumentTextCacheStorageUtils.OutputPaths;
import eu.dnetlib.iis.common.fault.FaultUtils;
import eu.dnetlib.iis.common.java.io.HdfsUtils;
import eu.dnetlib.iis.common.lock.LockManager;
import eu.dnetlib.iis.common.lock.LockManagerUtils;
import eu.dnetlib.iis.common.report.ReportEntryFactory;
import eu.dnetlib.iis.common.schemas.ReportEntry;
import eu.dnetlib.iis.common.spark.JavaSparkContextFactory;
import eu.dnetlib.iis.metadataextraction.schemas.DocumentText;
import eu.dnetlib.iis.referenceextraction.patent.schemas.ImportedPatent;
import eu.dnetlib.iis.wf.importer.facade.ServiceFacadeUtils;
import pl.edu.icm.sparkutils.avro.SparkAvroLoader;
import pl.edu.icm.sparkutils.avro.SparkAvroSaver;
import scala.Tuple2;

/**
 * Job responsible for retrieving full patent metadata via {@link OpenPatentWebServiceFacade} based on
 * {@link ImportedPatent} input. Stores results in cache for further usage.
 */
public class PatentMetadataRetrieverJob {
    
    private static final String COUNTER_PROCESSED_TOTAL = "processing.referenceExtraction.patent.retrieval.processed.total";
    
    private static final String COUNTER_PROCESSED_FAULT = "processing.referenceExtraction.patent.retrieval.processed.fault";
    
    private static final String COUNTER_FROMCACHE_TOTAL = "processing.referenceExtraction.patent.retrieval.fromCache.total";

    private static final SparkAvroLoader avroLoader = new SparkAvroLoader();
    private static final SparkAvroSaver avroSaver = new SparkAvroSaver();

    //------------------------ LOGIC --------------------------

    public static void main(String[] args) throws Exception {
        JobParameters params = new JobParameters();
        JCommander jcommander = new JCommander(params);
        jcommander.parse(args);
        
        try (JavaSparkContext sc = JavaSparkContextFactory.withConfAndKryo(new SparkConf())) {
            
            Configuration hadoopConf = sc.hadoopConfiguration();
            
            HdfsUtils.remove(hadoopConf, params.getOutputPath());
            HdfsUtils.remove(hadoopConf, params.getOutputFaultPath());
            HdfsUtils.remove(hadoopConf, params.getOutputReportPath());
            
            LockManager lockManager = LockManagerUtils.instantiateLockManager(params.getLockManagerFactoryClassName(),
                    hadoopConf);

            FacadeContentRetriever<ImportedPatent, String> contentRetriever = ServiceFacadeUtils
                    .instantiate(params.patentServiceFacadeFactoryClassName, params.patentServiceFacadeParams);

            JavaRDD<ImportedPatent> importedPatents = avroLoader.loadJavaRDD(sc, params.inputPath, ImportedPatent.class);

            final Path cacheRootDir = new Path(params.getCacheRootDir());
            CacheMetadataManagingProcess cacheManager = new CacheMetadataManagingProcess();

            String existingCacheId = cacheManager.getExistingCacheId(hadoopConf, cacheRootDir);

            // skipping already extracted
            JavaRDD<DocumentText> cachedSources = DocumentTextCacheStorageUtils.getRddOrEmpty(sc, avroLoader, cacheRootDir,
                    existingCacheId, CacheRecordType.text, DocumentText.class);
            // caching: will be written in new cache version and output
            cachedSources.cache();

            JavaPairRDD<CharSequence, DocumentText> cacheById = cachedSources.mapToPair(x -> new Tuple2<>(x.getId(), x));
            JavaPairRDD<CharSequence, ImportedPatent> inputById = importedPatents.mapToPair(x -> new Tuple2<>(getId(x), x));
            JavaPairRDD<CharSequence, Tuple2<ImportedPatent, Optional<DocumentText>>> inputJoinedWithCache = inputById.leftOuterJoin(cacheById);

            JavaRDD<ImportedPatent> toBeProcessed = inputJoinedWithCache.filter(x -> !x._2._2.isPresent()).values().map(x -> x._1);
            JavaRDD<DocumentText> entitiesReturnedFromCache = inputJoinedWithCache.filter(x -> x._2._2.isPresent()).values().map(x -> x._2.get());
            entitiesReturnedFromCache.cache();

            JavaPairRDD<CharSequence, FacadeContentRetrieverResponse<String>> returnedFromRemoteService =
                    retrieveFromRemoteService(toBeProcessed, contentRetriever);
            returnedFromRemoteService.cache();

            JavaRDD<DocumentText> retrievedPatentMetaToBeCached = mapContentRetrieverResponsesToDocumentTextForCache(
                    returnedFromRemoteService);
            JavaRDD<Fault> faultsToBeCached = mapContentRetrieverResponsesToFaultForCache(
                    returnedFromRemoteService);

            if (!retrievedPatentMetaToBeCached.isEmpty()) {
                // storing new cache entry
                JavaRDD<Fault> cachedFaults = DocumentTextCacheStorageUtils.getRddOrEmpty(sc, avroLoader, cacheRootDir,
                        existingCacheId, CacheRecordType.fault, Fault.class);

                DocumentTextCacheStorageUtils.storeInCache(avroSaver, cachedSources.union(retrievedPatentMetaToBeCached),
                        cachedFaults.union(faultsToBeCached), cacheRootDir, lockManager, cacheManager, hadoopConf,
                        params.numberOfEmittedFiles);
            }

            JavaRDD<DocumentText> retrievedPatentMeta = mapContentRetrieverResponsesToDocumentTextForOutput(
                    returnedFromRemoteService);
            JavaRDD<Fault> faults = mapContentRetrieverResponsesToFaultForOutput(
                    returnedFromRemoteService);

            JavaRDD<DocumentText> entitiesToBeWritten;
            if (!retrievedPatentMeta.isEmpty()) {
                // merging final results
                entitiesToBeWritten = entitiesReturnedFromCache.union(retrievedPatentMeta);
            } else {
                entitiesToBeWritten = entitiesReturnedFromCache;
            }

            // store final results
            storeInOutput(entitiesToBeWritten,
                    //notice: we do not propagate faults from cache, only new faults are written
                    faults, generateReportEntries(sc, entitiesReturnedFromCache, retrievedPatentMeta, faults),
                    new OutputPaths(params), params.numberOfEmittedFiles);
        }
    }

    //------------------------ PRIVATE --------------------------

    private static JavaPairRDD<CharSequence, FacadeContentRetrieverResponse<String>> retrieveFromRemoteService(
            JavaRDD<ImportedPatent> importedPatent,
            FacadeContentRetriever<ImportedPatent, String> contentRetriever) {
        return importedPatent
                .repartition(1)// limiting number of partitions to 1 in order to run EPO retrieval within a single task
                .mapToPair(patent -> new Tuple2<>(getId(patent), contentRetriever.retrieveContent(patent)));
    }

    private static JavaRDD<DocumentText> mapContentRetrieverResponsesToDocumentTextForCache(
            JavaPairRDD<CharSequence, FacadeContentRetrieverResponse<String>> returnedFromRemoteService) {
        return returnedFromRemoteService
                .filter(e -> !e._2.getClass().equals(FacadeContentRetrieverResponse.TransientFailure.class))
                .map(e -> {
                    if (e._2.getClass().equals(FacadeContentRetrieverResponse.Success.class)) {
                        return DocumentText.newBuilder().setId(e._1).setText(e._2.getContent()).build();
                    }
                    return DocumentText.newBuilder().setId(e._1).setText("").build();
                });
    }

    private static JavaRDD<Fault> mapContentRetrieverResponsesToFaultForCache(
            JavaPairRDD<CharSequence, FacadeContentRetrieverResponse<String>> returnedFromRemoteService) {
        return returnedFromRemoteService
                .filter(e -> e._2.getClass().equals(FacadeContentRetrieverResponse.PersistentFailure.class))
                .map(e -> FaultUtils.exceptionToFault(e._1, e._2.getException(), null));
    }

    private static JavaRDD<DocumentText> mapContentRetrieverResponsesToDocumentTextForOutput(
            JavaPairRDD<CharSequence, FacadeContentRetrieverResponse<String>> returnedFromRemoteService) {
        return returnedFromRemoteService
                .map(e -> {
                    if (e._2.getClass().equals(FacadeContentRetrieverResponse.Success.class)) {
                        return DocumentText.newBuilder().setId(e._1).setText(e._2.getContent()).build();
                    }
                    return DocumentText.newBuilder().setId(e._1).setText("").build();
                });
    }

    private static JavaRDD<Fault> mapContentRetrieverResponsesToFaultForOutput(
            JavaPairRDD<CharSequence, FacadeContentRetrieverResponse<String>> returnedFromRemoteService) {
        return returnedFromRemoteService
                .filter(e -> FacadeContentRetrieverResponse.Failure.class.isAssignableFrom(e._2.getClass()))
                .map(e -> FaultUtils.exceptionToFault(e._1, e._2.getException(), null));
    }

    private static JavaRDD<ReportEntry> generateReportEntries(JavaSparkContext sparkContext, 
            JavaRDD<DocumentText> fromCacheEntities, JavaRDD<DocumentText> processedEntities, JavaRDD<Fault> processedFaults) {
        
        ReportEntry fromCacheEntitiesCounter = ReportEntryFactory.createCounterReportEntry(COUNTER_FROMCACHE_TOTAL, fromCacheEntities.count());
        ReportEntry processedEntitiesCounter = ReportEntryFactory.createCounterReportEntry(COUNTER_PROCESSED_TOTAL, processedEntities.count());
        ReportEntry processedFaultsCounter = ReportEntryFactory.createCounterReportEntry(COUNTER_PROCESSED_FAULT, processedFaults.count());
        
        return sparkContext.parallelize(Lists.newArrayList(fromCacheEntitiesCounter, processedEntitiesCounter, processedFaultsCounter));
    }
    
    private static void storeInOutput(JavaRDD<DocumentText> retrievedPatentMeta, 
            JavaRDD<Fault> faults, JavaRDD<ReportEntry> reports, OutputPaths outputPaths, int numberOfEmittedFiles) {
        avroSaver.saveJavaRDD(retrievedPatentMeta.repartition(numberOfEmittedFiles), DocumentText.SCHEMA$, outputPaths.getResult());
        avroSaver.saveJavaRDD(faults.repartition(numberOfEmittedFiles), Fault.SCHEMA$, outputPaths.getFault());
        avroSaver.saveJavaRDD(reports.repartition(1), ReportEntry.SCHEMA$, outputPaths.getReport());
    }

    private static CharSequence getId(ImportedPatent patent) {
        return patent.getApplnNr();
    }

    @Parameters(separators = "=")
    private static class JobParameters extends CachedStorageJobParameters {
        @Parameter(names = "-inputPath", required = true)
        private String inputPath;

        @Parameter(names = "-numberOfEmittedFiles", required = true)
        private int numberOfEmittedFiles;

        @Parameter(names = "-patentServiceFacadeFactoryClassName", required = true)
        private String patentServiceFacadeFactoryClassName;

        @DynamicParameter(names = "-D", description = "dynamic parameters related to patent service facade", required = false)
        private Map<String, String> patentServiceFacadeParams = Maps.newHashMap();
    }
    
}
