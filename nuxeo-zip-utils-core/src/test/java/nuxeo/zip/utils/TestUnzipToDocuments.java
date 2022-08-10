package nuxeo.zip.utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
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
@Deploy({"org.nuxeo.ecm.platform.video",
    "org.nuxeo.ecm.platform.picture.core",
    "org.nuxeo.ecm.platform.tag",
    "org.nuxeo.ecm.platform.filemanager",
    "org.nuxeo.ecm.platform.types"})
@Deploy("nuxeo.zip.utils.nuxeo-zip-utils-core")
@Deploy("nuxeo.zip.utils.nuxeo-zip-utils-core:disable-listeners-contrib.xml")
public class TestUnzipToDocuments {

    // This zip file has files and folders at the root.
    private static final String FILES_AND_FOLDERS_ZIP = "files-and-folders.zip";
    private static final HashMap<String, String> PATHS_AND_DOCTYPES_FILESANDFOLDERS = new HashMap<>();

    static {
        PATHS_AND_DOCTYPES_FILESANDFOLDERS.put("/files-and-folders", "Folder");
        PATHS_AND_DOCTYPES_FILESANDFOLDERS.put("/files-and-folders/File.pdf", "File");
        PATHS_AND_DOCTYPES_FILESANDFOLDERS.put("/files-and-folders/f1", "Folder");
        PATHS_AND_DOCTYPES_FILESANDFOLDERS.put("/files-and-folders/f1/f1-f1", "Folder");
        PATHS_AND_DOCTYPES_FILESANDFOLDERS.put("/files-and-folders/f1/f1-f1/Video.mp4", "Video");
        PATHS_AND_DOCTYPES_FILESANDFOLDERS.put("/files-and-folders/f2", "Folder");
        PATHS_AND_DOCTYPES_FILESANDFOLDERS.put("/files-and-folders/f2/Picture.jpg", "Picture");
    }

    // This zip file has no folders.
    private static final String FILES_ONLY_ZIP = "files-only.zip";
    private static final HashMap<String, String> PATHS_AND_DOCTYPES_FILESONLY = new HashMap<>();

    static {
        PATHS_AND_DOCTYPES_FILESONLY.put("/files-only", "Folder");
        PATHS_AND_DOCTYPES_FILESONLY.put("/files-only/File.pdf", "File");
    }

    // This zip file has only folders.
    private static final String FOLDERS_ONLY_ZIP = "folders-only.zip";
    private static final HashMap<String, String> PATHS_AND_DOCTYPES_FOLDERSONLY = new HashMap<>();

    static {
        PATHS_AND_DOCTYPES_FOLDERSONLY.put("/folders-only", "Folder");
        PATHS_AND_DOCTYPES_FOLDERSONLY.put("/folders-only/f1", "Folder");
        PATHS_AND_DOCTYPES_FOLDERSONLY.put("/folders-only/f1/f1-f1", "Folder");
        PATHS_AND_DOCTYPES_FOLDERSONLY.put("/folders-only/f2", "Folder");
    }

    // This zip file has only a single folder at the root.
    private static final String SINGLE_FOLDER_ZIP = "single-folder.zip";
    private static final HashMap<String, String> PATHS_AND_DOCTYPES_SINGLEFOLDER = new HashMap<>();

    static {
        PATHS_AND_DOCTYPES_SINGLEFOLDER.put("/single-folder", "Folder");
        PATHS_AND_DOCTYPES_SINGLEFOLDER.put("/single-folder/nuxeo-unzip-test", "Folder");
        PATHS_AND_DOCTYPES_SINGLEFOLDER.put("/single-folder/nuxeo-unzip-test/File.pdf", "File");
        PATHS_AND_DOCTYPES_SINGLEFOLDER.put("/single-folder/nuxeo-unzip-test/f1", "Folder");
        PATHS_AND_DOCTYPES_SINGLEFOLDER.put("/single-folder/nuxeo-unzip-test/f1/f1-f1", "Folder");
        PATHS_AND_DOCTYPES_SINGLEFOLDER.put("/single-folder/nuxeo-unzip-test/f1/f1-f1/Video.mp4", "Video");
        PATHS_AND_DOCTYPES_SINGLEFOLDER.put("/single-folder/nuxeo-unzip-test/f2", "Folder");
        PATHS_AND_DOCTYPES_SINGLEFOLDER.put("/single-folder/nuxeo-unzip-test/f2/Picture.jpg", "Picture");
    }

