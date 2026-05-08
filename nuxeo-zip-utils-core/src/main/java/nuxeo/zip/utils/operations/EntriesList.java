package nuxeo.zip.utils.operations;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 *
 */
@Operation(id = EntriesList.ID, category = Constants.CAT_BLOB, label = "ZipUtils: Get Entries List", description = "Set the zipInfo_entriesList Context Variable"
        + " to to the full list of entries in the zip, ordered, as text, one entry/line."
        + " If the input is a document, use xpath for the blob to use (default is file:content)."
        + " If the property at xpath is a list of blobs (e.g. files:files), use blobIndex to pick which one (0-based, default 0)."
        + " Assumes the blob is a valid zip. Returns the input unchanged.")
public class EntriesList {

    public static final String ID = "ZipUtils.EntriesList";

    public static final String CONTEXT_VAR_NAME = "zipInfo_entriesList";

    @Context
    protected OperationContext ctx;

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath = "file:content";

    /**
     * Index of the blob to use when {@code xpath} resolves to a list of blobs (e.g. {@code files:files}).
     * Ignored when {@code xpath} resolves to a single blob.
     *
     * @since 2025.2
     */
    @Param(name = "blobIndex", required = false)
    protected Integer blobIndex = 0;

    @OperationMethod
    public DocumentModel run(DocumentModel input) throws IOException {

        Blob blob = resolveBlob(input);

        getEntries(blob);

        return input;

    }

    /**
     * Resolves the blob at {@code xpath}, supporting both single-blob properties and list-of-blob properties.
     *
     * @since 2025.2
     */
    @SuppressWarnings("unchecked")
    protected Blob resolveBlob(DocumentModel input) {
        Object value = input.getPropertyValue(xpath);
        if (value == null) {
            return null;
        }
        if (value instanceof Blob) {
            return (Blob) value;
        }
        if (value instanceof List) {
            List<Blob> blobs = (List<Blob>) value;
            int idx = blobIndex == null ? 0 : blobIndex.intValue();
            if (idx < 0 || idx >= blobs.size()) {
                throw new NuxeoException("blobIndex " + idx + " is out of range for property '" + xpath
                        + "' (size " + blobs.size() + ")");
            }
            return blobs.get(idx);
        }
        throw new NuxeoException("Property '" + xpath + "' is neither a Blob nor a list of Blobs");
    }

    @OperationMethod
    public Blob run(Blob input) throws IOException {

        getEntries(input);

        return input;

    }

    protected void getEntries(Blob input) throws IOException {

        String entriesStr = "";

        ctx.put(CONTEXT_VAR_NAME, "");

        if (input != null) {
            try (InputStream blobStream = input.getStream()) {
                List<String> names = ZipUtils.getEntryNames(blobStream);
                names.sort(null);
                entriesStr = String.join("\n", names);
                ctx.put(CONTEXT_VAR_NAME, entriesStr);

            } catch (IOException e) {
                // Nothing
            }
        }
    }
}
