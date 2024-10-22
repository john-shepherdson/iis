package eu.dnetlib.iis.wf.export.actionmanager.module;

import java.util.List;

import org.apache.avro.specific.SpecificRecord;

import eu.dnetlib.dhp.schema.action.AtomicAction;
import eu.dnetlib.dhp.schema.oaf.DataInfo;
import eu.dnetlib.dhp.schema.oaf.KeyValue;
import eu.dnetlib.dhp.schema.oaf.Relation;
import eu.dnetlib.iis.wf.export.actionmanager.cfg.StaticConfigurationProvider;

/**
 * Abstract {@link Relation} builder module.
 * 
 * @author mhorst
 *
 */
public abstract class AbstractRelationBuilderModule <S extends SpecificRecord> extends AbstractBuilderModule<S, Relation> {

    /**
     * Value to be exported in {@link Relation}{@link #collectedFromValue}.
     */
    private final String collectedFromValue;
 
    // ------------------------ CONSTRUCTORS --------------------------
    
    public AbstractRelationBuilderModule(Float trustLevelThreshold, String inferenceProvenance, String collectedFromValue) {
        super(trustLevelThreshold, inferenceProvenance);
        this.collectedFromValue = collectedFromValue;
    }
    
    // ----------------------------- LOGIC --------------------------------
    
    /**
     * Creates {@link Relation} initialized with basic metadata.
     * 
     * @param source          relation source
     * @param target          relation target
     * @param relType         relation type
     * @param subRelType      relation sub-type
     * @param relClass        relation class
     * @param confidenceLevel an input for trust level calculation, trust level set
     *                        to {@link StaticConfigurationProvider#ACTION_TRUST_0_9}
     *                        when confidence level is null
     * @param properties      relation properties
     * @throws TrustLevelThresholdExceededException when trust level threshold exceeded
     */
    protected Relation createRelation(String source, String target, String relType, String subRelType, String relClass,
            Float confidenceLevel, List<KeyValue> properties) throws TrustLevelThresholdExceededException {
        DataInfo dataInfo = confidenceLevel != null ? buildInference(confidenceLevel)
                : buildInferenceForTrustLevel(StaticConfigurationProvider.ACTION_TRUST_0_9);
        return BuilderModuleHelper.createRelation(source, target, relType, subRelType, relClass,
                properties, dataInfo, collectedFromValue);
    }
    
    /**
     * Creates an {@link AtomicAction} with {@link Relation} payload.
     * @param source relation source
     * @param target relation target
     * @param relType relation type
     * @param subRelType relation sub-type
     * @param relClass relation class
     * @param confidenceLevel confidence level to be used when calculating trust level
     * @throws TrustLevelThresholdExceededException when trust level threshold exceeded
     */
    protected AtomicAction<Relation> createAtomicActionWithRelation(String source, String target, String relType,
            String subRelType, String relClass, Float confidenceLevel) throws TrustLevelThresholdExceededException {
        return createAtomicActionWithRelation(source, target, relType, subRelType, relClass, confidenceLevel, null);
    }
    
    /**
     * Creates an {@link AtomicAction} with {@link Relation} payload.
     * @param source relation source
     * @param target relation target
     * @param relType relation type
     * @param subRelType relation sub-type
     * @param relClass relation class
     * @param confidenceLevel confidence level to be used when calculating trust level
     * @param properties relation properties
     */
    protected AtomicAction<Relation> createAtomicActionWithRelation(String source, String target, String relType,
            String subRelType, String relClass, List<KeyValue> properties) {
        AtomicAction<Relation> action = new AtomicAction<>();
        action.setClazz(Relation.class);
        try {
            action.setPayload(createRelation(source, target, relType, subRelType, relClass, null, properties));
        } catch (TrustLevelThresholdExceededException e) {
            throw new RuntimeException(e);
        }
        return action;
    }
    
    /**
     * Creates an {@link AtomicAction} with {@link Relation} payload.
     * @param source relation source
     * @param target relation target
     * @param relType relation type
     * @param subRelType relation sub-type
     * @param relClass relation class
     * @param properties relation properties
     * @throws TrustLevelThresholdExceededException when trust level threshold exceeded
     */
    protected AtomicAction<Relation> createAtomicActionWithRelation(String source, String target, String relType,
            String subRelType, String relClass, Float confidenceLevel, List<KeyValue> properties) throws TrustLevelThresholdExceededException {
        AtomicAction<Relation> action = new AtomicAction<>();
        action.setClazz(Relation.class);
        action.setPayload(createRelation(source, target, relType, subRelType, relClass, confidenceLevel, properties));
        return action;
    }

    
}
