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

/**
 *
 */
@Operation(id = EntriesList.ID, category = Constants.CAT_BLOB, label = "ZipUtils: Get Entries List", description = "Set the zipInfo_entriesList Context Variable"
        + " to to the full list of entries in the zip, ordered, as text, one entry/line."
        + " If the input is a document, use xpath for the blob to use (default is file:content)."
        + " Assumes the blob is a valid zip. Returns the input unchanged.")
public class EntriesList {

    public static final String ID = "ZipUtils.EntriesList";

    public static final String CONTEXT_VAR_NAME = "zipInfo_entriesList";

    @Context
    protected OperationContext ctx;

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath = "file:content";

    @OperationMethod
    public DocumentModel run(DocumentModel input) throws IOException {

        Blob blob = (Blob) input.getPropertyValue(xpath);

        getEntries(blob);

        return input;

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
