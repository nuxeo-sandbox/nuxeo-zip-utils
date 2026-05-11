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
 *     Thibaud Arguillere (With the help of OpenCode / Claude Opus)
 */
package nuxeo.zip.utils.operations;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;

/** Sets {@code zipInfo_countFiles}, {@code zipInfo_countDirectories} and {@code zipInfo_comment} context variables. */
@Operation(id = ZipInfo.ID, category = Constants.CAT_BLOB, label = "ZipUtils: Zip Info", description = "Return info about the zip in Context Variables: "
        + " zipInfo_countFiles, zipInfo_countDirectories, and zipInfo_comment."
        + " If the input is a document, use xpath for the blob to use (default is file:content)."
        + " Assumes the blob is a zip. Returns the input unchanged.")
public class ZipInfo {

    public static final String ID = "ZipUtils.ZipInfo";

    public static final String CTX_VAR_COMMENT = "zipInfo_comment";

    public static final String CTX_VAR_COUNT_FILES = "zipInfo_countFiles";

    public static final String CTX_VAR_COUNT_DIRECTORIES = "zipInfo_countDirectories";

    @Context
    protected OperationContext ctx;

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath = "file:content";

    @OperationMethod
    public DocumentModel run(DocumentModel input) throws IOException {
        var blob = (Blob) input.getPropertyValue(xpath);
        getZipInto(blob);
        return input;
    }

    @OperationMethod
    public Blob run(Blob input) throws IOException {
        getZipInto(input);
        return input;
    }

    protected void getZipInto(Blob input) throws IOException {
        int countFiles = 0;
        int countDirectories = 0;

        ctx.put(CTX_VAR_COMMENT, "");
        ctx.put(CTX_VAR_COUNT_FILES, -1);
        ctx.put(CTX_VAR_COUNT_DIRECTORIES, -1);

        if (input != null) {
            File zipBlobFile = input.getFile();
            try (var zipFile = new ZipFile(zipBlobFile)) {
                ctx.put(CTX_VAR_COMMENT, zipFile.getComment() == null ? "" : zipFile.getComment());
                var entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    if (entry.isDirectory()) {
                        countDirectories += 1;
                    } else {
                        countFiles += 1;
                    }
                }
            }
        }
        ctx.put(CTX_VAR_COUNT_FILES, countFiles);
        ctx.put(CTX_VAR_COUNT_DIRECTORIES, countDirectories);
    }
}
