package eu.dnetlib.iis.wf.affmatching.match;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import eu.dnetlib.iis.wf.affmatching.model.AffMatchAffiliation;
import eu.dnetlib.iis.wf.affmatching.model.AffMatchOrganization;
import eu.dnetlib.iis.wf.affmatching.model.AffMatchResult;

/**
* @author Łukasz Dumiszewski
*/

public class AffMatchResultChooserTest {

    private AffMatchResultChooser chooser = new AffMatchResultChooser();
    
    private AffMatchAffiliation affiliation = new AffMatchAffiliation("DOC_A", 1);
    
    private AffMatchOrganization organization_A = new AffMatchOrganization("ORG_A");
    private AffMatchOrganization organization_B = new AffMatchOrganization("ORG_B");
    
    
    
    //------------------------ TESTS --------------------------

    
    @Test(expected = NullPointerException.class)
    public void chooseBetter_affMatchResult1_null() {
        
        // given
        
        AffMatchResult affMatchResult2 = new AffMatchResult(affiliation, organization_B, 0.7f);
        
        // execute
        
        chooser.chooseBetter(null, affMatchResult2);
        
    }
    
    

    @Test(expected = NullPointerException.class)
    public void chooseBetter_affMatchResult2_null() {
        
        // given
        
        AffMatchResult affMatchResult1 = new AffMatchResult(affiliation, organization_A, 0.7f);
        
        // execute
        
        chooser.chooseBetter(affMatchResult1, null);
        
    }
    
    
    
    @Test
    public void chooseBetter_matchStrength_diff_1() {
        
        // given
        
        AffMatchResult affMatchResult1 = new AffMatchResult(affiliation, organization_A, 0.8f);
        AffMatchResult affMatchResult2 = new AffMatchResult(affiliation, organization_B, 0.7f);
        
        // execute & assert
        
        assertTrue(affMatchResult1 == chooser.chooseBetter(affMatchResult1, affMatchResult2));
        
    }

    
    
    @Test
    public void chooseBetter_matchStrength_diff_2() {
        
        // given
        
        AffMatchResult affMatchResult1 = new AffMatchResult(affiliation, organization_A, 0.8f);
        AffMatchResult affMatchResult2 = new AffMatchResult(affiliation, organization_B, 1f);
        
        
        // execute & assert
        
        assertTrue(affMatchResult2 == chooser.chooseBetter(affMatchResult1, affMatchResult2));
        
    }

    
    @Test
    public void chooseBetter_matchStrength_same() {
        
        // given
        
        AffMatchAffiliation affiliation = new AffMatchAffiliation("DOC1", 1);
        
        AffMatchResult affMatchResult1 = new AffMatchResult(affiliation, organization_A, 0.6f);
        AffMatchResult affMatchResult2 = new AffMatchResult(affiliation, organization_B, 0.6f);
        
        
        // execute & assert
        
        assertTrue(affMatchResult1 == chooser.chooseBetter(affMatchResult1, affMatchResult2));
        
    }
}
