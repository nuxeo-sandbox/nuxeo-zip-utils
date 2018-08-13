package nuxeo.zip.utils.operations;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;

/**
 *
 */
@Operation(id=GetFile.ID, category=Constants.CAT_BLOB, label="ZipUtils: Get File", description="Given the name (path) of an entry in the zip, "
        + "returns the corresponding file. Return null if the entry does not exist or is a folder."
        + " Assumes the input blob is a valid zip file."
        + " If input is a document, xpath can be used (default is file:content)")
public class GetFile {

    public static final String ID = "Document.GetFile";

    @Context
    protected CoreSession session;

    @Context
    protected MimetypeRegistry mimeTypeService;

    @Param(name = "entryName", required = true)
    protected String entryName;

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath = "file:content";

    @OperationMethod
    public Blob run(DocumentModel input) throws IOException {

        Blob blob = (Blob) input.getPropertyValue(xpath);

        return getFile(blob);
    }

    @OperationMethod
    public Blob run(Blob input) throws IOException {

        return getFile(input);
    }

    protected Blob getFile(Blob input) throws IOException {

        Blob result = null;

        if (input != null) {
            try (InputStream blobStream = input.getStream()) {
                try(ZipInputStream zipStream = new ZipInputStream(blobStream)) {
                    ZipEntry entry = zipStream.getNextEntry();
                    while (entry != null) {
                        if (!entry.isDirectory() && entry.getName().equals(entryName)) {
                            result = Blobs.createBlob(zipStream);
                            if(result != null) {
                                String fileName = StringUtils.substringAfterLast(entryName, "/");
                                result.setFilename(fileName);

                                String mimeType = mimeTypeService.getMimetypeFromFilename(fileName);
                                result.setMimeType(mimeType);
                            }
                            break;
                        }
                        entry = zipStream.getNextEntry();
                    }
                }
            }
        }

        return result;
    }
}
