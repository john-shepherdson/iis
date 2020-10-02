package eu.dnetlib.iis.wf.collapsers;

import eu.dnetlib.iis.common.schemas.Identifier;
import eu.dnetlib.iis.importer.schemas.DocumentToProject;
import org.apache.avro.AvroTypeException;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.mapred.AvroKey;
import org.apache.avro.mapred.AvroValue;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author mhorst
 *
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
public class CollapserReducerTest {

    @Mock
    private Context context;
    
    @Captor
    private ArgumentCaptor<AvroKey<IndexedRecord>> keyCaptor;
    
    @Captor
    private ArgumentCaptor<NullWritable> valueCaptor;
    
    
    private CollapserReducer reducer = new CollapserReducer();
    
    // ------------------------------------- TESTS -----------------------------------
    
    
    @Test
    public void testSetupWithoutCollapserClass() {
        // given
        Configuration conf = new Configuration();
        conf.set(CollapserReducer.INPUT_SCHEMA, DocumentToProject.class.getCanonicalName());
        doReturn(conf).when(context).getConfiguration();
        
        // execute
        assertThrows(IOException.class, () -> reducer.setup(context));
    }
    
    @Test
    public void testSetupWithoutInputSchema() {
        // given
        Configuration conf = new Configuration();
        conf.set(CollapserReducer.RECORD_COLLAPSER, DummyDocumentToProjectCollapser.class.getCanonicalName());
        doReturn(conf).when(context).getConfiguration();
        
        // execute
        assertThrows(IOException.class, () -> reducer.setup(context));
    }

    @Test
    public void testReduceWithInvalidInputClass() throws Exception {
        // given
        Configuration conf = new Configuration();
        conf.set(CollapserReducer.RECORD_COLLAPSER, DummyDocumentToProjectCollapser.class.getCanonicalName());
        conf.set(CollapserReducer.INPUT_SCHEMA, DocumentToProject.class.getCanonicalName());
        doReturn(conf).when(context).getConfiguration();
        reducer.setup(context);

        // execute
        assertThrows(AvroTypeException.class, () -> reducer.reduce(null, Collections.singletonList(
                new AvroValue(Identifier.newBuilder().setId("id1").build())), context));
    }
    
    @Test
    public void testReduce() throws Exception {
        // given
        Configuration conf = new Configuration();
        conf.set(CollapserReducer.RECORD_COLLAPSER, DummyDocumentToProjectCollapser.class.getCanonicalName());
        conf.set(CollapserReducer.INPUT_SCHEMA, DocumentToProject.class.getCanonicalName());
        doReturn(conf).when(context).getConfiguration();
        reducer.setup(context);

        String docId1 = "docId1";
        String docId2 = "docId2";
        String projId1 = "projId1";
        String projId2 = "projId2";
        
        List<AvroValue<IndexedRecord>> docProjList = new ArrayList<>();
        docProjList.add(buildInputRecord(docId1, projId1));
        docProjList.add(buildInputRecord(docId1, projId2));
        docProjList.add(buildInputRecord(docId2, projId1));
        docProjList.add(buildInputRecord(docId2, projId2));
        
        // execute
        reducer.reduce(null, docProjList, context);
        
        // assert
        verify(context, times(2)).write(keyCaptor.capture(), valueCaptor.capture());
        assertEquals(2, keyCaptor.getAllValues().size());
        assertTrue(keyCaptor.getAllValues().get(0).datum() instanceof Identifier);
        assertEquals(docId1, ((Identifier)keyCaptor.getAllValues().get(0).datum()).getId().toString());
        assertTrue(keyCaptor.getAllValues().get(1).datum() instanceof Identifier);
        assertEquals(docId2, ((Identifier)keyCaptor.getAllValues().get(1).datum()).getId().toString());
        assertEquals(2, valueCaptor.getAllValues().size());
        assertSame(NullWritable.get(), valueCaptor.getAllValues().get(0));
        assertSame(NullWritable.get(), valueCaptor.getAllValues().get(1));
        
    }

    // ------------------------------------- PRIVATE --------------------------------
    
    private static AvroValue buildInputRecord(String docId, String projId) {
        return new AvroValue(DocumentToProject.newBuilder().setDocumentId(docId).setProjectId(projId).build());
    }
    
}
