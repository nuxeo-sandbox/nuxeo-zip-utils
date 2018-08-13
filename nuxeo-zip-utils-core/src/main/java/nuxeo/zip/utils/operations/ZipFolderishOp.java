package nuxeo.zip.utils.operations;

import java.io.IOException;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

import nuxeo.zip.utils.ZipFolderish;

/**
 *
 */
@Operation(id = ZipFolderishOp.ID, category = Constants.CAT_BLOB, label = "ZipUtils: Zip Folderish", description = "Zip the content of a input Folderish document, "
        + " returns the zipped blob."
        + " By default, it uses file:content. This can be overriden by passing the ID of a chain/operation in callbackChain (the chain receives a Document, must return a blob)"
        + " It is also possible to override the default WHERE clause added to filter children (typical not hidden, not a version, ...)"
        + " when doNotCreateMainFolder is true, the zip archive TOC does not start with the title of the input folder ")
public class ZipFolderishOp {

    public static final String ID = "ZipUtils.ZipFolderish";

    public static final String CONTEXT_VAR_NAME = "zipInfo_isZip";

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    @Param(name = "callbackChain", required = false)
    protected String callbackChain = null;

    @Param(name = "whereClauseOverride", required = false)
    protected String whereClauseOverride = "";

    @Param(name = "doNotCreateMainFolder", required = false)
    protected Boolean doNotCreateMainFolder = null;

    @OperationMethod
    public Blob run(DocumentModel input) throws IOException {

        if (input == null || !input.isFolder()) {
            return null;
        }

        ZipFolderish zipFolderish = new ZipFolderish(input);
        zipFolderish.setGetBlolbCallbackChain(callbackChain);
        zipFolderish.setGetCchildrenWhereClause(whereClauseOverride);

        Blob result = zipFolderish.run(doNotCreateMainFolder == null ? false : doNotCreateMainFolder);

        return result;
    }

}
