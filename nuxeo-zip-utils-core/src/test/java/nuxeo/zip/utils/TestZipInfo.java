package nuxeo.zip.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
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

import nuxeo.zip.utils.operations.ZipInfo;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("nuxeo.zip.utils.nuxeo-zip-utils-core")
public class TestZipInfo {

    public static final String VALID_ZIP = "valid-zip.zip";

    public static final String NOT_VALID_ZIP = "not-valid-zip.zip";

    public static final int COUNT_FILES = 3;

    public static final int COUNT_DIRECTORIES = 3;

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
        Blob result = (Blob) automationService.run(ctx, ZipInfo.ID);

        assertEquals("", ctx.get(ZipInfo.CTX_VAR_COMMENT));
        assertEquals(COUNT_FILES, (int) ctx.get(ZipInfo.CTX_VAR_COUNT_FILES));
        assertEquals(COUNT_DIRECTORIES, (int) ctx.get(ZipInfo.CTX_VAR_COUNT_DIRECTORIES));

    }

    @Test
    public void testWithDocument() throws OperationException {

        DocumentModel doc = TestUtils.createDocumentFromFile(session, "/", "File", VALID_ZIP);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        DocumentModel result = (DocumentModel) automationService.run(ctx, ZipInfo.ID);

        assertEquals("", ctx.get(ZipInfo.CTX_VAR_COMMENT));
        assertEquals(COUNT_FILES, (int) ctx.get(ZipInfo.CTX_VAR_COUNT_FILES));
        assertEquals(COUNT_DIRECTORIES, (int) ctx.get(ZipInfo.CTX_VAR_COUNT_DIRECTORIES));

    }

    @Test
    public void shouldFailWithNonZipBlob() {
        File f = FileUtils.getResourceFileFromContext(NOT_VALID_ZIP);

        FileBlob blob = new FileBlob(f);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(blob);
        try {
            Blob result = (Blob) automationService.run(ctx, ZipInfo.ID);
            assertTrue("Should hav raised an exception with an invalid zip", false);
        } catch (Exception e) {

        }
    }
}
