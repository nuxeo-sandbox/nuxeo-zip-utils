/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thibaud Arguillere
 */
package nuxeo.zip.utils.operations;

import java.io.IOException;
import java.io.InputStream;
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

/** Sets context variables describing a single zip entry (size, compressed size, CRC, method). */
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
        var blob = (Blob) input.getPropertyValue(xpath);
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
        // 0 = stored, 8 = Deflated (compressed)
        ctx.put(CTX_VAR_METHOD, -1);

        if (input == null) {
            return;
        }
        try (InputStream blobStream = input.getStream();
                ZipInputStream zipStream = new ZipInputStream(blobStream)) {
            var entry = zipStream.getNextEntry();
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
