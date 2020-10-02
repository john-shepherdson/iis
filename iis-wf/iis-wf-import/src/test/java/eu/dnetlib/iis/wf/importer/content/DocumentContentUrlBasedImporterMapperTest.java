package eu.dnetlib.iis.wf.importer.content;

import eu.dnetlib.iis.importer.auxiliary.schemas.DocumentContentUrl;
import eu.dnetlib.iis.importer.schemas.DocumentContent;
import eu.dnetlib.iis.wf.importer.content.DocumentContentUrlBasedImporterMapper.InvalidRecordCounters;
import org.apache.avro.mapred.AvroKey;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static eu.dnetlib.iis.wf.importer.ImportWorkflowRuntimeParameters.IMPORT_CONTENT_MAX_FILE_SIZE_MB;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

/**
 * @author mhorst
 *
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
public class DocumentContentUrlBasedImporterMapperTest {

    private DocumentContentUrlBasedImporterMapper mapper;
    
    private byte[] content;
    
    @Mock
    private Context context;
    
    @Mock
    private Counter sizeExceededCounter;
    
    @Mock
    private Counter sizeInvalidCounter;
    
    @Mock
    private Counter unavailableCounter;
    
    @Captor
    private ArgumentCaptor<AvroKey<DocumentContent>> keyCaptor;
    
    @Captor
    private ArgumentCaptor<NullWritable> valueCaptor;
    
    
    @BeforeEach
    public void init() throws Exception {
        
        content = "test content".getBytes("utf8");
        
        mapper = new DocumentContentUrlBasedImporterMapper() {
            
            @Override
            protected byte[] getContent(String url) throws IOException, InvalidSizeException {
                return content;
            }
            
        };
        
        doReturn(sizeExceededCounter).when(context).getCounter(InvalidRecordCounters.SIZE_EXCEEDED);
        doReturn(sizeInvalidCounter).when(context).getCounter(InvalidRecordCounters.SIZE_INVALID);
        doReturn(unavailableCounter).when(context).getCounter(InvalidRecordCounters.UNAVAILABLE);
    }
    
    // --------------------------------- TESTS ---------------------------------
    
    @Test
    public void testObtainContent() throws Exception {
        // given
        Configuration conf = new Configuration();
        doReturn(conf).when(context).getConfiguration();
        
        String id = "contentId";
        String url = "contentUrl";
        DocumentContentUrl docContentUrl = new DocumentContentUrl();
        docContentUrl.setId(id);
        docContentUrl.setUrl(url);
        docContentUrl.setContentSizeKB(1l);
        mapper.setup(context);
        
        // execute
        mapper.map(new AvroKey<DocumentContentUrl>(docContentUrl), null, context);
        
        // assert
        verify(context, times(1)).write(keyCaptor.capture(), valueCaptor.capture());
        assertSame(NullWritable.get(), valueCaptor.getValue());
        DocumentContent docContent = keyCaptor.getValue().datum();
        assertEquals(id, docContent.getId());
        assertEquals(content, docContent.getPdf().array());
        verify(sizeExceededCounter, never()).increment(1);
        verify(sizeInvalidCounter, never()).increment(1);
        verify(unavailableCounter, never()).increment(1);
    }
    
    @Test
    public void testContentSizeInvalid() throws Exception {
        // given
        Configuration conf = new Configuration();
        doReturn(conf).when(context).getConfiguration();
        
        String id = "contentId";
        String url = "contentUrl";
        DocumentContentUrl docContentUrl = new DocumentContentUrl();
        docContentUrl.setId(id);
        docContentUrl.setUrl(url);
        docContentUrl.setContentSizeKB(-1l);
        mapper.setup(context);
        
        // execute
        mapper.map(new AvroKey<DocumentContentUrl>(docContentUrl), null, context);
        
        // assert
        verify(context, never()).write(any(), any());
        verify(sizeExceededCounter, never()).increment(1);
        verify(sizeInvalidCounter, times(1)).increment(1);
        verify(unavailableCounter, never()).increment(1);
    }
    
    @Test
    public void testContentSizeInvalidThrown() throws Exception {
        // given
        mapper = new DocumentContentUrlBasedImporterMapper() {
            
            @Override
            protected byte[] getContent(String url) throws IOException, InvalidSizeException {
                throw new InvalidSizeException();
            }
            
        };
        
        Configuration conf = new Configuration();
        doReturn(conf).when(context).getConfiguration();
        
        String id = "contentId";
        String url = "contentUrl";
        DocumentContentUrl docContentUrl = new DocumentContentUrl();
        docContentUrl.setId(id);
        docContentUrl.setUrl(url);
        docContentUrl.setContentSizeKB(1l);
        mapper.setup(context);
        
        // execute
        mapper.map(new AvroKey<DocumentContentUrl>(docContentUrl), null, context);
        
        // assert
        verify(context, never()).write(any(), any());
        verify(sizeExceededCounter, never()).increment(1);
        verify(sizeInvalidCounter, times(1)).increment(1);
        verify(unavailableCounter, never()).increment(1);
    }
    
    @Test
    public void testIOExceptionThrown() throws Exception {
        // given
        mapper = new DocumentContentUrlBasedImporterMapper() {
            
            @Override
            protected byte[] getContent(String url) throws IOException, InvalidSizeException {
                throw new IOException();
            }
            
        };
        
        Configuration conf = new Configuration();
        doReturn(conf).when(context).getConfiguration();
        
        String id = "contentId";
        String url = "contentUrl";
        DocumentContentUrl docContentUrl = new DocumentContentUrl();
        docContentUrl.setId(id);
        docContentUrl.setUrl(url);
        docContentUrl.setContentSizeKB(1l);
        mapper.setup(context);
        
        // execute
        mapper.map(new AvroKey<DocumentContentUrl>(docContentUrl), null, context);
        
        // assert
        verify(context, never()).write(any(), any());
        verify(sizeExceededCounter, never()).increment(1);
        verify(sizeInvalidCounter, never()).increment(1);
        verify(unavailableCounter, times(1)).increment(1);
    }
    
    @Test
    public void testContentSizeExceeded() throws Exception {
        // given
        Configuration conf = new Configuration();
        conf.set(IMPORT_CONTENT_MAX_FILE_SIZE_MB, "1");
        doReturn(conf).when(context).getConfiguration();
        
        String id = "contentId";
        String url = "contentUrl";
        DocumentContentUrl docContentUrl = new DocumentContentUrl();
        docContentUrl.setId(id);
        docContentUrl.setUrl(url);
        docContentUrl.setContentSizeKB(1025l);
        mapper.setup(context);
        
        // execute
        mapper.map(new AvroKey<DocumentContentUrl>(docContentUrl), null, context);
        
        // assert
        verify(context, never()).write(any(), any());
        verify(sizeExceededCounter, times(1)).increment(1);
        verify(sizeInvalidCounter, never()).increment(1);
        verify(unavailableCounter, never()).increment(1);
        
    }

}
