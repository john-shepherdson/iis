package eu.dnetlib.iis.wf.export.actionmanager.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.protobuf.InvalidProtocolBufferException;

import eu.dnetlib.actionmanager.actions.AtomicAction;
import eu.dnetlib.actionmanager.common.Agent;
import eu.dnetlib.data.proto.KindProtos;
import eu.dnetlib.data.proto.OafProtos.Oaf;
import eu.dnetlib.data.proto.ResultProtos.Result.ExternalReference;
import eu.dnetlib.data.proto.TypeProtos.Type;
import eu.dnetlib.iis.referenceextraction.softwareurl.schemas.DocumentToSoftwareUrl;
import eu.dnetlib.iis.wf.export.actionmanager.module.DocumentToSoftwareUrlActionBuilderModuleFactory.DocumentToSoftwareUrlActionBuilderModule;

/**
 * @author mhorst
 *
 */
public class DocumentToSoftwareUrlActionBuilderModuleFactoryTest {

    private DocumentToSoftwareUrlActionBuilderModule module;

    private Float trustLevelThreshold = 0.5f;

    private String actionSetId = "someActionSetId";

    private Agent agent = new Agent("agentId", "agent name", Agent.AGENT_TYPE.service);

    private String docId = "documentId";

    private String softwareUrl = "https://github.com/openaire/iis";

    private float matchStrength = 0.9f;

    private DocumentToSoftwareUrl documentToSoftwareUrl = buildDocumentToSoftwareUrl(docId, softwareUrl, matchStrength);

    @Before
    public void initModule() {
        DocumentToSoftwareUrlActionBuilderModuleFactory factory = new DocumentToSoftwareUrlActionBuilderModuleFactory();
        module = (DocumentToSoftwareUrlActionBuilderModule) factory.instantiate(null, trustLevelThreshold, null);
    }

    // ----------------------- TESTS --------------------------

    @Test(expected = NullPointerException.class)
    public void test_build_null_object() throws Exception {
        // execute
        module.build(null, agent, actionSetId);
    }

    @Test(expected = NullPointerException.class)
    public void test_build_null_agent() throws Exception {
        // execute
        module.build(documentToSoftwareUrl, null, actionSetId);
    }

    @Test(expected = NullPointerException.class)
    public void test_build_null_actionsetid() throws Exception {
        // execute
        module.build(documentToSoftwareUrl, agent, null);
    }

    @Test(expected = TrustLevelThresholdExceededException.class)
    public void test_build_below_threshold() throws Exception {
        // given
        DocumentToSoftwareUrl matchedOrgBelowThreshold = buildDocumentToSoftwareUrl(docId, softwareUrl, 0.4f);
        // execute
        module.build(matchedOrgBelowThreshold, agent, actionSetId);
    }

    @Test
    public void test_build() throws Exception {
        // execute
        List<AtomicAction> actions = module.build(documentToSoftwareUrl, agent, actionSetId);

        // assert
        assertNotNull(actions);
        assertEquals(1, actions.size());
        AtomicAction action = actions.get(0);
        assertNotNull(action);
        assertNotNull(action.getRowKey());
        assertEquals(actionSetId, action.getRawSet());
        assertEquals(docId, action.getTargetRowKey());
        assertEquals(Type.result.toString(), action.getTargetColumnFamily());
        assertOaf(action.getTargetValue());
    }

    // ----------------------- PRIVATE --------------------------

    private static DocumentToSoftwareUrl buildDocumentToSoftwareUrl(String docId, String softUrl, float confidenceLevel) {
        DocumentToSoftwareUrl.Builder builder = DocumentToSoftwareUrl.newBuilder();
        builder.setDocumentId(docId);
        builder.setSoftwareUrl(softUrl);
        builder.setConfidenceLevel(confidenceLevel);
        return builder.build();
    }

    private void assertOaf(byte[] oafBytes) throws InvalidProtocolBufferException {
        assertNotNull(oafBytes);
        Oaf.Builder oafBuilder = Oaf.newBuilder();
        oafBuilder.mergeFrom(oafBytes);
        Oaf oaf = oafBuilder.build();
        assertNotNull(oaf);

        assertTrue(KindProtos.Kind.entity == oaf.getKind());
        assertNotNull(oaf.getEntity());
        assertEquals(docId, oaf.getEntity().getId());
        assertNotNull(oaf.getEntity().getResult());
        assertEquals(1, oaf.getEntity().getResult().getExternalReferenceList().size());
        ExternalReference externalReference = oaf.getEntity().getResult().getExternalReferenceList().get(0);
        assertNotNull(externalReference);
        assertEquals(softwareUrl, externalReference.getUrl());
        
        assertNotNull(externalReference.getQualifier());
        // TODO check other fields that are currently not set

        assertNotNull(externalReference.getDataInfo());

        float normalizedTrust = matchStrength * module.getConfidenceToTrustLevelNormalizationFactor();
        assertEquals(normalizedTrust, Float.parseFloat(externalReference.getDataInfo().getTrust()), 0.0001);
    }
}
