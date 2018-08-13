package nuxeo.zip.utils.operations;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 *
 */
@Operation(id = EntryInfo.ID, category = Constants.CAT_BLOB, label = "ZipUtils: Get Entry Info", description = "Given the name (path) of an entry in the zip, "
        + "returns the information in several context int/long variables:"
        + " zipInfo_compressedSize, zipInfo_originalSize, zipInfo_crc, and zipInfo_method (0 = stored, 8 =  compressed)."
        + " Assumes the blob is a valid zip file."
        + " Input is returned unchanged. If input is a document, xpath can be used (default is file:content)")
public class EntryInfo {

    public static final String ID = "Document.EntryInfo";

    public static final String CTX_VAR_SIZE = "zipInfo_compressedSize";

    public static final String CTX_VAR_COMPRESSED_SIZE = "zipInfo_originalSize";

    public static final String CTX_VAR_CRC = "zipInfo_crc";

    public static final String CTX_VAR_METHOD = "zipInfo_method";

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    @Param(name = "entryName", required = true)
    protected String entryName;

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath = "file:content";

    @OperationMethod
    public DocumentModel run(DocumentModel input) throws IOException {

        Blob blob = (Blob) input.getPropertyValue(xpath);

        getEntryInfo(blob);

        return input;

    }

    @OperationMethod
    public Blob run(Blob input) throws IOException {

        getEntryInfo(input);

        return input;

    }

    protected void getEntryInfo(Blob input) throws IOException {

        ctx.put(CTX_VAR_SIZE, (long) -1);
        ctx.put(CTX_VAR_COMPRESSED_SIZE, (long) -1);
        ctx.put(CTX_VAR_CRC, (long) -1);
        ctx.put(CTX_VAR_METHOD, -1);// 0 = stored, 8 == Deflated (compressed)

        if (input != null) {
            try (InputStream blobStream = input.getStream()) {
                try(ZipInputStream zipStream = new ZipInputStream(blobStream)) {
                    ZipEntry entry = zipStream.getNextEntry();
                    while (entry != null) {
                        if (entry.getName().equals(entryName)) {
                            ctx.put(CTX_VAR_SIZE, entry.getSize());
                            ctx.put(CTX_VAR_COMPRESSED_SIZE, entry.getCompressedSize());
                            ctx.put(CTX_VAR_CRC, entry.getCrc());
                            ctx.put(CTX_VAR_METHOD, entry.getMethod());
                            return;
                        }
                        entry = zipStream.getNextEntry();
                    }
                }

            }
        }

    }
}
