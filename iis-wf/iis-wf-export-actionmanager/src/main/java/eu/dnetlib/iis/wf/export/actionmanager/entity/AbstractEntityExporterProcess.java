package eu.dnetlib.iis.wf.export.actionmanager.entity;

import static eu.dnetlib.iis.wf.export.actionmanager.ExportWorkflowRuntimeParameters.EXPORT_ACTION_SETID;
import static eu.dnetlib.iis.wf.export.actionmanager.ExportWorkflowRuntimeParameters.EXPORT_ENTITY_MDSTORE_SERVICE_LOCATION;
import static eu.dnetlib.iis.wf.export.actionmanager.ExportWorkflowRuntimeParameters.EXPORT_SEQ_FILE_ACTIVE;
import static eu.dnetlib.iis.wf.export.actionmanager.ExportWorkflowRuntimeParameters.EXPORT_SEQ_FILE_OUTPUT_DIR_NAME;
import static eu.dnetlib.iis.wf.export.actionmanager.ExportWorkflowRuntimeParameters.EXPORT_SEQ_FILE_OUTPUT_DIR_ROOT;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.TransformerException;
import javax.xml.ws.wsaddressing.W3CEndpointReferenceBuilder;

import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import eu.dnetlib.actionmanager.actions.ActionFactory;
import eu.dnetlib.actionmanager.actions.AtomicAction;
import eu.dnetlib.actionmanager.actions.XsltInfoPackageAction;
import eu.dnetlib.actionmanager.common.Operation;
import eu.dnetlib.actionmanager.rmi.ActionManagerException;
import eu.dnetlib.data.mdstore.DocumentNotFoundException;
import eu.dnetlib.data.mdstore.MDStoreService;
import eu.dnetlib.enabling.tools.JaxwsServiceResolverImpl;
import eu.dnetlib.iis.common.WorkflowRuntimeParameters;
import eu.dnetlib.iis.common.hbase.HBaseConstants;
import eu.dnetlib.iis.common.java.PortBindings;
import eu.dnetlib.iis.common.java.Process;
import eu.dnetlib.iis.common.java.ProcessUtils;
import eu.dnetlib.iis.common.java.io.CloseableIterator;
import eu.dnetlib.iis.common.java.io.DataStore;
import eu.dnetlib.iis.common.java.io.FileSystemPath;
import eu.dnetlib.iis.common.java.porttype.AvroPortType;
import eu.dnetlib.iis.common.java.porttype.PortType;
import eu.dnetlib.iis.wf.export.actionmanager.api.ActionManagerServiceFacade;
import eu.dnetlib.iis.wf.export.actionmanager.api.HBaseActionManagerServiceFacade;
import eu.dnetlib.iis.wf.export.actionmanager.api.SequenceFileActionManagerServiceFacade;
import eu.dnetlib.iis.wf.export.actionmanager.cfg.ActionManagerConfigurationProvider;
import eu.dnetlib.iis.wf.export.actionmanager.cfg.StaticConfigurationProvider;

/**
 * Abstract entity exporter process.
 * @author mhorst
 *
 */
public abstract class AbstractEntityExporterProcess<T extends SpecificRecordBase> implements Process {

	protected final Logger log = Logger.getLogger(this.getClass());

	protected final static String inputPort = "input";
	
	protected final static String entityIdPrefix;
	
	protected final Schema inputPortSchema;
	protected final String entityXSLTName;
	protected final String entityXSLTLocation;
	protected final ActionManagerConfigurationProvider configProvider;
	
