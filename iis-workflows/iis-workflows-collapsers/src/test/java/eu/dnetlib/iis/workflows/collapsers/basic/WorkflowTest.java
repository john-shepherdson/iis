package eu.dnetlib.iis.workflows.collapsers.basic;

import eu.dnetlib.iis.IntegrationTest;
import eu.dnetlib.iis.core.AbstractWorkflowTestCase;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author Dominika Tkaczyk
 * @author Michal Oniszczuk
 */
@Category(IntegrationTest.class)
public class WorkflowTest extends AbstractWorkflowTestCase {

    @Test
    public void testDefaultWorkflow() throws Exception {
        runWorkflow("eu/dnetlib/iis/workflows/collapsers/basic_collapser/default/oozie_app");
    }

    @Test
    public void testDocumentTextWorkflow() throws Exception {
        runWorkflow("eu/dnetlib/iis/workflows/collapsers/basic_collapser/documenttext/oozie_app");
    }

}
