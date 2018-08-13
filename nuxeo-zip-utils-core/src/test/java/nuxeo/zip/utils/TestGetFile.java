package nuxeo.zip.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import nuxeo.zip.utils.operations.GetFile;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("nuxeo.zip.utils.nuxeo-zip-utils-core")
public class TestGetFile {

    public static final String VALID_ZIP = "valid-zip.zip";

    public static final String VALID_PATH = "f2/Picture.jpg";

    public static final String FILE_NAME = "Picture.jpg";

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Test
    public void testWithBlob() throws OperationException {

        File f = FileUtils.getResourceFileFromContext(VALID_ZIP);

        FileBlob blob = new FileBlob(f);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(blob);
        Map<String, Object> params = new HashMap<>();
        params.put("entryName", VALID_PATH);
        Blob result = (Blob) automationService.run(ctx, GetFile.ID, params);
        assertNotNull(result);

        assertEquals(FILE_NAME, result.getFilename());
        assertNotNull(result.getMimeType());
        assertTrue(result.getMimeType().toLowerCase().startsWith("image/"));

    }

    @Test
    public void testWithDocument() throws OperationException {

        DocumentModel doc = TestUtils.createDocumentFromFile(session, "/", "File", VALID_ZIP);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Map<String, Object> params = new HashMap<>();
        params.put("entryName", VALID_PATH);
        Blob result = (Blob) automationService.run(ctx, GetFile.ID, params);
        assertNotNull(result);

        assertEquals(FILE_NAME, result.getFilename());
        assertNotNull(result.getMimeType());
        assertTrue(result.getMimeType().toLowerCase().startsWith("image/"));

    }
}
