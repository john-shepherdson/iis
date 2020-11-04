package eu.dnetlib.iis.wf.referenceextraction.project;

import eu.dnetlib.iis.common.AbstractOozieWorkflowTestCase;
import org.junit.jupiter.api.Test;

/**
 * 
 * @author Mateusz Kobos
 *
 */
public class ProjectRefExtractionWfTest extends AbstractOozieWorkflowTestCase {

    @Test
	public void testMainWorkflow() throws Exception {
    	testWorkflow("eu/dnetlib/iis/wf/referenceextraction/project/main/sampletest");
	}
    
    @Test
    public void testMainWorkflowWithoutProjects() throws Exception {
        testWorkflow("eu/dnetlib/iis/wf/referenceextraction/project/main/sampletest_without_projects");
    }

    @Test
	public void testMainWorkflowWithoutReferences() throws Exception {
        testWorkflow("eu/dnetlib/iis/wf/referenceextraction/project/main/sampletest_without_references");
	}

    @Test
	public void testMainWorkflowEmptyInput() throws Exception {
        testWorkflow("eu/dnetlib/iis/wf/referenceextraction/project/main/sampletest_empty_input");
	}
    
}
