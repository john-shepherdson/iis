package eu.dnetlib.iis.wf.importer.converter;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.hadoop.hbase.client.Result;
import org.apache.log4j.Logger;

import eu.dnetlib.data.proto.OafProtos.Oaf;
import eu.dnetlib.iis.importer.schemas.DocumentContent;
import eu.dnetlib.iis.wf.importer.content.ContentProviderService;
import eu.dnetlib.iis.wf.importer.input.approver.ResultApprover;

/**
 * HBase {@link Result} to avro {@link DocumentContent} converter.
 * 
 * @author mhorst
 *
 */
public class DocumentContentConverter extends AbstractAvroConverter<DocumentContent> {

    protected static final Logger log = Logger.getLogger(DocumentContentConverter.class);

    /**
     * Content provider.
     */
    private final ContentProviderService contentProviderService;

    /**
     * Row text encoding.
     */
    private final String encoding;

    /**
     * Default constructor.
     * 
     * @param encoding
     * @param resultApprover
     * @param contentProviderService
     */
    public DocumentContentConverter(String encoding, ResultApprover resultApprover,
            ContentProviderService contentProviderService) {
        super(resultApprover);
        this.encoding = encoding;
        this.contentProviderService = contentProviderService;
    }

    @Override
    public DocumentContent buildObject(Result source, Oaf resolvedOafObject) throws IOException {
        eu.dnetlib.data.proto.ResultProtos.Result sourceResult = resolvedOafObject.getEntity() != null
                ? resolvedOafObject.getEntity().getResult() : null;
        if (sourceResult == null) {
            log.error("skipping: no result object " + "for a row " + new String(source.getRow(), encoding));
            return null;
        }
        if (resolvedOafObject.getEntity().getId() != null) {
            byte[] content = contentProviderService.getContent(resolvedOafObject.getEntity().getId(), null);
            if (content != null && content.length > 0) {
                DocumentContent.Builder builder = DocumentContent.newBuilder();
                builder.setId(resolvedOafObject.getEntity().getId());
                builder.setPdf(ByteBuffer.wrap(content));
                return builder.build();
            }
            return null;

        } else {
            log.error("skipping: no id specified for " + "result of a row " + new String(source.getRow(), encoding));
            return null;
        }
    }
}
