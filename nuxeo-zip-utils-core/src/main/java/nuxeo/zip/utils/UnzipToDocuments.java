/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Michael Gena
 *     Thibaud Arguillere
 */
package nuxeo.zip.utils;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.api.Framework;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @since 10.2
 */
public class UnzipToDocuments {

    protected static Log logger = LogFactory.getLog(UnzipToDocuments.class);

    public static String DEFAULT_FOLDERISH_TYPE = "Folder";

    public static int DEFAULT_COMMIT_MODULO = 100;

    private DocumentModel parentDoc;

    private Blob zipBlob;

    private String childFolderishType = DEFAULT_FOLDERISH_TYPE;

    private int commitModulo = DEFAULT_COMMIT_MODULO;

    private String rootFolderishName;

    private String rootFolderishType = DEFAULT_FOLDERISH_TYPE;

    private Boolean mapRoot = false;

    private DocumentModel rootDocument;

    public UnzipToDocuments(DocumentModel parentDoc, Blob zipBlob) {
        this.parentDoc = parentDoc;
        this.zipBlob = zipBlob;
        rootFolderishName = FilenameUtils.getBaseName(zipBlob.getFilename());
    }

    /**
     * Creates Documents, in a hierarchical way, copying the tree-structure stored in the zip file.
     *
     * <P>Sometimes a zip file is a zip of the folder. Often in these cases you want the root of the extracted files to
     * be the root document in Nuxeo.
     *
     * <P>In other cases it's likely you want the root docuemnt in Nuxeo to be separate, therefore the contents of the
     * zip file should be imported as *children*.
     *
     * <P>The truth is there is no way to detect the desired behavior so you need to specify it using the
     * <code>mapRoot</code> property.
     *
     * <P>You can specify the document type of the root object using <code>setRootFolderishType</code>.
     *
     * <P>You can specify the name of the root object using <code>setRootFolderishName</code>, otherwise the name of the
     * zip file or the name of the root folder in the zip will be used (depedning on the value of <code>mapRoot</code>.
     *
     * @return the main document containing the unzipped data
     * @since 10.2
     */
    public DocumentModel run() throws NuxeoException {


        File tempFolderFile = null;
        ZipFile zipFile = null;

        CoreSession session = parentDoc.getCoreSession();
        FileManager fileManager = Framework.getService(FileManager.class);

        try {

            Path pathToTempFolder = Framework.createTempDirectory(rootFolderishType + "-Unzip");
            tempFolderFile = new File(pathToTempFolder.toString());
            File zipBlobFile = zipBlob.getFile();
            zipFile = new ZipFile(zipBlobFile);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            DocumentModel parentForImport;

            if (!mapRoot) {
                rootDocument = session.createDocument(session.createDocumentModel(parentDoc.getPathAsString(), rootFolderishName, rootFolderishType));
                parentForImport = rootDocument;
            } else {
                parentForImport = parentDoc;
            }

            while (entries.hasMoreElements()) {

                DocumentModel parentForNewBlob;
                ZipEntry entry = entries.nextElement();
                String entryPath = entry.getName();

                if (shouldIgnoreEntry(entryPath)) {
                    continue;
                }

                Boolean isDirectory = entry.isDirectory();

                // Create folderish documents as needed and get the parent for the Blob; i.e. where the Blob will be
                // imported.
                parentForNewBlob = handleFolders(session, parentForImport, entryPath, isDirectory);

                if (parentForNewBlob == null) {
                    // This is a file at the root level, so the parent is the container.
                    parentForNewBlob = parentForImport;
                }

                // I only need to import the files, not the folders, folderish docs are created by handleFolders()
                if (!isDirectory) {
                    String systemPath = pathToTempFolder.toString() + File.separator + entryPath;
                    File newFile = new File(systemPath);
                    if (!newFile.getParentFile().exists()) {
                        newFile.getParentFile().mkdirs();
                    }
                    FileOutputStream fos = new FileOutputStream(newFile);
                    InputStream zipEntryStream = zipFile.getInputStream(entry);
                    int len;
                    byte[] buffer = new byte[4096];
                    while ((len = zipEntryStream.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();

                    if (parentForNewBlob != null) {
                        // Import
                        FileBlob blob = new FileBlob(newFile);
                        fileManager.createDocumentFromBlob(session, blob, parentForNewBlob.getPathAsString(), true, blob.getFilename());
                    }
                }

            }

        } catch (IOException e) {
            throw new NuxeoException("Error while unzipping and creating Documents", e);
        } finally {
            org.apache.commons.io.FileUtils.deleteQuietly(tempFolderFile);
            try {
                zipFile.close();
            } catch (IOException e) {
                // Ignore;
            }
        }

        return rootDocument;
    }

    /**
     * Given a path from the zip file, make sure there are folderish documents in Nuxeo for each folder.
     *
     * @param session
     * @param entryPath
     * @param isDirectory
     * @return
     */
    private DocumentModel handleFolders(CoreSession session, DocumentModel parentForImport, String entryPath, Boolean isDirectory) {
        DocumentModel parentFolderForNewEntry = null;

        String repoPathToCurrentDoc = parentForImport.getPathAsString();
        String repoPathToCurrentDocParent = parentForImport.getPathAsString();
        String[] pathParts = entryPath.split("/");

        int limit;
        if (isDirectory)
            limit = pathParts.length;
        else
            limit = pathParts.length - 1;

        for (int i = 0; i < limit; i++) {

            String docType;

            if (i == 0) {
                docType = rootFolderishType;
            } else {
                docType = childFolderishType;
            }

            repoPathToCurrentDoc += "/" + pathParts[i];

            // Test to see if the document already exists...
            PathRef repoPathRefToCurrentDoc = new PathRef(repoPathToCurrentDoc);
            if (!session.exists(repoPathRefToCurrentDoc)) {
                parentFolderForNewEntry = session.createDocument(session.createDocumentModel(repoPathToCurrentDocParent, pathParts[i], docType));
                parentFolderForNewEntry.setPropertyValue("dc:title", pathParts[i]);
                session.saveDocument(parentFolderForNewEntry);
            } else {
                parentFolderForNewEntry = session.getDocument(repoPathRefToCurrentDoc);
            }

            // The top-level folderish is the document we should return.
            if (i == 0 && rootDocument == null) {
                rootDocument = parentFolderForNewEntry;
            }

            repoPathToCurrentDocParent += "/" + pathParts[i];
        }

        return parentFolderForNewEntry;
    }

    /**
     * Check if the entry should be ignored. Either because not relevant (__MACOSX, ...) or dangerous ("../")
     */
    protected boolean shouldIgnoreEntry(String fileName) {
        if (fileName.startsWith("__MACOSX/") || fileName.startsWith(".") || fileName.contains("../")
            || fileName.endsWith(".DS_Store")) {
            return true;
        }

        return false;
    }

    public void setChildFolderishType(String childFolderishType) {
        this.childFolderishType = StringUtils.isBlank(childFolderishType) ? DEFAULT_FOLDERISH_TYPE : childFolderishType;
    }

    public void setCommitModulo(int commitModulo) {
        this.commitModulo = commitModulo <= 0 ? DEFAULT_COMMIT_MODULO : commitModulo;
    }

    public void setRootFolderishName(String name) {
        rootFolderishName = StringUtils.isBlank(name) ? null : name;
    }

    public void setRootFolderishType(String type) {
        rootFolderishType = StringUtils.isBlank(type) ? DEFAULT_FOLDERISH_TYPE : type;
    }

    public void setMapRoot(Boolean mapRoot) {
        this.mapRoot = mapRoot;
    }
}
