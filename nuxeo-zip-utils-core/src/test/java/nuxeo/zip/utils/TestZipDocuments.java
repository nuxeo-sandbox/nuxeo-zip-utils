package nuxeo.zip.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
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
@Deploy({ "org.nuxeo.ecm.platform.video.api", "org.nuxeo.ecm.platform.video.core", "org.nuxeo.ecm.platform.picture.api",
        "org.nuxeo.ecm.platform.picture.core", "org.nuxeo.ecm.platform.tag", "org.nuxeo.ecm.platform.filemanager.core",
        "org.nuxeo.ecm.platform.types.core" })
@Deploy("nuxeo.zip.utils.nuxeo-zip-utils-core")
@Deploy("nuxeo.zip.utils.nuxeo-zip-utils-core:disable-listeners-contrib.xml")
public class TestZipDocuments {

    public static final String VALID_ZIP = "valid-zip.zip";

    // valid-zip.zip test file contain path to a main folder, but default UnzipToDocuments will create one, named by the
    // zip file itself minus the extension.
    private static final List<String> PATHS_IN_ZIP1 = new ArrayList<String>();
    static {
        PATHS_IN_ZIP1.add("valid-zip/valid-zip/File.pdf");
        PATHS_IN_ZIP1.add("valid-zip/valid-zip/f1/");
        PATHS_IN_ZIP1.add("valid-zip/valid-zip/f1/f1-f1/");
        PATHS_IN_ZIP1.add("valid-zip/valid-zip/f1/f1-f1/Video.mp4");
        PATHS_IN_ZIP1.add("valid-zip/valid-zip/f2/");
        PATHS_IN_ZIP1.add("valid-zip/valid-zip/f2/Picture.jpg");
    }

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

    /*
     * We use UnzipToDocuments to create the documents. It it fails, well, go debug it in its dedicated unit test :-)
     */
    @Test
    public void testWithZip1() throws IOException {

        // =====================================> Create the Documents from the zip
        File f = FileUtils.getResourceFileFromContext(VALID_ZIP);

        FileBlob blob = new FileBlob(f);

        UnzipToDocuments unzipToDocs = new UnzipToDocuments(testDocsFolder, blob);

        DocumentModel mainUnzippedFolderDoc = unzipToDocs.run();
        assertNotNull(mainUnzippedFolderDoc);

        // =====================================> Zip the folder
        ZipFolderish zipFolderish = new ZipFolderish(mainUnzippedFolderDoc);
        Blob zipped = zipFolderish.run();
        assertNotNull(zipped);

        List<String> entries = ZipUtils.getEntryNames(zipped.getFile());
        for (String path : PATHS_IN_ZIP1) {
            if (!entries.contains(path)) {
                assertTrue("Missing entry: " + path, false);
            }
        }
    }

    @Test
    public void testWithZip1AndDuplicate() throws IOException {

        // =====================================> Create the Documents from the zip
        File f = FileUtils.getResourceFileFromContext(VALID_ZIP);

        FileBlob blob = new FileBlob(f);

        UnzipToDocuments unzipToDocs = new UnzipToDocuments(testDocsFolder, blob);

        DocumentModel mainUnzippedFolderDoc = unzipToDocs.run();
        assertNotNull(mainUnzippedFolderDoc);

        // =====================================> Create a duplicate
        DocumentModel parent = coreSession.getDocument(new PathRef("/test-unzip/valid-zip/valid-zip/f2"));
        DocumentModel image = coreSession.getDocument(new PathRef("/test-unzip/valid-zip/valid-zip/f2/Picture.jpg"));
        // Create 3 extra copies
        for (int i = 0; i < 3; i++) {
            /* DocumentModel ignore= */ coreSession.copy(image.getRef(), parent.getRef(), image.getName());
        }

        coreSession.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // =====================================> Zip the folder
        ZipFolderish zipFolderish = new ZipFolderish(mainUnzippedFolderDoc);
        Blob zipped = zipFolderish.run();
        assertNotNull(zipped);

        // Check all normal entries are there
        List<String> entries = ZipUtils.getEntryNames(zipped.getFile());
        for (String path : PATHS_IN_ZIP1) {
            if (!entries.contains(path)) {
                assertTrue("Missing entry: " + path, false);
            }
        }

        // Check the extra have been renamed
        assertTrue(entries.contains("valid-zip/valid-zip/f2/Picture-2.jpg"));
        assertTrue(entries.contains("valid-zip/valid-zip/f2/Picture-3.jpg"));
        assertTrue(entries.contains("valid-zip/valid-zip/f2/Picture-4.jpg"));

    }
}
