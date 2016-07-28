package eu.dnetlib.iis.wf.affmatching.match.voter;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import datafu.com.google.common.base.Objects;
import eu.dnetlib.iis.wf.affmatching.model.AffMatchAffiliation;
import eu.dnetlib.iis.wf.affmatching.model.AffMatchOrganization;

/**
 * Match voter that checks if {@link AffMatchOrganization#getName()} words
 * are present in {@link AffMatchAffiliation#getOrganizationName()} words.
 * 
 * @author madryk, lukdumi
 */
public class CommonWordsVoter extends AbstractAffOrgMatchVoter {

    private static final long serialVersionUID = 1L;
    
    /** How the ratio of common words will be calculated */
    public enum RatioRelation {/** the ratio of common words will be calculated with regard to the number of words in {@link AffMatchAffiliation#getOrganizationName()}*/ 
                               WITH_REGARD_TO_AFF_WORDS,
                               /** the ratio of common words will be calculated with regard to the number of words in organization name in an organization object */
                               WITH_REGARD_TO_ORG_WORDS}
    
    private StringFilter stringFilter = new StringFilter();
    
    private CommonSimilarWordCalculator commonSimilarWordCalculator; 
    
    private RatioRelation ratioRelation = RatioRelation.WITH_REGARD_TO_AFF_WORDS;
    
    private List<Character> charsToFilter;
    
    private double minFittingOrgWordsRatio;
    
    private int wordToRemoveMaxLength;
    
    private Function<AffMatchOrganization, List<String>> getOrgNamesFunction = new GetOrgNameFunction();
    
    //------------------------ CONSTRUCTORS --------------------------
    
    /**
     * Default constructor
     * 
     * @param charsToFilter - list of characters that will be filtered out before comparing words
     * @param wordToRemoveMaxLength - words with length equal or less than 
     *      this value will be filtered out before comparing words.
     *      Setting it to zero disables this feature.
     * @param minFittingOrgWordsRatio - minimum ratio of {@link AffMatchOrganization#getName()}
     *      words that have to be found in {@link AffMatchAffiliation#getOrganizationName()}
     *      to all {@link AffMatchOrganization#getName()} words.
     *      Value must be between (0,1].
     * 
     * @see StringSimilarityChecker#containSimilarString(java.util.Collection, String, double)
     */
    public CommonWordsVoter(List<Character> charsToFilter, int wordToRemoveMaxLength, double minFittingOrgWordsRatio, RatioRelation ratioRelation) {
        Preconditions.checkNotNull(charsToFilter);
        Preconditions.checkArgument(wordToRemoveMaxLength >= 0);
        Preconditions.checkArgument(minFittingOrgWordsRatio > 0 && minFittingOrgWordsRatio <= 1);
        Preconditions.checkNotNull(ratioRelation);
        
        this.charsToFilter = charsToFilter;
        this.wordToRemoveMaxLength = wordToRemoveMaxLength;
        this.minFittingOrgWordsRatio = minFittingOrgWordsRatio;
        this.ratioRelation = ratioRelation;
        
    }
    
    //------------------------ LOGIC --------------------------
    
    /**
     * Returns true if minFittingOrgWordsRatio of the words in at least one of the organization names
     * are found in {@link AffMatchAffiliation#getOrganizationName()}.
     * 
     * @see #FittingOrgWordsMatchVoter(List, int, double, double)
     * @see #setGetOrgNamesFunction(Function)
     */
    @Override
    public boolean voteMatch(AffMatchAffiliation affiliation, AffMatchOrganization organization) {
        
        String filteredAffName = stringFilter.filterCharsAndShortWords(affiliation.getOrganizationName(), charsToFilter, wordToRemoveMaxLength);
        
        if (StringUtils.isEmpty(filteredAffName)) {
            return false;
        }
        
        List<String> affWords = ImmutableList.copyOf(StringUtils.split(filteredAffName));
        
        for (String orgName : getOrgNamesFunction.apply(organization)) {
            
            String filteredOrgName = stringFilter.filterCharsAndShortWords(orgName, charsToFilter, wordToRemoveMaxLength);
        
            if (StringUtils.isEmpty(filteredOrgName)) {
                continue;
            }   
            
            List<String> orgWords = ImmutableList.copyOf(StringUtils.split(filteredOrgName));
                
            if (isProperNumberOfSimilarWords(affWords, orgWords)) {
                return true;
            }
        }
        
        return false;
    }
    
    
    //------------------------ PRIVATE --------------------------

    private boolean isProperNumberOfSimilarWords(List<String> affWords, List<String> orgWords) {
        
        double similarWordRatio = 0;
        
        if (ratioRelation == RatioRelation.WITH_REGARD_TO_AFF_WORDS) {
            
            similarWordRatio = commonSimilarWordCalculator.calcSimilarWordRatio(affWords, orgWords);
            
        } else {

            similarWordRatio = commonSimilarWordCalculator.calcSimilarWordRatio(orgWords, affWords);

        }
        
        return similarWordRatio >= minFittingOrgWordsRatio;
    }

    
    //------------------------ SETTERS --------------------------
    
    /**
     * Sets the function that will be used to get the organization names 
     */
    public void setGetOrgNamesFunction(Function<AffMatchOrganization, List<String>> getOrgNamesFunction) {
        this.getOrgNamesFunction = getOrgNamesFunction;
    }

    public void setCommonSimilarWordCalculator(CommonSimilarWordCalculator commonSimilarWordCalculator) {
        this.commonSimilarWordCalculator = commonSimilarWordCalculator;
    }

    public void setRatioRelation(RatioRelation ratioRelation) {
        Preconditions.checkNotNull(ratioRelation);
        this.ratioRelation = ratioRelation;
    }
    
    
    //------------------------ toString --------------------------
    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("matchStength", getMatchStrength())
                                           .add("ratioRelation", ratioRelation)
                                           .add("charsToFilter", charsToFilter)
                                           .add("minFittingOrgWordsRatio", minFittingOrgWordsRatio)
                                           .add("wordToRemoveMaxLength", wordToRemoveMaxLength)
                                           .add("getOrgNamesFunction", getOrgNamesFunction.getClass().getSimpleName())
                                           .add("commonSimilarWordCalculator", commonSimilarWordCalculator)
                                           .toString();
    }

  

}
