package eu.dnetlib.iis.wf.importer.facade;

import java.util.Collections;

import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.apache.log4j.Logger;

import eu.dnetlib.enabling.is.lookup.rmi.ISLookUpDocumentNotFoundException;
import eu.dnetlib.enabling.is.lookup.rmi.ISLookUpException;
import eu.dnetlib.enabling.is.lookup.rmi.ISLookUpService;
import eu.dnetlib.enabling.resultset.client.ResultSetClientFactory;
import eu.dnetlib.enabling.tools.JaxwsServiceResolverImpl;

/**
 * WebService based database facade.
 * 
 * @author mhorst
 *
 */
public class WebServiceISLookupFacade extends AbstractResultSetAwareWebServiceFacade<ISLookUpService> implements ISLookupFacade {

    private final Logger log = Logger.getLogger(this.getClass());

    
    //------------------------ CONSTRUCTORS -------------------
    
    /**
     * @param serviceLocation database service location
     * @param resultSetReadTimeout result set providing database results read timeout
     * @param resultSetPageSize result set data chunk size
     */
    public WebServiceISLookupFacade(String serviceLocation, long resultSetReadTimeout, int resultSetPageSize) {
        super(ISLookUpService.class, serviceLocation, resultSetReadTimeout, resultSetPageSize);
    }

    //------------------------ LOGIC --------------------------
    
    @Override
    public Iterable<String> searchProfile(String xPathQuery) throws ServiceFacadeException {
        try {
            W3CEndpointReference eprResult = service.searchProfile(xPathQuery);
            // obtaining resultSet
            ResultSetClientFactory rsFactory = new ResultSetClientFactory();
            rsFactory.setTimeout(resultSetReadTimeout);
            rsFactory.setServiceResolver(new JaxwsServiceResolverImpl());
            rsFactory.setPageSize(resultSetPageSize);
            return rsFactory.getClient(eprResult);    
        }  catch (ISLookUpDocumentNotFoundException e) {
            log.error("unable to find profile for query: " + xPathQuery, e);
            return Collections.emptyList();
        } catch (ISLookUpException e) {
            throw new ServiceFacadeException("searching profiles in ISLookup failed with query '" + xPathQuery + "'", e);
        }
        
    }

}
