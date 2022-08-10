package nuxeo.zip.utils;

import static org.junit.Assert.*;

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

import nuxeo.zip.utils.operations.CheckIsZip;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.thumbnail",
        "org.nuxeo.ecm.platform.picture.core",
        "org.nuxeo.ecm.platform.tag",
        "org.nuxeo.ecm.platform.commandline.executor",
        "org.nuxeo.ecm.platform.rendition.core" })
@Deploy("nuxeo.zip.utils.nuxeo-zip-utils-core")
@Deploy("nuxeo.zip.utils.nuxeo-zip-utils-core:disable-listeners-contrib.xml")
public class TestCheckIsZip {

    public static final String VALID_ZIP = "valid-zip.zip";

    public static final String NOT_VALID_ZIP = "not-valid-zip.zip";

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Test
    public void testIsZipWithBlob() throws OperationException {

        File f = FileUtils.getResourceFileFromContext(VALID_ZIP);

        FileBlob blob = new FileBlob(f);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(blob);
        Blob result = (Blob) automationService.run(ctx, CheckIsZip.ID);

        assertEquals(blob, result);
        assertTrue((boolean) ctx.get(CheckIsZip.CONTEXT_VAR_NAME));
    }

    @Test
    public void testIsNotZipWithBlob() throws OperationException {

        File f = FileUtils.getResourceFileFromContext(NOT_VALID_ZIP);

        FileBlob blob = new FileBlob(f);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(blob);
        Blob result = (Blob) automationService.run(ctx, CheckIsZip.ID);

        assertEquals(blob, result);
        assertFalse((boolean) ctx.get(CheckIsZip.CONTEXT_VAR_NAME));
    }

    @Test
    public void testIsZipWithDocument() throws OperationException {

        DocumentModel doc = TestUtils.createDocumentFromFile(session, "/", "File", VALID_ZIP);

        OperationContext ctx = new OperationContext(session);
        // Check with no parameters
        ctx.setInput(doc);
        DocumentModel result = (DocumentModel) automationService.run(ctx, CheckIsZip.ID);
        // Document should not be modified
        assertEquals (doc.getChangeToken(), result.getChangeToken());

        assertTrue((boolean) ctx.get(CheckIsZip.CONTEXT_VAR_NAME));

    }

    @Test
    public void testIsNotZipWithDocumentAndXpath() throws OperationException {

        DocumentModel doc = TestUtils.createDocumentFromFile(session, "/", "File", VALID_ZIP);

        // Using thumb:thumbnail here to avoid creating a dedicated schema
        doc = TestUtils.setThumbnailFieldFromFile(doc, NOT_VALID_ZIP);
        doc = session.saveDocument(doc);

        OperationContext ctx = new OperationContext(session);
        Map<String, Object> params = new HashMap<>();
        params.put("xpath", "thumb:thumbnail");
        ctx.setInput(doc);
        DocumentModel result = (DocumentModel) automationService.run(ctx, CheckIsZip.ID, params);
        // Document should not be modified
        assertEquals (doc.getChangeToken(), result.getChangeToken());

        assertFalse((boolean) ctx.get(CheckIsZip.CONTEXT_VAR_NAME));

    }
}
