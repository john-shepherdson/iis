package eu.dnetlib.iis.workflows.transformers.udfs;

import com.google.common.collect.Lists;
import java.io.IOException;
import org.apache.pig.EvalFunc;
import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.logicalLayer.FrontendException;
import org.apache.pig.impl.logicalLayer.schema.Schema;
import org.apache.pig.impl.logicalLayer.schema.SchemaUtil;

/**
 * Converts null value to an empty DataBag.
 * 
 * @author Dominika Tkaczyk
 */
public class NullToEmptyBag extends EvalFunc<DataBag> {

    @Override
    public DataBag exec(Tuple tuple) throws IOException {
        if (tuple == null || tuple.size() != 1) {
            return null;
        }
        DataBag db = (DataBag) tuple.get(0);
        if (db == null) {
            BagFactory bagFactory = BagFactory.getInstance();
            return bagFactory.newDefaultBag();
        }
        return db;
    }

    @Override
    public Schema outputSchema(Schema input) {
        try {
            return SchemaUtil.newBagSchema(Lists.newArrayList(DataType.CHARARRAY));
        } catch (FrontendException ex) {
            return null;
        }
    }
    
}