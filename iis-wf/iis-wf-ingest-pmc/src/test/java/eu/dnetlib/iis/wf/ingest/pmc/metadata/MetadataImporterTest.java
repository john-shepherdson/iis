package eu.dnetlib.iis.wf.ingest.pmc.metadata;

import eu.dnetlib.iis.audit.schemas.Fault;
import eu.dnetlib.iis.common.ClassPathResourceProvider;
import eu.dnetlib.iis.common.javamapreduce.MultipleOutputs;
import eu.dnetlib.iis.ingest.pmc.metadata.schemas.ExtractedDocumentMetadata;
import eu.dnetlib.iis.metadataextraction.schemas.DocumentText;
import org.apache.avro.mapred.AvroKey;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.jdom.input.JDOMParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static eu.dnetlib.iis.wf.ingest.pmc.metadata.MetadataImporter.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 * @author mhorst
 *
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
public class MetadataImporterTest {

    private static final String XML_FILE = "/eu/dnetlib/iis/wf/ingest/pmc/metadata/data/document_jats23_nested_in_oai.xml";
    
    private static final String NON_XML_FILE = "/eu/dnetlib/iis/wf/ingest/pmc/metadata/data/document_invalid.xml";
    
    @Mock
    private Context context;
    
    @Mock
    private MultipleOutputs multipleOutputs;
    
    @Captor
    private ArgumentCaptor<String> mosKeyCaptor;
    
    @Captor
    private ArgumentCaptor<AvroKey<?>> mosValueCaptor;

    
    private MetadataImporter mapper;

    
    @BeforeEach
    public void init() {
        mapper = new MetadataImporter() {
            
            @Override
            protected MultipleOutputs instantiateMultipleOutputs(Context context) {
                return multipleOutputs;
            }
            
        };
    }
    
    // ------------------------------------- TESTS -----------------------------------
    
    @Test
    public void testSetupWithoutNamedOutputMeta() {
        // given
        Configuration conf = new Configuration();
        doReturn(conf).when(context).getConfiguration();
        
        // execute
        assertThrows(RuntimeException.class, () -> mapper.setup(context));
    }
    
    @Test
    public void testSetupWithoutNamedOutputFault() {
        // given
        Configuration conf = new Configuration();
        conf.set(NAMED_OUTPUT_META, "meta");
        doReturn(conf).when(context).getConfiguration();
        
        // execute
        assertThrows(RuntimeException.class, () -> mapper.setup(context));
    }
    
    @Test
    public void testMap() throws Exception {
        // given
        Configuration conf = new Configuration();
        conf.set(NAMED_OUTPUT_META, "meta");
        conf.set(NAMED_OUTPUT_FAULT, "fault");
        doReturn(conf).when(context).getConfiguration();
        mapper.setup(context);
        
        String id = "id";
        DocumentText.Builder docTextBuilder = DocumentText.newBuilder();
        docTextBuilder.setId(id);
        docTextBuilder.setText(ClassPathResourceProvider.getResourceContent(XML_FILE));
        
        // execute
        mapper.map(new AvroKey<>(docTextBuilder.build()), null, context);
        
        // assert
        verify(context, never()).write(any(), any());
        verify(multipleOutputs, times(1)).write(mosKeyCaptor.capture(), mosValueCaptor.capture());
        // doc meta
        assertEquals(conf.get(NAMED_OUTPUT_META), mosKeyCaptor.getValue());
        ExtractedDocumentMetadata docMeta = (ExtractedDocumentMetadata) mosValueCaptor.getValue().datum();
        assertNotNull(docMeta);
        assertEquals(id, docMeta.getId());
    }
    
    @Test
    public void testMapWithExcludedIds() throws Exception {
        // given
        String id = "id";
        DocumentText.Builder docTextBuilder = DocumentText.newBuilder();
        docTextBuilder.setId(id);
        docTextBuilder.setText(ClassPathResourceProvider.getResourceContent(XML_FILE));
        
        Configuration conf = new Configuration();
        conf.set(NAMED_OUTPUT_META, "meta");
        conf.set(NAMED_OUTPUT_FAULT, "fault");
        conf.set(EXCLUDED_IDS, id);
        doReturn(conf).when(context).getConfiguration();
        mapper.setup(context);
        
        // execute
        mapper.map(new AvroKey<>(docTextBuilder.build()), null, context);
        
        // assert
        verify(context, never()).write(any(), any());
        verify(multipleOutputs, never()).write(any(), any());
    }
    
    @Test
    public void testMapWithEmptryContent() throws Exception {
        // given
        String id = "id";
        DocumentText.Builder docTextBuilder = DocumentText.newBuilder();
        docTextBuilder.setId(id);
        docTextBuilder.setText("");
        
        Configuration conf = new Configuration();
        conf.set(NAMED_OUTPUT_META, "meta");
        conf.set(NAMED_OUTPUT_FAULT, "fault");
        doReturn(conf).when(context).getConfiguration();
        mapper.setup(context);
        
        // execute
        mapper.map(new AvroKey<>(docTextBuilder.build()), null, context);
        
        // assert
        verify(context, never()).write(any(), any());
        verify(multipleOutputs, never()).write(any(), any());
    }
    
    @Test
    public void testMapWithIvalidXml() throws Exception {
        // given
        Configuration conf = new Configuration();
        conf.set(NAMED_OUTPUT_META, "meta");
        conf.set(NAMED_OUTPUT_FAULT, "fault");
        doReturn(conf).when(context).getConfiguration();
        mapper.setup(context);
        
        String id = "id";
        DocumentText.Builder docTextBuilder = DocumentText.newBuilder();
        docTextBuilder.setId(id);
        docTextBuilder.setText(ClassPathResourceProvider.getResourceContent(NON_XML_FILE));
        
        // execute
        mapper.map(new AvroKey<>(docTextBuilder.build()), null, context);
        
        // assert
        verify(context, never()).write(any(), any());
        verify(multipleOutputs, times(2)).write(mosKeyCaptor.capture(), mosValueCaptor.capture());
        // doc meta
        assertEquals(conf.get(NAMED_OUTPUT_META), mosKeyCaptor.getAllValues().get(0));
        ExtractedDocumentMetadata docMeta = (ExtractedDocumentMetadata) mosValueCaptor.getAllValues().get(0).datum();
        assertNotNull(docMeta);
        assertEquals(id, docMeta.getId());
        assertEquals("", docMeta.getText());
        assertEquals(JatsXmlHandler.ENTITY_TYPE_UNKNOWN, docMeta.getEntityType());
        // fault
        assertEquals(conf.get(NAMED_OUTPUT_FAULT), mosKeyCaptor.getAllValues().get(1));
        Fault fault = (Fault) mosValueCaptor.getAllValues().get(1).datum();
        assertNotNull(fault);
        assertEquals(id, fault.getInputObjectId());
        assertEquals(JDOMParseException.class.getName(), fault.getCode());
        assertTrue(fault.getTimestamp() > 0);
    }
    
    @Test
    public void testCleanup() throws Exception {
        // given
        Configuration conf = new Configuration();
        conf.set(NAMED_OUTPUT_META, "meta");
        conf.set(NAMED_OUTPUT_FAULT, "fault");
        conf.set(NAMED_OUTPUT_FAULT, "fault");
        
        doReturn(conf).when(context).getConfiguration();
        mapper.setup(context);
        
        // execute
        mapper.cleanup(context);
        
        // assert
        verify(multipleOutputs, times(1)).close();
    }
}
