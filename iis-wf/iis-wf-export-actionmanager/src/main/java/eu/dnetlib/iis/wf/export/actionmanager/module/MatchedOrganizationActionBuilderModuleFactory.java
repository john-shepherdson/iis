package eu.dnetlib.iis.wf.export.actionmanager.module;

import java.util.List;

import org.apache.hadoop.conf.Configuration;

import com.google.common.base.Preconditions;

import datafu.com.google.common.collect.Lists;
import eu.dnetlib.actionmanager.actions.AtomicAction;
import eu.dnetlib.actionmanager.common.Agent;
import eu.dnetlib.data.mapreduce.util.OafDecoder;
import eu.dnetlib.data.proto.KindProtos.Kind;
import eu.dnetlib.data.proto.OafProtos.Oaf;
import eu.dnetlib.data.proto.OafProtos.OafRel;
import eu.dnetlib.data.proto.RelTypeProtos.RelType;
import eu.dnetlib.data.proto.RelTypeProtos.SubRelType;
import eu.dnetlib.data.proto.ResultOrganizationProtos.ResultOrganization;
import eu.dnetlib.data.proto.ResultOrganizationProtos.ResultOrganization.Affiliation;
import eu.dnetlib.iis.wf.affmatching.model.MatchedOrganization;

/**
 * {@link MatchedOrganization} action builder factory module.
 * 
 * @author mhorst
 *
 */
public class MatchedOrganizationActionBuilderModuleFactory implements ActionBuilderFactory<MatchedOrganization> {

    private static final String REL_CLASS_IS_AFFILIATED_WITH = Affiliation.RelName.isAffiliatedWith.toString();

    private static final String SEMANTIC_SCHEME_DNET_RELATIONS_RESULT_ORG = "dnet:result_organization_relations";

    private static final AlgorithmName algorithmName = AlgorithmName.document_affiliations;

    /**
     * {@link MatchedOrganization} action builder module.
     *
     */
    class MatchedOrganizationActionBuilderModule extends AbstractBuilderModule implements ActionBuilderModule<MatchedOrganization> {

        /**
         * @param predefinedTrust default trust level
         * @param trustLevelThreshold trust level threshold or null when all records should be exported
         */
        public MatchedOrganizationActionBuilderModule(String predefinedTrust, Float trustLevelThreshold) {
            super(predefinedTrust, trustLevelThreshold, algorithmName);
        }

        @Override
        public List<AtomicAction> build(MatchedOrganization object, Agent agent, String actionSetId)
                throws TrustLevelThresholdExceededException {
            Preconditions.checkNotNull(object);
            Preconditions.checkNotNull(agent);
            Preconditions.checkNotNull(actionSetId);
            String docId = object.getDocumentId().toString();
            String orgId = object.getOrganizationId().toString();
            Oaf.Builder oafBuilder = Oaf.newBuilder();
            oafBuilder.setKind(Kind.relation);
            OafRel.Builder relBuilder = OafRel.newBuilder();
            relBuilder.setChild(false);
            relBuilder.setRelType(RelType.resultOrganization);
            relBuilder.setSubRelType(SubRelType.affiliation);
            relBuilder.setRelClass(REL_CLASS_IS_AFFILIATED_WITH);
            relBuilder.setSource(docId);
            relBuilder.setTarget(orgId);
            ResultOrganization.Builder resOrgBuilder = ResultOrganization.newBuilder();
            Affiliation.Builder affBuilder = Affiliation.newBuilder();
            affBuilder.setRelMetadata(buildRelMetadata(SEMANTIC_SCHEME_DNET_RELATIONS_RESULT_ORG, REL_CLASS_IS_AFFILIATED_WITH));
            resOrgBuilder.setAffiliation(affBuilder.build());
            relBuilder.setResultOrganization(resOrgBuilder.build());
            oafBuilder.setRel(relBuilder.build());
            oafBuilder.setDataInfo(buildInference(object.getMatchStrength()));
            oafBuilder.setLastupdatetimestamp(System.currentTimeMillis());
            Oaf oaf = oafBuilder.build();
            return Lists.newArrayList(actionFactory.createAtomicAction(actionSetId, agent, docId,
                    OafDecoder.decode(oaf).getCFQ(), orgId, oaf.toByteArray()));
        }
    }

    @Override
    public ActionBuilderModule<MatchedOrganization> instantiate(String predefinedTrust, Float trustLevelThreshold,
            Configuration config) {
        return new MatchedOrganizationActionBuilderModule(predefinedTrust, trustLevelThreshold);
    }

    @Override
    public AlgorithmName getAlgorithName() {
        return algorithmName;
    }
}
