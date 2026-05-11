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

import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;

/** Sets the {@value #CONTEXT_VAR_NAME} context variable to {@code true} if the input blob is a valid zip. */
@Operation(id = CheckIsZip.ID, category = Constants.CAT_BLOB, label = "ZipUtils: Is Zip", description = "Set the zipInfo_isZip Context Variable to true or false."
        + " If the input is a document, use xpath for the blob to use (default is file:content). Returns the input unchanged.")
public class CheckIsZip {

    private static final Logger log = LogManager.getLogger(CheckIsZip.class);

    public static final String ID = "ZipUtils.IsZip";

    public static final String CONTEXT_VAR_NAME = "zipInfo_isZip";

    @Context
    protected OperationContext ctx;

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath = "file:content";

    @OperationMethod
    public DocumentModel run(DocumentModel input) {
        var blob = (Blob) input.getPropertyValue(xpath);
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
                // Treat unreadable blob as "not a zip" but log so failures are not silently lost.
                log.warn("Failed to read blob {} while checking zip validity", input.getFilename(), e);
            }
        }
        ctx.put(CONTEXT_VAR_NAME, isZip);
    }
}
