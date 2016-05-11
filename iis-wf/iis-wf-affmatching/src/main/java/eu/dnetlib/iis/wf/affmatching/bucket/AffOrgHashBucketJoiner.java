package eu.dnetlib.iis.wf.affmatching.bucket;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;

import eu.dnetlib.iis.wf.affmatching.bucket.projectorg.model.AffMatchDocumentOrganization;
import eu.dnetlib.iis.wf.affmatching.model.AffMatchAffiliation;
import eu.dnetlib.iis.wf.affmatching.model.AffMatchOrganization;
import scala.Tuple2;

/**
 * Implementation of {@link AffOrgJoiner} that links {@link AffMatchAffiliation} to {@link AffMatchOrganization} according to
 * hashes generated by {@link #setAffiliationBucketHasher(BucketHasher)} and {@link #setOrganizationBucketHasher(BucketHasher)}.  
 * 
 * @author Łukasz Dumiszewski
*/

public class AffOrgHashBucketJoiner implements AffOrgJoiner {


    private static final long serialVersionUID = 1L;
    
    
    private BucketHasher<AffMatchAffiliation> affiliationBucketHasher = new AffiliationOrgNameFirstLettersBucketHasher();
    
    private BucketHasher<AffMatchOrganization> organizationBucketHasher = new OrganizationNameFirstLettersBucketHasher();
    
    
    
    //------------------------ LOGIC --------------------------
    
    /**
     * Joins the given affiliations to organizations by using hashes generated by {@link #setAffiliationBucketHasher(BucketHasher)} and
     * {@link #setOrganizationBucketHasher(BucketHasher)}. Returns pairs of affiliations and organizations that have the same hashes.<br />
     * This implementation completely ignores documentOrganizations parameter.
     */
    @Override
    public JavaRDD<Tuple2<AffMatchAffiliation, AffMatchOrganization>> join(JavaRDD<AffMatchAffiliation> affiliations, JavaRDD<AffMatchOrganization> organizations,
            JavaRDD<AffMatchDocumentOrganization> documentOrganizations) {

        JavaPairRDD<String, AffMatchAffiliation> hashAffiliations = affiliations.mapToPair(aff -> new Tuple2<String, AffMatchAffiliation>(affiliationBucketHasher.hash(aff), aff)); 
        
        JavaPairRDD<String, AffMatchOrganization> hashOrganizations = organizations.mapToPair(org -> new Tuple2<String, AffMatchOrganization>(organizationBucketHasher.hash(org), org)); 
        
        JavaPairRDD<String, Tuple2<AffMatchAffiliation, AffMatchOrganization>> hashAffOrgs = hashAffiliations.join(hashOrganizations);

        return hashAffOrgs.values();
    }


    //------------------------ SETTERS --------------------------

    /**
     * A hasher used to generate hashes for {@link AffMatchAffilation}s. 
     */
    public void setAffiliationBucketHasher(BucketHasher<AffMatchAffiliation> affiliationBucketHasher) {
        this.affiliationBucketHasher = affiliationBucketHasher;
    }


    /**
     * A hasher used to generate hashes for {@link AffMatchOrganization}s. 
     */
    public void setOrganizationBucketHasher(BucketHasher<AffMatchOrganization> organizationBucketHasher) {
        this.organizationBucketHasher = organizationBucketHasher;
    }

}
