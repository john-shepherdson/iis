package eu.dnetlib.iis.wf.export.actionmanager.module;

import static eu.dnetlib.iis.wf.export.actionmanager.ExportWorkflowRuntimeParameters.EXPORT_DOCUMENTSSIMILARITY_THRESHOLD;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.Logger;

import eu.dnetlib.dhp.schema.action.AtomicAction;
import eu.dnetlib.dhp.schema.oaf.KeyValue;
import eu.dnetlib.dhp.schema.oaf.Relation;
import eu.dnetlib.iis.common.WorkflowRuntimeParameters;
import eu.dnetlib.iis.documentssimilarity.schemas.DocumentSimilarity;
import eu.dnetlib.iis.wf.export.actionmanager.cfg.StaticConfigurationProvider;

/**
 * {@link DocumentSimilarity} based action builder module.
 * 
 * @author mhorst
 *
 */
public class DocumentSimilarityActionBuilderModuleFactory extends AbstractActionBuilderFactory<DocumentSimilarity, Relation> {

    private static final String REL_TYPE = "resultResult";
    
    private static final String SUBREL_TYPE = "similarity";
    
    private static final String REL_CLASS_IS_AMONG_TOP_N = "isAmongTopNSimilarDocuments";
    
    private static final String REL_CLASS_HAS_AMONG_TOP_N = "hasAmongTopNSimilarDocuments";
    
    private static final String REL_PROPERTY_SIMILARITY_LEVEL = "similarityLevel";

    
    private static final Logger log = Logger.getLogger(DocumentSimilarityActionBuilderModuleFactory.class);

    // ------------------------ CONSTRUCTORS --------------------------

    public DocumentSimilarityActionBuilderModuleFactory() {
        super(AlgorithmName.document_similarities_standard);
    }

    // ------------------------ LOGIC ---------------------------------
    
    @Override
    public ActionBuilderModule<DocumentSimilarity, Relation> instantiate(Configuration config) {
        Float similarityThreshold = null;
        String thresholdStr = WorkflowRuntimeParameters.getParamValue(EXPORT_DOCUMENTSSIMILARITY_THRESHOLD, config);
        if (thresholdStr != null) {
            similarityThreshold = Float.valueOf(thresholdStr);
            log.info("setting documents similarity exporter threshold to: " + similarityThreshold);
        }
        return new DocumentSimilarityActionBuilderModule(provideTrustLevelThreshold(config), similarityThreshold);
    }
    
    // ------------------------ INNER CLASS --------------------------

    class DocumentSimilarityActionBuilderModule extends AbstractBuilderModule<DocumentSimilarity, Relation> {

        
        private final Float similarityThreshold;

        // ------------------------ CONSTRUCTORS --------------------------

        /**
         * @param trustLevelThreshold trust level threshold or null when all records should be exported
         * @param similarityThreshold similarity threshold, skipped when null
         */
        public DocumentSimilarityActionBuilderModule(Float trustLevelThreshold, Float similarityThreshold) {
            super(trustLevelThreshold, buildInferenceProvenance());
            this.similarityThreshold = similarityThreshold;
        }

        // ------------------------ LOGIC --------------------------

        @Override
        public List<AtomicAction<Relation>> build(DocumentSimilarity object) {
            // checking similarity threshold if set
            if (similarityThreshold != null && object.getSimilarity() != null
                    && object.getSimilarity() < similarityThreshold) {
                return Collections.emptyList();
            } else {
                String docId = object.getDocumentId().toString();
                String otherDocId = object.getOtherDocumentId().toString();
                return Arrays.asList(createAction(docId, otherDocId, object.getSimilarity(), REL_CLASS_HAS_AMONG_TOP_N),
                        createAction(otherDocId, docId, object.getSimilarity(), REL_CLASS_IS_AMONG_TOP_N));
            }
        }

        // ------------------------ PRIVATE --------------------------

        /**
         * Creates similarity related actions.
         */
        private AtomicAction<Relation> createAction(String source, String target, float score, String relClass) {
            AtomicAction<Relation> action = new AtomicAction<>();
            action.setClazz(Relation.class);
            
            Relation relation = new Relation();
            relation.setSource(source);
            relation.setTarget(target);
            relation.setRelType(REL_TYPE);
            relation.setSubRelType(SUBREL_TYPE);
            relation.setRelClass(relClass);
            relation.setDataInfo(buildInferenceForTrustLevel(StaticConfigurationProvider.ACTION_TRUST_0_9));
            relation.setLastupdatetimestamp(System.currentTimeMillis());
            throw new RuntimeException("awaiting Relation model extension required to export score");
            // FIXME extend Relation model to export similarity score, replace throwing Exception with the two lines below
            // relation.setProperties(Collections.singletonList(buildSimilarityLevel(score)));
            // action.setPayload(relation);
            // return action;
        }

        private KeyValue buildSimilarityLevel(float score) {
            KeyValue keyValue = new KeyValue();
            keyValue.setKey(REL_PROPERTY_SIMILARITY_LEVEL);
            keyValue.setValue(BuilderModuleHelper.getDecimalFormat().format(score));
            return keyValue;
        }
        
    }

}
