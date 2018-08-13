package nuxeo.zip.utils;

import static org.junit.Assert.*;

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

import nuxeo.zip.utils.operations.EntriesList;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("nuxeo.zip.utils.nuxeo-zip-utils-core")
public class TestEntriesList {

    public static final String VALID_ZIP = "valid-zip.zip";

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Test
    public void testEntriesWithBlob() throws OperationException {

        File f = FileUtils.getResourceFileFromContext(VALID_ZIP);

        FileBlob blob = new FileBlob(f);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(blob);
        Blob ignore = (Blob) automationService.run(ctx, EntriesList.ID);

        String entriesStr = (String) ctx.get(EntriesList.CONTEXT_VAR_NAME);
        assertNotNull(entriesStr);
        // Check it's ordered
        boolean isOrdered = looksSorted(entriesStr);
        assertTrue("The result should be ordered", isOrdered);

    }

    @Test
    public void testEntriesWithDocument() throws OperationException {

        DocumentModel doc = TestUtils.createDocumentFromFile(session, "/", "File", VALID_ZIP);

        OperationContext ctx = new OperationContext(session);
        // Check with no parameters
        ctx.setInput(doc);
        DocumentModel result = (DocumentModel) automationService.run(ctx, EntriesList.ID);
        // Document should not be modified
        assertEquals (doc.getChangeToken(), result.getChangeToken());

        String entriesStr = (String) ctx.get(EntriesList.CONTEXT_VAR_NAME);
        assertNotNull(entriesStr);
        // Check it's ordered
        boolean isOrdered = looksSorted(entriesStr);
        assertTrue("The result should be ordered", isOrdered);


    }

    protected boolean looksSorted(String strWithLines) {

        String [] check = strWithLines.split("\n");
        int max = check.length;
        for(int i = 1; i < max; i++) {
            if(check[i - 1].compareTo(check[i]) > 0) {
                return false;
            }
        }

        return true;
    }
}
