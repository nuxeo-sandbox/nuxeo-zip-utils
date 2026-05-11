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
import java.util.List;
import java.util.Map;

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
import org.nuxeo.ecm.core.api.NuxeoException;

/** Sets the {@value #CONTEXT_VAR_NAME} context variable to the sorted list of entries in a zip blob. */
@Operation(id = EntriesList.ID, category = Constants.CAT_BLOB, label = "ZipUtils: Get Entries List", description = "Set the zipInfo_entriesList Context Variable"
        + " to to the full list of entries in the zip, ordered, as text, one entry/line."
        + " If the input is a document, use xpath for the blob to use (default is file:content)."
        + " If the property at xpath is a list of blobs (e.g. files:files), use blobIndex to pick which one (0-based, default 0)."
        + " Assumes the blob is a valid zip. Returns the input unchanged.")
public class EntriesList {

    private static final Logger log = LogManager.getLogger(EntriesList.class);

    public static final String ID = "ZipUtils.EntriesList";

    public static final String CONTEXT_VAR_NAME = "zipInfo_entriesList";

    @Context
    protected OperationContext ctx;

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath = "file:content";

    /**
     * Index of the blob to use when {@code xpath} resolves to a list of blobs (e.g. {@code files:files}).
     * Ignored when {@code xpath} resolves to a single blob.
     *
     * @since 2025.2
     */
    @Param(name = "blobIndex", required = false)
    protected Integer blobIndex = 0;

    @OperationMethod
    public DocumentModel run(DocumentModel input) throws IOException {
        var blob = resolveBlob(input);
        getEntries(blob);
        return input;
    }

    /**
     * Resolves the blob at {@code xpath}, supporting both single-blob properties and list-of-blob properties.
     *
     * @since 2025.2
     */
    protected Blob resolveBlob(DocumentModel input) {
        Object value = input.getPropertyValue(xpath);
        if (value == null) {
            return null;
        }
        if (value instanceof Blob blob) {
            return blob;
        }
        if (value instanceof List<?> items) {
            int idx = blobIndex == null ? 0 : blobIndex.intValue();
            if (idx < 0 || idx >= items.size()) {
                throw new NuxeoException("blobIndex %d is out of range for property '%s' (size %d)"
                        .formatted(idx, xpath, items.size()));
            }
            var item = items.get(idx);
            if (item instanceof Blob blob) {
                return blob;
            }
            // Complex-list schema (e.g. files:files where item is { file: <Blob>, filename: ... }).
            // Find the first Blob-valued field in the map.
            if (item instanceof Map<?, ?> map) {
                for (var v : map.values()) {
                    if (v instanceof Blob blob) {
                        return blob;
                    }
                }
            }
            throw new NuxeoException("Item at '%s/%d' does not contain a Blob".formatted(xpath, idx));
        }
        throw new NuxeoException("Property '%s' is neither a Blob nor a list of Blobs".formatted(xpath));
    }

    @OperationMethod
    public Blob run(Blob input) throws IOException {
        getEntries(input);
        return input;
    }

    protected void getEntries(Blob input) throws IOException {
        ctx.put(CONTEXT_VAR_NAME, "");
        if (input == null) {
            return;
        }
        try (InputStream blobStream = input.getStream()) {
            var names = ZipUtils.getEntryNames(blobStream);
            names.sort(null);
            ctx.put(CONTEXT_VAR_NAME, String.join("\n", names));
        } catch (IOException e) {
            // Leave the context variable empty and warn; callers asked for entries of a blob we cannot read.
            log.warn("Failed to read zip entries from blob {}", input.getFilename(), e);
        }
    }
}
