package org.nuxeo.utils.archive.operations;

import java.io.IOException;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
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
import org.nuxeo.utils.archive.utils.ArchiveUtils;

/**
 *
 */
@Operation(id = GetArchiveEntryOp.ID, category = Constants.CAT_BLOB, label = "Detect archive type", description = "Set the zipInfo_isZip Context Variable to true or false."
        + " If the input is a document, use xpath for the blob to use (default is file:content). Returns the input unchanged.")
public class GetArchiveEntryOp {

    public static final String ID = "Archive.GetEntry";

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

        return getEntry(blob);
    }

    @OperationMethod
    public Blob run(Blob input) throws IOException {

        return getEntry(input);
    }

    protected Blob getEntry(Blob input) throws IOException {

        Blob result = null;

        if (input != null) {
            try (ArchiveInputStream i = ArchiveUtils.open(input)) {
                ArchiveEntry entry = null;
                while ((entry = i.getNextEntry()) != null) {
                    if (!i.canReadEntryData(entry)) {
                        // log something...
                        continue;
                    }
                    if (!entry.isDirectory() && entry.getName().equals(entryName)) {
                        result = Blobs.createBlob(i);
                        if (result != null) {
                            String fileName = StringUtils.substringAfterLast(entryName, "/");
                            result.setFilename(fileName);

                            String mimeType = mimeTypeService.getMimetypeFromFilename(fileName);
                            result.setMimeType(mimeType);
                        }
                        break;
                    }
                }
            } catch (ArchiveException aex) {
                throw new IOException(aex);
            }
        }

        return result;
    }
}
