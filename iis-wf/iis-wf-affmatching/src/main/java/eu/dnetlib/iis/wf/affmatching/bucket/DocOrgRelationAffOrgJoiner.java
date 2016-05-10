package eu.dnetlib.iis.wf.affmatching.bucket;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;

import eu.dnetlib.iis.wf.affmatching.bucket.projectorg.model.AffMatchDocumentOrganization;
import eu.dnetlib.iis.wf.affmatching.model.AffMatchAffiliation;
import eu.dnetlib.iis.wf.affmatching.model.AffMatchOrganization;
import scala.Tuple2;

/**
 * Implementation of {@link AffOrgJoiner} that joins {@link AffMatchAffiliation} with {@link AffMatchOrganization}
 * based on document-organization relations.
 * 
 * @author madryk
 */
public class DocOrgRelationAffOrgJoiner implements AffOrgJoiner {

    private static final long serialVersionUID = 1L;


    //------------------------ LOGIC --------------------------

    /**
     * Joins the given affiliations with organizations based on document-organization relations.<br />
     * Affiliation will be joined with the organization if documentOrganizations rdd will
     * contain pair ({@link AffMatchAffiliation#getDocumentId()}, {@link AffMatchOrganization#getId()})
     */
    @Override
    public JavaRDD<Tuple2<AffMatchAffiliation, AffMatchOrganization>> join(JavaRDD<AffMatchAffiliation> affiliations, JavaRDD<AffMatchOrganization> organizations,
            JavaRDD<AffMatchDocumentOrganization> documentOrganizations) {
        
        JavaPairRDD<String, AffMatchAffiliation> affiliationsDocIdKey = affiliations.keyBy(aff -> aff.getDocumentId());
        JavaPairRDD<String, AffMatchOrganization> organizationsOrgIdKey = organizations.keyBy(org -> org.getId());
        
        JavaPairRDD<String, AffMatchDocumentOrganization> documentOrganizationDocIdKey = documentOrganizations.keyBy(docOrg -> docOrg.getDocumentId());
        
        JavaRDD<Tuple2<AffMatchAffiliation, AffMatchOrganization>> affOrgBucketPairs = affiliationsDocIdKey
                .join(documentOrganizationDocIdKey)
                .mapToPair(x -> new Tuple2<String, AffMatchAffiliation>(x._2._2.getOrganizationId(), x._2._1))
                .join(organizationsOrgIdKey)
                .values();
        
        
        return affOrgBucketPairs;
    }

}
