package eu.dnetlib.iis.common.java;

import org.apache.hadoop.conf.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author mhorst
 *
 */
public class ProcessUtilsTest {

    String paramName = "paramName";
    
    Configuration hadoopConf;
    
    Map<String, String> parameters;
    
    @BeforeEach
    public void init() {
        hadoopConf = new Configuration();
        parameters = new HashMap<>();
    }
    
    @Test
    public void testGetParameterValueUndefined() throws Exception {
        // execute & assert
        assertNull(ProcessUtils.getParameterValue(paramName, null, null));
        assertNull(ProcessUtils.getParameterValue(paramName, hadoopConf, parameters));
    }
    
    @Test
    public void testGetParameterValueDefinedInHadoopConf() throws Exception {
        // given
        String expectedValue = "value";
        hadoopConf.set(paramName, expectedValue);
        
        // execute & assert
        assertEquals(expectedValue, ProcessUtils.getParameterValue(paramName, hadoopConf, parameters));
    }

    @Test
    public void testGetParameterValueDefinedInParams() throws Exception {
        // given
        String expectedValue = "value";
        parameters.put(paramName, expectedValue);
        
        // execute & assert
        assertEquals(expectedValue, ProcessUtils.getParameterValue(paramName, hadoopConf, parameters));
    }
    
    @Test
    public void testGetParameterValueDefinedInBoth() throws Exception {
        // given
        String expectedParamValue = "paramValue";
        String expectedHadoopCfgValue = "hadoopCfgValue";
        parameters.put(paramName, expectedParamValue);
        hadoopConf.set(paramName, expectedHadoopCfgValue);
        
        // execute & assert
        assertEquals(expectedParamValue, ProcessUtils.getParameterValue(paramName, hadoopConf, parameters));
    }
}
