package eu.dnetlib.iis.workflows.referenceextraction.project;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import eu.dnetlib.iis.common.IntegrationTest;
import eu.dnetlib.iis.common.AbstractOozieWorkflowTestCase;
import eu.dnetlib.iis.common.OozieWorkflowTestConfiguration;

/**
 * 
 * @author mhorst
 *
 */
@Category(IntegrationTest.class)
public class ProjectRefExtractionOutputTransformerWfTest extends AbstractOozieWorkflowTestCase {

    @Test
	public void testProjectToConceptWorkflow() throws Exception {
    	OozieWorkflowTestConfiguration wf = new OozieWorkflowTestConfiguration();
        wf.setTimeoutInSeconds(720);
        testWorkflow("eu/dnetlib/iis/workflows/referenceextraction/project/output_transformer/test", wf);
    }
}
