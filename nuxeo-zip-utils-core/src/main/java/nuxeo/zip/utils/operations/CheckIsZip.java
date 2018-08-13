package nuxeo.zip.utils.operations;

import java.io.IOException;
import java.io.InputStream;

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
@Operation(id = CheckIsZip.ID, category = Constants.CAT_BLOB, label = "ZipUtils: Is Zip", description = "Set the zipInfo_isZip Context Variable to true or false."
        + " If the input is a document, use xpath for the blob to use (default is file:content). Returns the input unchanged.")
public class CheckIsZip {

    public static final String ID = "ZipUtils.IsZip";

    public static final String CONTEXT_VAR_NAME = "zipInfo_isZip";

    @Context
    protected OperationContext ctx;

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath = "file:content";

    @OperationMethod
    public DocumentModel run(DocumentModel input) {

        Blob blob = (Blob) input.getPropertyValue(xpath);

        checkIsZip(blob);

        return input;

    }

    @OperationMethod
    public Blob run(Blob input) {

        checkIsZip(input);

        return input;

    }

    protected void checkIsZip(Blob input) {

        boolean isZip = false;

        if (input != null) {
            try (InputStream blobStream = input.getStream()) {
                isZip = ZipUtils.isValid(blobStream);
            } catch (IOException e) {
                // Nothing
            }
        }

        ctx.put(CONTEXT_VAR_NAME, isZip);
    }
}
