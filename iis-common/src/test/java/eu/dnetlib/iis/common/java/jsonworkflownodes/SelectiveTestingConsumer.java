package eu.dnetlib.iis.common.java.jsonworkflownodes;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.avro.specific.SpecificRecord;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;

import eu.dnetlib.iis.common.OrderedProperties;
import eu.dnetlib.iis.common.java.PortBindings;
import eu.dnetlib.iis.common.java.Process;
import eu.dnetlib.iis.common.java.io.CloseableIterator;
import eu.dnetlib.iis.common.java.io.DataStore;
import eu.dnetlib.iis.common.java.io.FileSystemPath;
import eu.dnetlib.iis.common.java.io.JsonUtils;
import eu.dnetlib.iis.common.java.porttype.AnyPortType;
import eu.dnetlib.iis.common.java.porttype.PortType;

/**
 * Avro datastores selective fields testing consumer. 
 * 
 * Requirements are provided as comma separated list of properties files holding field requirements for each individual input object. 
 * Properties keys are paths to the fields we want to test in given object, properties values are expected values (with special markup for null: $NULL$).
 * 
 * Number of input avro records should be equal to the number of provided properties files.
 *   
 * @author mhorst
 */
public class SelectiveTestingConsumer implements Process {

    private static final String PROPERTIES_CSV = "expectation_properties_csv";

    private static final String PORT_INPUT = "datastore";

    private static final String NULL_VALUE_INDICATOR = "$NULL$";

    private final static Logger log = Logger.getLogger(SelectiveTestingConsumer.class);

    private static final Map<String, PortType> inputPorts = new HashMap<String, PortType>();
    
    {
        inputPorts.put(PORT_INPUT, new AnyPortType());
    }
    
    //------------------------ LOGIC ---------------------------------
    
    @Override
    public Map<String, PortType> getInputPorts() {
        return inputPorts;
    }

    @Override
    public Map<String, PortType> getOutputPorts() {
        return Collections.emptyMap();
    }

    @Override
    public void run(PortBindings portBindings, Configuration configuration, Map<String, String> parameters)
            throws Exception {

        Path inputRecordsPath = portBindings.getInput().get(PORT_INPUT);
        String propertiesPathsCSV = parameters.get(PROPERTIES_CSV);
        Preconditions.checkArgument(StringUtils.isNotBlank(propertiesPathsCSV), 
                "no '%s' property value provided, field requirements were not specified!", PROPERTIES_CSV);

        String[] recordsExpectationPropertiesLocations = StringUtils.split(propertiesPathsCSV, ',');
        FileSystem fs = FileSystem.get(configuration);

        if (!fs.exists(inputRecordsPath)) {
            throw new RuntimeException(inputRecordsPath + " hdfs location does not exist!");
        }
        
        try (CloseableIterator<SpecificRecord> recordsIterator = DataStore.getReader(new FileSystemPath(fs, inputRecordsPath))) {
            
            int recordsCount = 0;
            
            while (recordsIterator.hasNext()) {
                
                SpecificRecord record = recordsIterator.next();
                recordsCount++;

                if (recordsCount > recordsExpectationPropertiesLocations.length) {
                    throw new RuntimeException("got more records than expected: " + "unable to verify record no " + recordsCount
                            + ", no field specification provided! Record contents: " + JsonUtils.toPrettyJSON(record.toString()));
                } else {
                    validateRecord(record, readProperties(recordsExpectationPropertiesLocations[recordsCount - 1]));    
                }
            }
            
            if (recordsCount < recordsExpectationPropertiesLocations.length) {
                throw new RuntimeException(
                        "records count mismatch: " + "got: " + recordsCount + " expected: " + recordsExpectationPropertiesLocations.length);
            }
        }
    }
    
    //------------------------ PRIVATE ---------------------------------
    
    /**
     * Reads properties from given location.
     */
    private static Properties readProperties(String location) throws IOException {
        log.info("fields expectations location: " + location);
        Properties properties = new OrderedProperties();
        properties.load(TestingConsumer.class.getResourceAsStream(location.trim()));
        return properties;
    }
    
    /**
     * Validates record fields against specified expectations. RuntimeException is thrown when invalid.
     * 
     * @param record avro record to be validated
     * @param recordFieldExpectations set of field expectations defined as properties where key is field location and value is expected value
     */
    private static void validateRecord(SpecificRecord record, Properties recordFieldExpectations) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Iterator<Entry<Object,Object>> expectationPropertiesIterator = recordFieldExpectations.entrySet().iterator();
        while (expectationPropertiesIterator.hasNext()) {
            Entry<Object,Object> fieldExpectation = expectationPropertiesIterator.next();
            
            Object currentValue = PropertyUtils.getNestedProperty(record, (String)fieldExpectation.getKey());
            
            if ((currentValue != null && !fieldExpectation.getValue().equals(currentValue.toString())) 
                    || (currentValue == null && !NULL_VALUE_INDICATOR.equals(fieldExpectation.getValue()))) {
                throw new RuntimeException(
                        "invalid field value for path: " + fieldExpectation.getKey() + ", expected: '" + fieldExpectation.getValue() + "', "
                                + "got: '" + currentValue + "' Full object content: " + JsonUtils.toPrettyJSON(record.toString()));
            }
        }
    }
    
}