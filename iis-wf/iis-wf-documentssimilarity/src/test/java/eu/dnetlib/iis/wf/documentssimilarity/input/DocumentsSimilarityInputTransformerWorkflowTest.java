package eu.dnetlib.iis.wf.documentssimilarity.input;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import eu.dnetlib.iis.common.AbstractOozieWorkflowTestCase;
import eu.dnetlib.iis.common.IntegrationTest;
import eu.dnetlib.iis.common.OozieWorkflowTestConfiguration;

/**
 * 
 * @author Michal Oniszczuk (m.oniszczuk@icm.edu.pl)
 *
 */
@Category(IntegrationTest.class)
public class DocumentsSimilarityInputTransformerWorkflowTest extends AbstractOozieWorkflowTestCase {

    @Test
	public void testWorkflow() throws Exception {
    	OozieWorkflowTestConfiguration wf = new OozieWorkflowTestConfiguration();
        wf.setTimeoutInSeconds(720);
        testWorkflow("eu/dnetlib/iis/wf/documentssimilarity/input_transformer/test", wf);
    }

}
