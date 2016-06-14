package eu.dnetlib.iis.wf.affmatching.match.voter;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Objects;

import eu.dnetlib.iis.wf.affmatching.model.AffMatchAffiliation;
import eu.dnetlib.iis.wf.affmatching.model.AffMatchOrganization;

/**
* @author Łukasz Dumiszewski
*/

public class NameStrictWithCharFilteringMatchVoter extends AbstractAffOrgMatchVoter {

    
    private static final long serialVersionUID = 1L;

    private StringFilter stringFilter = new StringFilter();
    
    private List<Character> charsToFilter;
    
    
    //------------------------ CONSTRUCTORS --------------------------
    
    /**
     * @param charsToFilter - list of characters that will be not taken
     *      into account when comparing organization names
     */
    public NameStrictWithCharFilteringMatchVoter(List<Character> charsToFilter) {
        this.charsToFilter = charsToFilter;
    }
    
    //------------------------ LOGIC --------------------------
    
    
    /**
     * Returns true if the name of the passed organization is the same as the name of the organization in the passed affiliation
     * (does not take chars to filter into account). 
     */
    @Override
    public boolean voteMatch(AffMatchAffiliation affiliation, AffMatchOrganization organization) {
        
        String filteredAffName = stringFilter.filterChars(affiliation.getOrganizationName(), charsToFilter);
        String filteredOrgName = stringFilter.filterChars(organization.getName(), charsToFilter);
        
        
        if (StringUtils.isEmpty(filteredAffName) || StringUtils.isEmpty(filteredOrgName)) {
            return false;
        }
        
        return filteredAffName.equals(filteredOrgName);
    }

    //------------------------ toString --------------------------
    
    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("matchStrength", getMatchStrength()).add("charsToFilter", charsToFilter).toString();
    }
   
}
