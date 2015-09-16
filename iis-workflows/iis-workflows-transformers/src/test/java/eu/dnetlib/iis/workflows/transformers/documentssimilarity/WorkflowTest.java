package eu.dnetlib.iis.workflows.transformers.documentssimilarity;

import eu.dnetlib.iis.IntegrationTest;
import eu.dnetlib.iis.core.AbstractWorkflowTestCase;
import eu.dnetlib.iis.core.WorkflowConfiguration;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * 
 * @author Michal Oniszczuk (m.oniszczuk@icm.edu.pl)
 *
 */
@Category(IntegrationTest.class)
public class WorkflowTest extends AbstractWorkflowTestCase {

    @Test
	public void testWorkflow() throws Exception {
        WorkflowConfiguration wf = new WorkflowConfiguration();
        wf.setTimeoutInSeconds(720);
        runWorkflow("eu/dnetlib/iis/workflows/transformers/documentssimilarity/sampledataproducer/oozie_app", wf);
    }

}