    private static final HashMap<String, String> PATHS_AND_DOCTYPES_SINGLEFOLDER_MAPPED = new HashMap<>();

    static {
        PATHS_AND_DOCTYPES_SINGLEFOLDER_MAPPED.put("/nuxeo-unzip-test", "Folder");
        PATHS_AND_DOCTYPES_SINGLEFOLDER_MAPPED.put("/nuxeo-unzip-test/File.pdf", "File");
        PATHS_AND_DOCTYPES_SINGLEFOLDER_MAPPED.put("/nuxeo-unzip-test/f1", "Folder");
        PATHS_AND_DOCTYPES_SINGLEFOLDER_MAPPED.put("/nuxeo-unzip-test/f1/f1-f1", "Folder");
        PATHS_AND_DOCTYPES_SINGLEFOLDER_MAPPED.put("/nuxeo-unzip-test/f1/f1-f1/Video.mp4", "Video");
        PATHS_AND_DOCTYPES_SINGLEFOLDER_MAPPED.put("/nuxeo-unzip-test/f2", "Folder");
        PATHS_AND_DOCTYPES_SINGLEFOLDER_MAPPED.put("/nuxeo-unzip-test/f2/Picture.jpg", "Picture");
    }

    private static final String WITHOUT_ROOT_ENTRY_ZIP = "without-root-folder-entry.zip";
    private static final HashMap<String, String> PATHS_AND_DOCTYPES_WITHOUTROOTENTRY = new HashMap<>();

    static {
        PATHS_AND_DOCTYPES_WITHOUTROOTENTRY.put("/without-root-folder-entry/folder1", "Folder");
        PATHS_AND_DOCTYPES_WITHOUTROOTENTRY.put("/without-root-folder-entry/folder1/File.pdf", "File");
        PATHS_AND_DOCTYPES_WITHOUTROOTENTRY.put("/without-root-folder-entry/folder1/folder2", "Folder");
        PATHS_AND_DOCTYPES_WITHOUTROOTENTRY.put("/without-root-folder-entry/folder1/folder2/Picture.jpg", "Picture");
    }

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected TransactionalFeature txFeature;

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
        txFeature.nextTransaction();
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
    /**
     * This test validates that the unzip and import works for a zip with files and folders.
     */
    public void shouldUnzipFilesAndFolders() {

        File f = FileUtils.getResourceFileFromContext(FILES_AND_FOLDERS_ZIP);

        FileBlob blob = new FileBlob(f);

        UnzipToDocuments unzipToDocs = new UnzipToDocuments(testDocsFolder, blob);

        DocumentModel mainUnzippedFolderDoc = unzipToDocs.run();
        assertNotNull(mainUnzippedFolderDoc);

        checkUnzippedContent(PATHS_AND_DOCTYPES_FILESANDFOLDERS);

    }

    @Test
    /**
     * This test validates that the unzip and import works for a zip with files only.
     */
    public void shouldUnzipFilesOnly() {

        File f = FileUtils.getResourceFileFromContext(FILES_ONLY_ZIP);

        FileBlob blob = new FileBlob(f);

        UnzipToDocuments unzipToDocs = new UnzipToDocuments(testDocsFolder, blob);

        DocumentModel mainUnzippedFolderDoc = unzipToDocs.run();
        assertNotNull(mainUnzippedFolderDoc);

        checkUnzippedContent(PATHS_AND_DOCTYPES_FILESONLY);

    }

    @Test
    /**
     * This test validates that the unzip and import works for a zip with folders only.
     */
    public void shouldUnzipFoldersOnly() {

        File f = FileUtils.getResourceFileFromContext(FOLDERS_ONLY_ZIP);

        // TODO: Do I really want to check and make sure no other children are created? Not sure I really care...

        FileBlob blob = new FileBlob(f);

        UnzipToDocuments unzipToDocs = new UnzipToDocuments(testDocsFolder, blob);

        DocumentModel mainUnzippedFolderDoc = unzipToDocs.run();
        assertNotNull(mainUnzippedFolderDoc);

        checkUnzippedContent(PATHS_AND_DOCTYPES_FOLDERSONLY);

    }

