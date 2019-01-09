package nuxeo.zip.utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

import javax.inject.Inject;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({"org.nuxeo.ecm.platform.video.api", "org.nuxeo.ecm.platform.video.core",
    "org.nuxeo.ecm.platform.picture.api", "org.nuxeo.ecm.platform.picture.core",
    "org.nuxeo.ecm.platform.tag",
    "org.nuxeo.ecm.platform.filemanager.core", "org.nuxeo.ecm.platform.types.core"})
@Deploy("nuxeo.zip.utils.nuxeo-zip-utils-core")
@Deploy("nuxeo.zip.utils.nuxeo-zip-utils-core:disable-listeners-contrib.xml")
public class TestUnzipToDocumentsSimple {

    private static final String FILES_AND_FOLDERS_ZIP = "without-root-folder-entry.zip";
    private static final HashMap<String, String> PATHS_AND_DOCTYPES_FILESANDFOLDERS = new HashMap<>();

    static {
        PATHS_AND_DOCTYPES_FILESANDFOLDERS.put("/folder1", "Folder");
        PATHS_AND_DOCTYPES_FILESANDFOLDERS.put("/folder1/File.pdf", "File");
        PATHS_AND_DOCTYPES_FILESANDFOLDERS.put("/folder1/folder2", "Folder");
        PATHS_AND_DOCTYPES_FILESANDFOLDERS.put("/folder1/folder2/Picture.jpg", "Picture");
    }

    @Inject
    protected CoreSession coreSession;

    private DocumentModel testDocsFolder;

    @Before
    public void setup() {

        testDocsFolder = coreSession.createDocumentModel("/", "test-unzip", "Folder");
        testDocsFolder.setPropertyValue("dc:title", "test-unzip");
        testDocsFolder = coreSession.createDocument(testDocsFolder);
        testDocsFolder = coreSession.saveDocument(testDocsFolder);

        coreSession.save();

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    @After
    public void cleanup() {
        coreSession.removeDocument(testDocsFolder.getRef());
        coreSession.save();
    }

    private void checkUnzippedContent(Map<String, String> expectedValues) {

        String mainParentPath = testDocsFolder.getPathAsString();
        String testPath;
        DocumentModel testDoc;

        for (Entry<String, String> entry : expectedValues.entrySet()) {
            String subPath = entry.getKey();
            String expectedType = entry.getValue();

            testPath = mainParentPath + subPath;
            DocumentRef docRef = new PathRef(testPath);
            try {
                testDoc = coreSession.getDocument(docRef);
                assertEquals(expectedType, testDoc.getType());
            } catch (DocumentNotFoundException e) {
                fail("Document " + subPath + " was not created", false);
            }
        }
    }

    @Test
    public void shouldUnzipFilesAndFolders() {

        File f = FileUtils.getResourceFileFromContext(FILES_AND_FOLDERS_ZIP);

        FileBlob blob = new FileBlob(f);

        UnzipToDocuments unzipToDocs = new UnzipToDocuments(testDocsFolder, blob);

        DocumentModel mainUnzippedFolderDoc = unzipToDocs.run();
        assertNotNull(mainUnzippedFolderDoc);

        checkUnzippedContent(PATHS_AND_DOCTYPES_FILESANDFOLDERS);

    }

}