	static {
		try {
			entityIdPrefix = new String(HBaseConstants.ROW_PREFIX_RESULT,
					HBaseConstants.STATIC_FIELDS_ENCODING_UTF8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Default constructor.
	 * @param inputPortSchema
	 * @param entityXSLTName
	 * @param entityXSLTLocation
	 * @param entityNamespacePrefix
	 */
	public AbstractEntityExporterProcess(Schema inputPortSchema,
			String entityXSLTName, String entityXSLTLocation,
			String entityNamespacePrefix) {
		this.inputPortSchema = inputPortSchema;
		this.entityXSLTName = entityXSLTName;
		this.entityXSLTLocation = entityXSLTLocation;
		this.configProvider = new StaticConfigurationProvider(
				StaticConfigurationProvider.AGENT_DEFAULT, 
				StaticConfigurationProvider.PROVENANCE_DEFAULT,
				StaticConfigurationProvider.ACTION_TRUST_0_9, 
				entityNamespacePrefix);
	}
	
	@Override
	public void run(PortBindings portBindings, Configuration conf, Map<String, String> parameters) throws Exception {
		String mdStoreLocation = ProcessUtils.getParameterValue(
				EXPORT_ENTITY_MDSTORE_SERVICE_LOCATION, conf, parameters);
		String actionSetId = ProcessUtils.getParameterValue(
				EXPORT_ACTION_SETID, conf, parameters);
		if (mdStoreLocation==null || WorkflowRuntimeParameters.UNDEFINED_NONEMPTY_VALUE.equals(mdStoreLocation)) {
			throw new InvalidParameterException("unable to export document entities to action manager, " + 
					"unknown MDStore service location. "
					+ "Required parameter '" + EXPORT_ENTITY_MDSTORE_SERVICE_LOCATION + "' is missing!");
		}
		if (actionSetId==null || WorkflowRuntimeParameters.UNDEFINED_NONEMPTY_VALUE.equals(actionSetId)) {
			throw new RuntimeException("unable to export document entities to action manager, " +
					"no '" + EXPORT_ACTION_SETID + "' required parameter provided!");
		}
		
		MDStoreService mdStore = buildMDStoreClient(mdStoreLocation);
		ActionManagerServiceFacade actionManager = buildActionManager(conf, parameters);
		ActionFactory actionFactory = buildActionFactory();
		CloseableIterator<T> it = DataStore.<T>getReader(
				new FileSystemPath(FileSystem.get(conf), portBindings.getInput().get(inputPort)));
		
		try {
			int counter = 0;
			Set<String> exportedEntityIds = new HashSet<String>();
			while (it.hasNext()) {
				MDStoreIdWithEntityId mdStoreComplexId = deliverMDStoreIds(it.next());
				if (!exportedEntityIds.contains(mdStoreComplexId.getEntityId())) {
					String mdRecordId = convertToMDStoreId(mdStoreComplexId.getEntityId());
					try {
						handleRecord(mdStore.deliverRecord(mdStoreComplexId.getMdStoreId(), mdRecordId), 
								actionSetId, actionFactory, actionManager);
						exportedEntityIds.add(mdStoreComplexId.getEntityId());
						counter++;
					} catch (DocumentNotFoundException e) {
						log.error("mdrecord: " + mdRecordId + 
									" wasn't found in mdstore: " + mdStoreComplexId.getMdStoreId() ,e);
//						TODO write missing document identifiers in output datastore
					}  catch (Exception e) {
						log.error("got exception when trying to retrieve "
								+ "MDStore record for mdstore id " + mdStoreComplexId.getMdStoreId() + 
								", and document id: " + mdRecordId, e);
						throw e;
					}
				}
			}
			log.warn("exported " + counter + " entities in total");
		} finally {
			it.close();
			actionManager.close();
		}
	}
	
	/**
	 * Handles single MDStore record.
	 * @param mdStoreRecord
	 * @param actionSetId
	 * @param actionFactory
	 * @param actionManager
	 * @throws Exception 
	 */
	protected void handleRecord(String mdStoreRecord, 
			String actionSetId, ActionFactory actionFactory, 
			ActionManagerServiceFacade actionManager) throws Exception {
		if (mdStoreRecord!=null) {
			XsltInfoPackageAction xsltAction = actionFactory.generateInfoPackageAction(
					entityXSLTName, actionSetId, 
					configProvider.provideAgent(), 
					Operation.INSERT, mdStoreRecord,
					configProvider.provideProvenance(),
					configProvider.provideNamespacePrefix(),
					configProvider.provideActionTrust());
			if (xsltAction!=null) {
				List<AtomicAction> atomicActions = xsltAction.asAtomicActions();
				if (atomicActions!=null) {
					actionManager.storeAction(atomicActions,
							configProvider.provideProvenance(),
							configProvider.provideActionTrust(),
							configProvider.provideNamespacePrefix());	
				}
			}
		}
	}
	
	/**
	 * Provides action manager instance.
	 * @param conf
	 * @param parameters
	 * @return action manager instance
	 * @throws IOException
	 */
	protected ActionManagerServiceFacade buildActionManager(
			Configuration conf, Map<String, String> parameters) throws IOException {
		boolean seqFileExportMode = Boolean.valueOf(ProcessUtils.getParameterValue(
				EXPORT_SEQ_FILE_ACTIVE, conf, parameters));
		return seqFileExportMode?
				new SequenceFileActionManagerServiceFacade(conf, 
						ProcessUtils.getParameterValue(EXPORT_SEQ_FILE_OUTPUT_DIR_ROOT, 
								conf, parameters), 
						ProcessUtils.getParameterValue(EXPORT_SEQ_FILE_OUTPUT_DIR_NAME, 
								conf, parameters)):
				new HBaseActionManagerServiceFacade(conf, parameters);
	}
	
	/**
	 * Builds mdstore service client.
	 * @param mdStoreLocation
	 * @return {@link MDStoreService} client
	 */
	protected MDStoreService buildMDStoreClient(String mdStoreLocation) {
		W3CEndpointReferenceBuilder eprBuilder = new W3CEndpointReferenceBuilder();
		eprBuilder.address(mdStoreLocation);
		eprBuilder.build();
		return new JaxwsServiceResolverImpl().getService(
				MDStoreService.class, eprBuilder.build());
	}
	
	public class MDStoreIdWithEntityId {
		private final String mdStoreId;
		private final String entityId;
		
		public MDStoreIdWithEntityId(String mdStoreId, 
				String entityId) {
			this.mdStoreId = mdStoreId;
			this.entityId = entityId;
		}
		
		public String getMdStoreId() {
			return mdStoreId;
		}

		public String getEntityId() {
			return entityId;
		}
	}
	
	/**
	 * Provides mdstore and entity identifiers for given input record.
	 * @param element
	 * @return mdstore and entity identifiers pair
	 */
	abstract protected MDStoreIdWithEntityId deliverMDStoreIds(T element);

	/**
	 * Creates action factory.
	 * @return action factory
	 */
	protected ActionFactory buildActionFactory() {
		Map<String,Resource> xslts = new HashMap<String, Resource>();
		xslts.put(entityXSLTName, new ClassPathResource(
				entityXSLTLocation));
		ActionFactory actionFactory = new ActionFactory();
		actionFactory.setXslts(xslts);
		return actionFactory;
	}

	@Override
	public Map<String, PortType> getInputPorts() {
		HashMap<String, PortType> inputPorts = 
				new HashMap<String, PortType>();
		inputPorts.put(inputPort, 
				new AvroPortType(inputPortSchema));
		return inputPorts;
	}

	@Override
	public Map<String, PortType> getOutputPorts() {
		return Collections.emptyMap();
	}

	/**
	 * Converts to MDStore id by skipping result entity prefix.
	 * @param id
	 * @return MDStore compliant identifier
	 */
	protected final String convertToMDStoreId(String id) {
		if (id!=null) {
			if (id.startsWith(entityIdPrefix)) {
				return id.substring(entityIdPrefix.length());
			} else {
				return id;
			}
		} else {
			return null;
		}
	}
}