    @Test
    /**
     * This test validates that the developer can specify the root folderish type.
     */
    public void shouldUnzipToWorkpace() {

        File f = FileUtils.getResourceFileFromContext(FILES_AND_FOLDERS_ZIP);

        FileBlob blob = new FileBlob(f);

        UnzipToDocuments unzipToDocs = new UnzipToDocuments(testDocsFolder, blob);
        unzipToDocs.setRootFolderishType("Workspace");

        DocumentModel mainUnzippedFolderDoc = unzipToDocs.run();
        assertNotNull(mainUnzippedFolderDoc);

        assertEquals(mainUnzippedFolderDoc.getType(), "Workspace");

    }

    @Test
    /**
     * This test validates that the developer can specify the root folderish name.
     */
    public void shouldUnzipToTEST() {

        File f = FileUtils.getResourceFileFromContext(FILES_AND_FOLDERS_ZIP);

        FileBlob blob = new FileBlob(f);

        UnzipToDocuments unzipToDocs = new UnzipToDocuments(testDocsFolder, blob);
        unzipToDocs.setRootFolderishName("TEST");

        DocumentModel mainUnzippedFolderDoc = unzipToDocs.run();
        assertNotNull(mainUnzippedFolderDoc);

        assertEquals(mainUnzippedFolderDoc.getName(), "TEST");

    }

    @Test
    /**
     * This test validates that, given a zip of a folder, the folder is mapped as a child of the root document.
     */
    public void shouldMapRootFolderToChildDoc() {

        File f = FileUtils.getResourceFileFromContext(SINGLE_FOLDER_ZIP);

        FileBlob blob = new FileBlob(f);

        UnzipToDocuments unzipToDocs = new UnzipToDocuments(testDocsFolder, blob);

        unzipToDocs.setMapRoot(false);

        DocumentModel mainUnzippedFolderDoc = unzipToDocs.run();
        assertNotNull(mainUnzippedFolderDoc);

        checkUnzippedContent(PATHS_AND_DOCTYPES_SINGLEFOLDER);

    }

    @Test
    /**
     * This test validates that, given a zip of a single folder, the folder is mapped to the root document instead of
     * created *as a child* of the root document.
     */
    public void shouldMapRootFolderToRootDoc() {

        File f = FileUtils.getResourceFileFromContext(SINGLE_FOLDER_ZIP);

        FileBlob blob = new FileBlob(f);

        UnzipToDocuments unzipToDocs = new UnzipToDocuments(testDocsFolder, blob);

        unzipToDocs.setMapRoot(true);

        DocumentModel mainUnzippedFolderDoc = unzipToDocs.run();
        assertNotNull(mainUnzippedFolderDoc);

        checkUnzippedContent(PATHS_AND_DOCTYPES_SINGLEFOLDER_MAPPED);

    }


    @Test
    /**
     * This test validates that, given a zip that is NOT of a single folder, you can't tell the plug-in to map the root
     * content to the root Document.
     */
    // TODO - not actually implemented
    public void shouldFailToMapRootFolderToRootDoc() {

        File f = FileUtils.getResourceFileFromContext(FILES_AND_FOLDERS_ZIP);

        FileBlob blob = new FileBlob(f);

        UnzipToDocuments unzipToDocs = new UnzipToDocuments(testDocsFolder, blob);

        unzipToDocs.setMapRoot(true);

        DocumentModel mainUnzippedFolderDoc = unzipToDocs.run();
        assertNotNull(mainUnzippedFolderDoc);

    }

    @Test
    public void shouldUnzipWithoutRootEntry() {

        File f = FileUtils.getResourceFileFromContext(WITHOUT_ROOT_ENTRY_ZIP);

        FileBlob blob = new FileBlob(f);

        UnzipToDocuments unzipToDocs = new UnzipToDocuments(testDocsFolder, blob);

        DocumentModel mainUnzippedFolderDoc = unzipToDocs.run();
        assertNotNull(mainUnzippedFolderDoc);

        checkUnzippedContent(PATHS_AND_DOCTYPES_WITHOUTROOTENTRY);

    }

}
