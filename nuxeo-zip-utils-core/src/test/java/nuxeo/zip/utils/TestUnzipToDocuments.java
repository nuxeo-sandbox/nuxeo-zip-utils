package nuxeo.zip.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.video.api", "org.nuxeo.ecm.platform.video.core",
     "org.nuxeo.ecm.platform.picture.api", "org.nuxeo.ecm.platform.picture.core",
     "org.nuxeo.ecm.platform.tag",
    "org.nuxeo.ecm.platform.filemanager.core", "org.nuxeo.ecm.platform.types.core" })
@Deploy("nuxeo.zip.utils.nuxeo-zip-utils-core")
@Deploy("nuxeo.zip.utils.nuxeo-zip-utils-core:disable-listeners-contrib.xml")
public class TestUnzipToDocuments {

    // This zip file has files and folders at the root.
    public static final String FILES_AND_FOLDERS_ZIP = "files-and-folders.zip";
    private static final HashMap<String, String> PATHS_AND_DOCTYPES_FILESANDFOLDERS = new HashMap<String, String>();
    static {
        PATHS_AND_DOCTYPES_FILESANDFOLDERS.put("/files-and-folders/File.pdf", "File");
        PATHS_AND_DOCTYPES_FILESANDFOLDERS.put("/files-and-folders/f1", "Folder");
        PATHS_AND_DOCTYPES_FILESANDFOLDERS.put("/files-and-folders/f1/f1-f1", "Folder");
        PATHS_AND_DOCTYPES_FILESANDFOLDERS.put("/files-and-folders/f1/f1-f1/Video.mp4", "Video");
        PATHS_AND_DOCTYPES_FILESANDFOLDERS.put("/files-and-folders/f2", "Folder");
        PATHS_AND_DOCTYPES_FILESANDFOLDERS.put("/files-and-folders/f2/Picture.jpg", "Picture");
    }

    // In a test we  ask to created our own main Workspace as destination for the unzipping
    private static final HashMap<String, String> PATHS_AND_DOCTYPES_ZIP1_CUSTOM = new HashMap<String, String>();
    static {
        PATHS_AND_DOCTYPES_ZIP1_CUSTOM.put("/TEST", "Workspace");
        PATHS_AND_DOCTYPES_ZIP1_CUSTOM.put("/TEST/File.pdf", "File");
        PATHS_AND_DOCTYPES_ZIP1_CUSTOM.put("/TEST/f1", "Folder");
        PATHS_AND_DOCTYPES_ZIP1_CUSTOM.put("/TEST/f1/f1-f1", "Folder");
        PATHS_AND_DOCTYPES_ZIP1_CUSTOM.put("/TEST/f1/f1-f1/Video.mp4", "Video");
        PATHS_AND_DOCTYPES_ZIP1_CUSTOM.put("/TEST/f2", "Folder");
        PATHS_AND_DOCTYPES_ZIP1_CUSTOM.put("/TEST/f2/Picture.jpg", "Picture");
    }

    // valid-zip-2.zip expands inside a "nuxeo-unzip-test" folder
    public static final String VALID_ZIP_2 = "valid-zip-2.zip";
    private static final HashMap<String, String> PATHS_AND_DOCTYPES_ZIP2 = new HashMap<String, String>();
    static {
        PATHS_AND_DOCTYPES_ZIP2.put("/nuxeo-unzip-test/File.pdf", "File");
        PATHS_AND_DOCTYPES_ZIP2.put("/nuxeo-unzip-test/f1", "Folder");
        PATHS_AND_DOCTYPES_ZIP2.put("/nuxeo-unzip-test/f1/f1-f1", "Folder");
        PATHS_AND_DOCTYPES_ZIP2.put("/nuxeo-unzip-test/f1/f1-f1/Video.mp4", "Video");
        PATHS_AND_DOCTYPES_ZIP2.put("/nuxeo-unzip-test/f2", "Folder");
        PATHS_AND_DOCTYPES_ZIP2.put("/nuxeo-unzip-test/f2/Picture.jpg", "Picture");
    }

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected AutomationService automationService;

    protected DocumentModel testDocsFolder;

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

    protected void checkUnzippedContent(Map<String, String> expectedValues) {

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
                assertTrue("Document " + subPath + " was not created", false);
            }
        }
    }

    @Test
    // This test just validates that the unzip and import works; it does not check the root document name, for example.
    public void shouldUnzipBlob() {

        File f = FileUtils.getResourceFileFromContext(FILES_AND_FOLDERS_ZIP);

        FileBlob blob = new FileBlob(f);

        UnzipToDocuments unzipToDocs = new UnzipToDocuments(testDocsFolder, blob);

        DocumentModel mainUnzippedFolderDoc = unzipToDocs.run();
        assertNotNull(mainUnzippedFolderDoc);

        checkUnzippedContent(PATHS_AND_DOCTYPES_FILESANDFOLDERS);

    }

    @Test
    public void testWithBlobAndZip1AndCustomMainFolder() {

        File f = FileUtils.getResourceFileFromContext(FILES_AND_FOLDERS_ZIP);

        FileBlob blob = new FileBlob(f);

        UnzipToDocuments unzipToDocs = new UnzipToDocuments(testDocsFolder, blob);
        unzipToDocs.setMainFolderishName("TEST");
        unzipToDocs.setMainFolderishType("Workspace");

        DocumentModel mainUnzippedFolderDoc = unzipToDocs.run();
        assertNotNull(mainUnzippedFolderDoc);

        checkUnzippedContent(PATHS_AND_DOCTYPES_ZIP1_CUSTOM);

    }

    @Test
    public void testWithBlobAndZip2() throws IOException {

        File f = FileUtils.getResourceFileFromContext(VALID_ZIP_2);

        FileBlob blob = new FileBlob(f);

        UnzipToDocuments unzipToDocs = new UnzipToDocuments(testDocsFolder, blob);

        DocumentModel mainUnzippedFolderDoc = unzipToDocs.run();
        assertNotNull(mainUnzippedFolderDoc);

        checkUnzippedContent(PATHS_AND_DOCTYPES_ZIP2);

    }

    // TODO: Test zip with no folders, only paths to files in folders

    // TODO: Test zip with/without parent doc

    // TODO: Test zip with no folders at all, all files at root
    // Should therefore create a root document to store them.
}
