package eu.dnetlib.iis.wf.referenceextraction.community.input;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Files;

import eu.dnetlib.iis.common.utils.AvroAssertTestUtil;
import eu.dnetlib.iis.common.utils.AvroTestUtils;
import eu.dnetlib.iis.common.utils.JsonAvroTestUtils;
import eu.dnetlib.iis.importer.schemas.Concept;
import eu.dnetlib.iis.referenceextraction.community.schemas.Community;
import pl.edu.icm.sparkutils.test.SparkJob;
import pl.edu.icm.sparkutils.test.SparkJobBuilder;
import pl.edu.icm.sparkutils.test.SparkJobExecutor;

/**
 * @author mhorst
 *
 */
public class CommunityReferenceExtractionInputTransformerJobTest {

    private static final String DATA_DIRECTORY_PATH = "src/test/resources/eu/dnetlib/iis/wf/referenceextraction/community/data/input_transformer";
    
    
    private SparkJobExecutor executor = new SparkJobExecutor();
    
    private File workingDir;
    
    private String inputConceptPath;
    
    private String outputDirPath;
    
    
    @Before
    public void before() {
        
        workingDir = Files.createTempDir();
        inputConceptPath = workingDir + "/referenceextraction_community_input_transformer/inputConcept";
        outputDirPath = workingDir + "/referenceextraction_community_input_transformer/output";
    }
    
    
    @After
    public void after() throws IOException {
        
        FileUtils.deleteDirectory(workingDir);
        
    }
    
    
    //------------------------ TESTS --------------------------
    
    @Test
    public void inputTransformer() throws IOException {
        // given
        String jsonInputMetadataFile = DATA_DIRECTORY_PATH + "/concept.json";
        String jsonOutputFile = DATA_DIRECTORY_PATH + "/community.json";
        
        AvroTestUtils.createLocalAvroDataStore(
                JsonAvroTestUtils.readJsonDataStore(jsonInputMetadataFile, Concept.class),
                inputConceptPath);
        
        // execute
        executor.execute(buildInputTransformerJob(inputConceptPath, outputDirPath));
        
        // assert
        AvroAssertTestUtil.assertEqualsWithJsonIgnoreOrder(outputDirPath, jsonOutputFile, Community.class);
        
    }
    
    //------------------------ PRIVATE --------------------------
    
    private SparkJob buildInputTransformerJob(String inputConcept, String outputDirPath) {
        SparkJob sparkJob = SparkJobBuilder
                .create()
                
                .setAppName("Spark Community Reference Extraction - Input Transformer")

                .setMainClass(CommunityReferenceExtractionInputTransformerJob.class)
                .addArg("-inputConcept", inputConcept)
                .addArg("-acknowledgementParamName", "suggestedAcknowledgement")
                .addArg("-output", outputDirPath)
                .addJobProperty("spark.driver.host", "localhost")
                
                .build();
        
        return sparkJob;
    }
    
}
