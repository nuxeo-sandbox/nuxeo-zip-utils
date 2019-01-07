package nuxeo.zip.utils.operations;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

import nuxeo.zip.utils.UnzipToDocuments;

/**
 *
 */
@Operation(id = UnzipToDocumentsOp.ID, category = Constants.CAT_BLOB, label = "ZipUtils: Unzip to Documents", description = "Unzips the blob and creates the"
        + " same structure in the target (the parent container when input is a Document and target is not provided)."
        + " When using a blob as input, the target parameter is required. When input is a document, blob is read in the xpath, file:content by default."
        + " The operation does nothing if the input is null."
        + " It is possible the type of Folderish to create for containers, default le Folder."
        + " Every commitModulo documents created, the transaction is commited."
        + " mainFolderishType and mainFolderishName atre optional, used for zips not containing a min folder to unzip in."
        + " Returns created container.")
public class UnzipToDocumentsOp {

    public static final String ID = "ZipUtils.UnzipToDocuments";

    public static final String CONTEXT_VAR_NAME = "zipInfo_isZip";

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    @Param(name = "target", required = false)
    protected DocumentModel target;

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath = "file:content";

    @Param(name = "folderishType", required = false, values = { "Folder" })
    protected String folderishType = "Folder";

    @Param(name = "commitModulo", required = false, values = { "100" })
    protected Integer commitModulo = 100;

    @Param(name = "mainFolderishType", required = false, values = { "Folder" })
    protected String mainFolderishType = "Folder";

    @Param(name = "mainFolderishName", required = false)
    protected String mainFolderishName;

    @OperationMethod
    public DocumentModel run(DocumentModel input) {

        if (input == null) {
            return null;
        }

        DocumentRef parent = input.getParentRef();
        DocumentModel parentDocument;

        if (target == null) {
            target = session.getDocument(parent);
        }

        if (StringUtils.isBlank(xpath)) {
            xpath = "file:content";
        }
        Blob zipBlob = (Blob) input.getPropertyValue(xpath);

        DocumentModel result = doUnzip(zipBlob);

        return result;
    }

    @OperationMethod
    public DocumentModel run(Blob input) {

        if (input == null) {
            return null;
        }

        if (target == null) {
            throw new IllegalArgumentException("When receiving a Blob, the target parameter cannot be empty");
        }

        DocumentModel result = doUnzip(input);
        return result;
    }

    protected DocumentModel doUnzip(Blob zipBlob) {

        UnzipToDocuments unzipToDocs = new UnzipToDocuments(target, zipBlob);
        unzipToDocs.setFolderishType(folderishType);
        unzipToDocs.setCommitModulo(commitModulo);
        unzipToDocs.setMainFolderishType(mainFolderishType);
        unzipToDocs.setMainFolderishName(mainFolderishName);
        DocumentModel result = unzipToDocs.run();

        return result;
    }

}
