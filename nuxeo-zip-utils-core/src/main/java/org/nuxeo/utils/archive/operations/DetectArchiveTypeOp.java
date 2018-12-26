package org.nuxeo.utils.archive.operations;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.utils.archive.utils.ArchiveTypes;

/**
 *
 */
@Operation(id = DetectArchiveTypeOp.ID, category = Constants.CAT_BLOB, label = "Detect archive type", description = "")
public class DetectArchiveTypeOp {

    protected static final Log log = LogFactory.getLog(DetectArchiveTypeOp.class);

    public static final String ID = "Archive.DetectType";

    public static final String ARCHIVE_TYPE = "archive_type";

    public static final String COMPRESS_TYPE = "compress_type";

    @Context
    protected OperationContext ctx;

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath = "file:content";

    @Param(name = "updateMimeType", required = false)
    protected boolean updateMimeType = false;

    @Param(name = "save", required = false)
    protected boolean save = false;

    @OperationMethod
    public DocumentModel run(DocumentModel input) {

        Blob blob = (Blob) input.getPropertyValue(xpath);
        run(blob);

        if (updateMimeType) {
            if (!input.hasFacet("archive")) {
                input.addFacet("archive");
            }
            if (ctx.containsKey(COMPRESS_TYPE)) {
                String value = ArchiveTypes.mimeType((String) ctx.get(COMPRESS_TYPE));
                if (value != null) {
                    input.setProperty("archive", "encoding", value);
                }
            }
            if (ctx.containsKey(ARCHIVE_TYPE)) {
                String value = ArchiveTypes.mimeType((String) ctx.get(ARCHIVE_TYPE));
                if (value != null) {
                    input.setProperty("archive", "type", value);
                }
            }

            ctx.getCoreSession().saveDocument(input);
        }

        if (save) {
            ctx.getCoreSession().save();
        }

        return input;

    }

    @OperationMethod
    public Blob run(Blob input) {

        checkArchive(input);

        if (updateMimeType) {
            String mt = null;
            if (ctx.containsKey(COMPRESS_TYPE)) {
                mt = ArchiveTypes.mimeType((String) ctx.get(COMPRESS_TYPE));
            } else if (ctx.containsKey(ARCHIVE_TYPE)) {
                mt = ArchiveTypes.mimeType((String) ctx.get(ARCHIVE_TYPE));
            }
            if (mt != null) {
                input.setMimeType(mt);
            }
        }

        return input;

    }

    protected void checkArchive(Blob input) {

        if (input != null) {
            InputStream blobStream = null;
            try {
                blobStream = new BufferedInputStream(input.getStream());
                try {
                    String compress = CompressorStreamFactory.detect(blobStream);
                    ctx.put(COMPRESS_TYPE, compress);

                    // Unwrap...
                    blobStream = CompressorStreamFactory.getSingleton()
                                                        .createCompressorInputStream(compress, blobStream);
                    blobStream = new BufferedInputStream(blobStream);
                } catch (Exception ce) {
                    // Ignore
                    log.debug("Compressor detection", ce);
                }
                try {
                    String archive = ArchiveStreamFactory.detect(blobStream);
                    ctx.put(ARCHIVE_TYPE, archive);
                } catch (Exception ae) {
                    // Ignore
                    log.debug("Archive detection", ae);
                }
            } catch (Exception e) {
                // Nothing
                log.debug("Stream detection", e);
            } finally {
                if (blobStream != null) {
                    try {
                        blobStream.close();
                    } catch (IOException e) {
                        // Ignore
                        log.debug("Close error", e);
                    }
                }
            }
        }

    }
}
