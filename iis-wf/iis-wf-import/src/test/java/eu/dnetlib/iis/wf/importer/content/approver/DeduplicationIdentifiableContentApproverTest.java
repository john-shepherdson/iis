package eu.dnetlib.iis.wf.importer.content.approver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author mhorst
 *
 */
public class DeduplicationIdentifiableContentApproverTest {

    private final byte[] content = "content".getBytes();
    
    private final String id = "id";
    
    DeduplicationIdentifiableContentApprover approver = new DeduplicationIdentifiableContentApprover();

    // ------------------------------------ TESTS ------------------------------------
    
    @Test
    public void testApproveOnTheSameId() throws Exception {
        assertTrue(approver.approve(id, content));
        assertFalse(approver.approve(id, content));
    }
    
    @Test
    public void testApproveOnDifferentIds() throws Exception {
        assertTrue(approver.approve(id, content));
        assertTrue(approver.approve("differentId", content));
    }
    
}
