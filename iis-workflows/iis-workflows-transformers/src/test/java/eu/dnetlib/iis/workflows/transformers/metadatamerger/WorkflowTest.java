package eu.dnetlib.iis.workflows.transformers.metadatamerger;

import eu.dnetlib.iis.IntegrationTest;
import eu.dnetlib.iis.core.AbstractWorkflowTestCase;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * 
 * @author Dominika Tkaczyk
 *
 */
@Category(IntegrationTest.class)
public class WorkflowTest extends AbstractWorkflowTestCase {

    @Test
	public void testJoin() throws Exception {
        runWorkflow("eu/dnetlib/iis/workflows/transformers/metadatamerger/sampledataproducer/oozie_app");
    }

}
