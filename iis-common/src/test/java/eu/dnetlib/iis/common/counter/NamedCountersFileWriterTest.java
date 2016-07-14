package eu.dnetlib.iis.common.counter;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author madryk
 */
public class NamedCountersFileWriterTest {

    private NamedCountersFileWriter countersFileWriter = new NamedCountersFileWriter();
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    
    private String counterName1 = "COUNTER_1";
    
    private String counterName2 = "COUNTER_2";
    
    
    //------------------------ TESTS --------------------------
    
    @Test
    public void writeCounters() throws IOException {
        
        // given
        
        NamedCounters namedCounters = new NamedCounters(new String[] { counterName1, counterName2 });
        namedCounters.increment(counterName1, 4L);
        namedCounters.increment(counterName2, 2L);
        
        // execute
        
        countersFileWriter.writeCounters(new NamedCounters(), tempFolder.getRoot().getPath() + "/counters.properties");
        
        // assert
        
        Properties actualProperties = loadProperties(new File(tempFolder.getRoot(), "counters.properties"));
        
        Properties expectedProperties = new Properties();
        expectedProperties.put(counterName1, "4");
        expectedProperties.put(counterName2, "2");
        
        assertEquals(expectedProperties, actualProperties);
    }
    
    
    //------------------------ PRIVATE --------------------------
    
    private Properties loadProperties(File propertiesFile) throws FileNotFoundException, IOException {
        Properties properties = new Properties();
        
        try (Reader reader = new FileReader(propertiesFile)) {
            properties.load(reader);
        }
        
        return properties;
    }
    
}
