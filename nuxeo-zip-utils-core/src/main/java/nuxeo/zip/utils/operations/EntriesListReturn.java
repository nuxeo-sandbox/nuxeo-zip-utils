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

import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Returns the sorted list of entries in a zip blob as a text/plain Blob (one entry per line, sorted).
 * <p>
 * Unlike {@link EntriesList} which only sets a context variable, this operation returns the data
 * directly so it can be consumed from REST clients (e.g. the Web UI element).
 *
 * @since 2025.2
 */
@Operation(id = EntriesListReturn.ID, category = Constants.CAT_BLOB,
        label = "ZipUtils: Get Entries List (returning)",
        description = "Returns the sorted list of entries in the zip as a text/plain Blob"
                + " (one entry per line). If input is a Document, use xpath (default file:content)."
                + " If the property at xpath is a list of blobs (e.g. files:files), use blobIndex"
                + " to pick which one (0-based, default 0). Returns null if no blob is found.")
public class EntriesListReturn {

    public static final String ID = "ZipUtils.EntriesListReturn";

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath = "file:content";

    @Param(name = "blobIndex", required = false)
    protected Integer blobIndex = 0;

    @OperationMethod
    public Blob run(DocumentModel input) throws IOException {
        return toBlob(resolveBlob(input));
    }

    @OperationMethod
    public Blob run(Blob input) throws IOException {
        return toBlob(input);
    }

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

    protected Blob toBlob(Blob input) throws IOException {
        if (input == null) {
            return null;
        }
        try (InputStream stream = input.getStream()) {
            var names = ZipUtils.getEntryNames(stream);
            names.sort(null);
            var text = String.join("\n", names);
            var result = Blobs.createBlob(text, "text/plain", "UTF-8");
            result.setFilename("zip-entries.txt");
            return result;
        }
    }
}
