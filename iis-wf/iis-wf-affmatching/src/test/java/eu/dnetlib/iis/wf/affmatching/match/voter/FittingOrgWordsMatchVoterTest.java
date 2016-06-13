package eu.dnetlib.iis.wf.affmatching.match.voter;

import static com.google.common.collect.ImmutableList.of;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import eu.dnetlib.iis.wf.affmatching.model.AffMatchAffiliation;
import eu.dnetlib.iis.wf.affmatching.model.AffMatchOrganization;

/**
 * @author madryk
 */
@RunWith(MockitoJUnitRunner.class)
public class FittingOrgWordsMatchVoterTest {

    @InjectMocks
    private FittingOrgWordsMatchVoter voter = new FittingOrgWordsMatchVoter(of(','), 2, 0.8, 0.8);
    
    @Mock
    private StringFilter stringFilter;
    
    @Mock
    private StringSimilarityChecker similarityChecker;
    
    
    private AffMatchAffiliation aff = new AffMatchAffiliation("DOC_ID", 1);
    
    private String affOrgName = "AFF_ORG_NAME";
    
    private AffMatchOrganization org = new AffMatchOrganization("ORG_ID");
    
    private String orgName = "ORG_NAME";
    
    
    @Before
    public void setup() {
        aff.setOrganizationName(affOrgName);
        org.setName(orgName);
    }
    
    
    //------------------------ TESTS --------------------------
    
    @Test(expected = NullPointerException.class)
    public void constructor_NULL_CHARS_TO_FILTER() {
        // execute
        new FittingOrgWordsMatchVoter(null, 2, 0.8, 0.8);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void constructor_NEGATIVE_MIN_WORD_LENGTH() {
        // execute
        new FittingOrgWordsMatchVoter(of(), -1, 0.8, 0.8);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void constructor_TOO_HIGH_MIN_FITTING_ORG_WORDS_PERCENTAGE() {
        // execute
        new FittingOrgWordsMatchVoter(of(), 2, 1.1, 0.8);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void constructor_TOO_LOW_MIN_FITTING_ORG_WORDS_PERCENTAGE() {
        // execute
        new FittingOrgWordsMatchVoter(of(), 2, 0, 0.8);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void constructor_TOO_HIGH_MIN_FITTING_WORD_SIMILARITY() {
        // execute
        new FittingOrgWordsMatchVoter(of(), 2, 0.8, 1.1);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void constructor_TOO_LOW_MIN_FITTING_WORD_SIMILARITY() {
        // execute
        new FittingOrgWordsMatchVoter(of(), 2, 0.8, 0);
    }
    
    
    @Test
    public void voteMatch_ORG_CONTAINS_ALL_AFF_WORDS() {
        
        // given
        
        when(stringFilter.filterCharsAndShortWords(affOrgName, ImmutableList.of(','), 2)).thenReturn("Department Chemistry University Toronto");
        when(stringFilter.filterCharsAndShortWords(orgName, ImmutableList.of(','), 2)).thenReturn("University Toronto");
        
        when(similarityChecker.containSimilarString(ImmutableSet.of("Department", "Chemistry", "University", "Toronto"), "University", 0.8)).thenReturn(true);
        when(similarityChecker.containSimilarString(ImmutableSet.of("Department", "Chemistry", "University", "Toronto"), "Toronto", 0.8)).thenReturn(true);
        
        
        // execute & assert
        assertTrue(voter.voteMatch(aff, org));
    }
    
    @Test
    public void voteMatch_ORG_CONTAINS_NOT_ALL_AFF_WORDS() {
        
        // given
        
        when(stringFilter.filterCharsAndShortWords(affOrgName, ImmutableList.of(','), 2)).thenReturn("George's Hospital Medical School");
        when(stringFilter.filterCharsAndShortWords(orgName, ImmutableList.of(','), 2)).thenReturn("Saint George's Hospital Medical School");
        
        when(similarityChecker.containSimilarString(ImmutableSet.of("George's", "Hospital", "Medical", "School"), "Saint", 0.8)).thenReturn(false);
        when(similarityChecker.containSimilarString(ImmutableSet.of("George's", "Hospital", "Medical", "School"), "George's", 0.8)).thenReturn(true);
        when(similarityChecker.containSimilarString(ImmutableSet.of("George's", "Hospital", "Medical", "School"), "Hospital", 0.8)).thenReturn(true);
        when(similarityChecker.containSimilarString(ImmutableSet.of("George's", "Hospital", "Medical", "School"), "Medical", 0.8)).thenReturn(true);
        when(similarityChecker.containSimilarString(ImmutableSet.of("George's", "Hospital", "Medical", "School"), "School", 0.8)).thenReturn(true);
        
        
        // execute & assert
        assertTrue(voter.voteMatch(aff, org));
    }
    
    @Test
    public void voteMatch_NOT_MATCH_ORG_CONTAINS_TOO_LESS_AFF_WORDS() {
        
        // given
        
        when(stringFilter.filterCharsAndShortWords(affOrgName, ImmutableList.of(','), 2)).thenReturn("Ohio University");
        when(stringFilter.filterCharsAndShortWords(orgName, ImmutableList.of(','), 2)).thenReturn("Ohio State University");
        
        when(similarityChecker.containSimilarString(ImmutableSet.of("Ohio", "University"), "Ohio", 0.8)).thenReturn(true);
        when(similarityChecker.containSimilarString(ImmutableSet.of("Ohio", "University"), "State", 0.8)).thenReturn(false);
        when(similarityChecker.containSimilarString(ImmutableSet.of("Ohio", "University"), "University", 0.8)).thenReturn(true);
        
        
        // execute & assert
        assertFalse(voter.voteMatch(aff, org));
    }
    
}
