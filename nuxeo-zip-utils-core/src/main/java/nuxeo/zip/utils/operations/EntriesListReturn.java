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

    @SuppressWarnings("unchecked")
    protected Blob resolveBlob(DocumentModel input) {
        Object value = input.getPropertyValue(xpath);
        if (value == null) {
            return null;
        }
        if (value instanceof Blob) {
            return (Blob) value;
        }
        if (value instanceof List) {
            List<?> items = (List<?>) value;
            int idx = blobIndex == null ? 0 : blobIndex.intValue();
            if (idx < 0 || idx >= items.size()) {
                throw new NuxeoException("blobIndex " + idx + " is out of range for property '" + xpath
                        + "' (size " + items.size() + ")");
            }
            Object item = items.get(idx);
            if (item instanceof Blob) {
                return (Blob) item;
            }
            // Complex-list schema (e.g. files:files where item is { file: <Blob>, filename: ... }).
            // Find the first Blob-valued field in the map.
            if (item instanceof Map) {
                for (Object v : ((Map<String, Object>) item).values()) {
                    if (v instanceof Blob) {
                        return (Blob) v;
                    }
                }
            }
            throw new NuxeoException("Item at '" + xpath + "/" + idx + "' does not contain a Blob");
        }
        throw new NuxeoException("Property '" + xpath + "' is neither a Blob nor a list of Blobs");
    }

    protected Blob toBlob(Blob input) throws IOException {
        if (input == null) {
            return null;
        }
        try (InputStream stream = input.getStream()) {
            List<String> names = ZipUtils.getEntryNames(stream);
            names.sort(null);
            String text = String.join("\n", names);
            Blob result = Blobs.createBlob(text, "text/plain", "UTF-8");
            result.setFilename("zip-entries.txt");
            return result;
        }
    }
}
