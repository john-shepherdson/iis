package eu.dnetlib.iis.wf.importer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import static eu.dnetlib.iis.common.WorkflowRuntimeParameters.OOZIE_ACTION_OUTPUT_FILENAME;
import static org.junit.jupiter.api.Assertions.assertEquals;



/**
 * Utility methods useful for assertion validation.
 * @author mhorst
 *
 */
public final class VerificationUtils {

    
    // ------------------------------- CONSTRUCTORS -----------------------------
    
    private VerificationUtils() {}

    
    // ------------------------------- LOGIC ------------------------------------
    

    /**
     * Retrieves properties stored by entity exporter process.
     */
    public static Properties getStoredProperties() throws FileNotFoundException, IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(System.getProperty(OOZIE_ACTION_OUTPUT_FILENAME)));
        return properties;
    }
    
    /**
     * Verifies execution report.
     */
    public static void verifyReport(int expectedTotal, String counterName) throws FileNotFoundException, IOException {
        Properties reportProperties = getStoredProperties();
        assertEquals(expectedTotal, Integer.parseInt(reportProperties.getProperty(counterName)));
    }

    
}
