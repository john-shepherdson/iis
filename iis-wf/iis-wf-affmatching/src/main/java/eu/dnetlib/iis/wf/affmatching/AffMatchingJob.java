package eu.dnetlib.iis.wf.affmatching;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import eu.dnetlib.iis.common.java.io.HdfsUtils;
import eu.dnetlib.iis.common.spark.JavaSparkContextFactory;
import eu.dnetlib.iis.wf.affmatching.match.AffOrgMatcher;
import eu.dnetlib.iis.wf.affmatching.orgalternativenames.AffMatchOrganizationAltNameFiller;
import eu.dnetlib.iis.wf.affmatching.orgalternativenames.CsvOrganizationAltNamesDictionaryFactory;
import eu.dnetlib.iis.wf.affmatching.orgalternativenames.OrganizationAltNameConst;
import eu.dnetlib.iis.wf.affmatching.read.AffiliationReader;
import eu.dnetlib.iis.wf.affmatching.read.AffiliationReaderFactory;
import eu.dnetlib.iis.wf.affmatching.read.IisAffiliationReader;
import eu.dnetlib.iis.wf.affmatching.read.IisOrganizationReader;
import eu.dnetlib.iis.wf.affmatching.write.IisAffMatchResultWriter;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;

import java.io.IOException;
import java.util.*;

import static com.google.common.collect.ImmutableList.of;
import static eu.dnetlib.iis.wf.affmatching.match.DocOrgRelationMatcherFactory.createDocOrgRelationMatcher;
import static eu.dnetlib.iis.wf.affmatching.match.FirstWordsHashBucketMatcherFactory.createNameFirstWordsHashBucketMatcher;
import static eu.dnetlib.iis.wf.affmatching.match.MainSectionHashBucketMatcherFactory.*;

/**
 * Job matching affiliations with organizations.
 * 
 * @param inputAvroOrgPath path to directory with avro files containing organizations 
 * 
 * @author Łukasz Dumiszewski
 */
public class AffMatchingJob {

    public static final String PARAM_AFF_MATCHING_SERVICE_AFFILIATION_READER_FACTORY_CLASSNAME =
            "aff_matching_service_affiliation_reader_factory_classname";

    private static CsvOrganizationAltNamesDictionaryFactory alternativeNamesFactory = new CsvOrganizationAltNamesDictionaryFactory();

    //------------------------ LOGIC --------------------------
    
    public static void main(String[] args) throws Exception {
        AffMatchingJobParameters params = new AffMatchingJobParameters();
        JCommander jcommander = new JCommander(params);
        jcommander.parse(args);

        try (JavaSparkContext sc = JavaSparkContextFactory.withConfAndKryo(new SparkConf())) {
            HdfsUtils.remove(sc.hadoopConfiguration(), params.outputAvroPath);
            HdfsUtils.remove(sc.hadoopConfiguration(), params.outputAvroReportPath);

            AffMatchingService affMatchingService = createAffMatchingService(sc, params);

            affMatchingService
                    .matchAffiliations(sc,
                            params.inputAvroAffPath,
                            params.inputAvroOrgPath,
                            params.outputAvroPath,
                            params.outputAvroReportPath);
        }
    }

    //------------------------ PRIVATE --------------------------

    @Parameters(separators = "=")
    private static class AffMatchingJobParameters {

        @Parameter(names = "-inputAvroOrgPath", required = true, description="path to directory with avro files containing organizations")
        private String inputAvroOrgPath;
        
        @Parameter(names = "-inputAvroAffPath", required = true, description="path to directory with avro files containing affiliations")
        private String inputAvroAffPath;
        
        @Parameter(names = "-inputAvroInferredDocProjPath", required = true, description="path to directory with avro files containing inferred document to project relations")
        private String inputAvroInferredDocProjPath;
        
        @Parameter(names = "-inputAvroDocProjPath", required = true, description="path to directory with avro files containing document to project relations")
        private String inputAvroDocProjPath;
        
        @Parameter(names = "-inputDocProjConfidenceThreshold", description="minimal confidence level for document to project relations (no limit by default)")
        private Float inputDocProjConfidenceThreshold = null;
        
        @Parameter(names = "-inputAvroProjOrgPath", required = true, description="path to directory with avro files containing project to organization relations")
        private String inputAvroProjOrgPath;
        
        @Parameter(names = "-numberOfEmittedFiles", required = true)
        private int numberOfEmittedFiles;
        
        @Parameter(names = "-outputAvroPath", required = true)
        private String outputAvroPath;
        
        @Parameter(names = "-outputAvroReportPath", required = true, description="path to a directory with the execution result report")
        private String outputAvroReportPath;

        @DynamicParameter(names = "-D", description = "dynamic parameters related to quality tests")
        private Map<String, String> parameters = new HashMap<>();
    }

    private static AffMatchingService createAffMatchingService(JavaSparkContext sparkContext, AffMatchingJobParameters params) throws Exception {
        AffMatchingService affMatchingService = new AffMatchingService();
        affMatchingService.setNumberOfEmittedFiles(params.numberOfEmittedFiles);
        
        // readers
        affMatchingService.setAffiliationReader(createAffiliationReader(params));
        affMatchingService.setOrganizationReader(new IisOrganizationReader());

        // writer
        affMatchingService.setAffMatchResultWriter(new IisAffMatchResultWriter());

        // matchers
        AffOrgMatcher docOrgRelationMatcher =
                createDocOrgRelationMatcher(sparkContext, params.inputAvroDocProjPath, params.inputAvroInferredDocProjPath, params.inputAvroProjOrgPath, params.inputDocProjConfidenceThreshold);
        AffOrgMatcher mainSectionHashBucketMatcher = createNameMainSectionHashBucketMatcher();
        AffOrgMatcher alternativeNameMainSectionHashBucketMatcher = createAlternativeNameMainSectionHashBucketMatcher();
        AffOrgMatcher shortNameMainSectionHashBucketMatcher = createShortNameMainSectionHashBucketMatcher();
        AffOrgMatcher firstWordsHashBucketMatcher = createNameFirstWordsHashBucketMatcher();

        affMatchingService.setAffOrgMatchers(of(docOrgRelationMatcher, mainSectionHashBucketMatcher, alternativeNameMainSectionHashBucketMatcher, shortNameMainSectionHashBucketMatcher, firstWordsHashBucketMatcher));

        AffMatchOrganizationAltNameFiller altNameFiller = createAffMatchOrganizationAltNameFiller();
        affMatchingService.setAffMatchOrganizationAltNameFiller(altNameFiller);
        
        return affMatchingService;
    }

    private static AffiliationReader createAffiliationReader(AffMatchingJobParameters params) throws Exception {
        String className = params.parameters.get(PARAM_AFF_MATCHING_SERVICE_AFFILIATION_READER_FACTORY_CLASSNAME);
        if (Objects.isNull(className)) {
            return new IisAffiliationReader();
        }
        return ((AffiliationReaderFactory) Class.forName(className).getConstructor().newInstance()).create();
    }

    private static AffMatchOrganizationAltNameFiller createAffMatchOrganizationAltNameFiller() throws IOException {
        AffMatchOrganizationAltNameFiller altNameFiller = new AffMatchOrganizationAltNameFiller();
        
        List<Set<String>> alternativeNamesDictionary = alternativeNamesFactory.createAlternativeNamesDictionary(OrganizationAltNameConst.CLASSPATH_ALTERNATIVE_NAMES_CSV_FILES);
        altNameFiller.setAlternativeNamesDictionary(alternativeNamesDictionary);
        
        return altNameFiller;
    }
}
