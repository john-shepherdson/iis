package eu.dnetlib.iis.wf.affmatching;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Files;

import eu.dnetlib.iis.common.schemas.ReportEntry;
import eu.dnetlib.iis.common.utils.AvroAssertTestUtil;
import eu.dnetlib.iis.common.utils.AvroTestUtils;
import eu.dnetlib.iis.common.utils.JsonAvroTestUtils;
import eu.dnetlib.iis.importer.schemas.Organization;
import eu.dnetlib.iis.importer.schemas.ProjectToOrganization;
import eu.dnetlib.iis.metadataextraction.schemas.ExtractedDocumentMetadata;
import eu.dnetlib.iis.referenceextraction.project.schemas.DocumentToProject;
import eu.dnetlib.iis.wf.affmatching.model.MatchedOrganization;
import pl.edu.icm.sparkutils.test.SparkJob;
import pl.edu.icm.sparkutils.test.SparkJobBuilder;
import pl.edu.icm.sparkutils.test.SparkJobExecutor;

/**
* @author Łukasz Dumiszewski
*/

public class AffMatchingJobTest {
    
    
    private SparkJobExecutor executor = new SparkJobExecutor();
    
    private File workingDir;
    
    private String inputOrgDirPath;
    
    private String inputAffDirPath;
    
    private String inputInferredDocProjDirPath;
    
    private String inputDocProjDirPath;
    
    private float inputDocProjConfidenceThreshold = 0.8f;
    
    private String inputProjOrgDirPath;
    
    private String outputDirPath;
    
    private String outputReportPath;
    
    
    @Before
    public void before() {
        
        workingDir = Files.createTempDir();
        
        inputOrgDirPath = workingDir + "/affiliation_matching/input/organizations";
        inputAffDirPath = workingDir + "/affiliation_matching/input/affiliations";
        inputInferredDocProjDirPath = workingDir + "/affiliation_matching/input/doc_proj_inferred";
        inputDocProjDirPath = workingDir + "/affiliation_matching/input/doc_proj";
        inputProjOrgDirPath = workingDir + "/affiliation_matching/input/proj_org";
        outputDirPath = workingDir + "/affiliation_matching/output";
        outputReportPath = workingDir + "/affiliation_matching/report";
        
    }
    
    
    @After
    public void after() throws IOException {
        
        FileUtils.deleteDirectory(workingDir);
        
    }
    
    
    //------------------------ TESTS --------------------------
    
    @Test
    public void affiliationMatchingJob() throws IOException {
        
        
        // given
        
        String jsonInputOrgPath = "src/test/resources/data/input/organizations.json";
        String jsonInputAffPath = "src/test/resources/data/input/affiliations.json";
        String jsonInputInferredDocProjPath = "src/test/resources/data/input/docProjInferred.json";
        String jsonInputDocProjPath = "src/test/resources/data/input/docProj.json";
        String jsonInputProjOrgPath = "src/test/resources/data/input/projOrg.json";
        
        String jsonOutputPath = "src/test/resources/data/expectedOutput/matchedOrganizations.json";
        String jsonOutputReportPath = "src/test/resources/data/expectedOutput/report.json";
        
        
        AvroTestUtils.createLocalAvroDataStore(
                JsonAvroTestUtils.readJsonDataStore(jsonInputOrgPath, Organization.class), inputOrgDirPath);
        
        AvroTestUtils.createLocalAvroDataStore(
                JsonAvroTestUtils.readJsonDataStore(jsonInputAffPath, ExtractedDocumentMetadata.class), inputAffDirPath);
        
        AvroTestUtils.createLocalAvroDataStore(
                JsonAvroTestUtils.readJsonDataStore(jsonInputInferredDocProjPath, DocumentToProject.class), inputInferredDocProjDirPath);
        
        AvroTestUtils.createLocalAvroDataStore(
                JsonAvroTestUtils.readJsonDataStore(jsonInputDocProjPath, eu.dnetlib.iis.importer.schemas.DocumentToProject.class), inputDocProjDirPath);
        
        AvroTestUtils.createLocalAvroDataStore(
                JsonAvroTestUtils.readJsonDataStore(jsonInputProjOrgPath, ProjectToOrganization.class), inputProjOrgDirPath);
        
        // execute & assert
        
        executeJobAndAssert(jsonOutputPath, jsonOutputReportPath);
    }
    
    
    
    
    //------------------------ PRIVATE --------------------------
    
    
    private void executeJobAndAssert(String jsonOutputPath, String jsonOutputReportPath) throws IOException {

        SparkJob sparkJob = SparkJobBuilder
                                           .create()
                                           
                                           .setAppName(getClass().getName())
        
                                           .setMainClass(AffMatchingJob.class)
                                           .addArg("-inputAvroOrgPath", inputOrgDirPath)
                                           .addArg("-inputAvroAffPath", inputAffDirPath)
                                           .addArg("-inputAvroDocProjPath", inputDocProjDirPath)
                                           .addArg("-inputAvroInferredDocProjPath", inputInferredDocProjDirPath)
                                           .addArg("-inputDocProjConfidenceThreshold", String.valueOf(inputDocProjConfidenceThreshold))
                                           .addArg("-inputAvroProjOrgPath", inputProjOrgDirPath)
                                           .addArg("-outputAvroPath", outputDirPath)
                                           .addArg("-outputAvroReportPath", outputReportPath)
                                           .build();
        
        
        // execute
        
        executor.execute(sparkJob);
        
        
        
        // assert
        AvroAssertTestUtil.assertEqualsWithJsonIgnoreOrder(outputDirPath, jsonOutputPath, MatchedOrganization.class);
        AvroAssertTestUtil.assertEqualsWithJsonIgnoreOrder(outputReportPath, jsonOutputReportPath, ReportEntry.class);

    }



}
