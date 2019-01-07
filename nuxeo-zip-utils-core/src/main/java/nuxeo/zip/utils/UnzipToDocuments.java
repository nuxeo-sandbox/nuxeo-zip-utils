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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

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

    private int commitModulo= DEFAULT_COMMIT_MODULO;

    private String rootFolderishName = null;

    private String rootFolderishType = DEFAULT_FOLDERISH_TYPE;

    private Boolean mapRoot = false;

    public UnzipToDocuments(DocumentModel parentDoc, Blob zipBlob) {
        this.parentDoc = parentDoc;
        this.zipBlob = zipBlob;
    }

    // TODO: The assumptions below about folders are wrong. The code can be made more effecient/simpler once you understand that paths to folders are NOT required in a zip file. In fact they usually don't exist unless the folder is empty.

    /**
     * Creates Documents, in a hierarchical way, copying the tree-structure stored in the zip file.
     * <p>
     * If the zip file contains no path for a main extraction folder, one is created using either the file name of the
     * zip blob (minus thez extension), or the valie set using <code>setRootFolderishName</code>. The sames goes for the
     * type of container for this main container, it wikll be Folderish or the value set using
     * <code>setRootFolderishType</code>
     *
     * @return the main document (childFolderishType) containing the unzipped data
     * @since 10.2
     */
    public DocumentModel run() throws NuxeoException {

        File mainParentFolderOnDisk = null;
        ZipFile zipFile = null;
        DocumentModel mainUnzippedFolderDoc = null;

        CoreSession session = parentDoc.getCoreSession();
        FileManager fileManager = Framework.getService(FileManager.class);

        try {
            String dcTitle;
            String path;
            int idx;
            DocumentModel folderishDoc;
            String parentDocPath = parentDoc.getPathAsString();
            byte[] buffer = new byte[4096];
            int len = 0;
            int count = 0;

            Path outDirPath = Framework.createTempDirectory("ZipUtils-Unzip");
            mainParentFolderOnDisk = new File(outDirPath.toString());
            boolean isMainUzippedFolderDoc = false;

            // We must order the zip by names (full path names), so we make sure we cill create
            // the Container before trying to create their content. For example,, as the entries
            // are ordered by hash, we may receive "folder1/file.txt" before receiving "folder1/"
            // Using a TreeMap to order by name
            File zipBlobFile = zipBlob.getFile();
            zipFile = new ZipFile(zipBlobFile);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            Map<String, ZipEntry> entriesByName = new TreeMap<String, ZipEntry>();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                entriesByName.put(entry.getName(), entry);
            }

            // If the first entry is not a directory, it means we have to create a Folderish where to unzip the
            // documents. We do this by detting a prefix. This while-loop will iterate only the first items, until it
            // reaches a "non-ignorable" entry. If it is a direc tory, all os good. Else, a prexif will have to be used.
            String directoryPrefix = "";
            count = 0;
            Iterator<ZipEntry> sortedEntries = entriesByName.values().iterator();
            while (sortedEntries.hasNext()) {

                ZipEntry entry = sortedEntries.next();

                String fileName = entry.getName();
                if (shouldIgnoreEntry(fileName)) {
                    continue;
                }

                count += 1;
                if (count == 1 && !entry.isDirectory()) {
                    // We need to create a folderish
                    if (StringUtils.isNotBlank(rootFolderishName)) {
                        directoryPrefix = rootFolderishName;
                    } else {
                        directoryPrefix = FilenameUtils.getBaseName(zipBlob.getFilename());
                    }
                    mainUnzippedFolderDoc = session.createDocumentModel(parentDocPath, directoryPrefix,
                        rootFolderishType);
                    mainUnzippedFolderDoc.setPropertyValue("dc:title", directoryPrefix);
                    mainUnzippedFolderDoc = session.createDocument(mainUnzippedFolderDoc);
                    mainUnzippedFolderDoc = session.saveDocument(mainUnzippedFolderDoc);
                    break;
                } else {
                    break;
                }
            }

            // Now, we can walk this tree
            sortedEntries = entriesByName.values().iterator();
            while (sortedEntries.hasNext()) {

                ZipEntry entry = sortedEntries.next();

                String fileName = entry.getName();
                String fileNamePrefixed = fileName;
                if (shouldIgnoreEntry(fileName)) {
                    continue;
                }

                if (!directoryPrefix.isEmpty()) {
                    fileNamePrefixed = directoryPrefix + "/" + fileName;
                }

                dcTitle = fileNamePrefixed.split("/")[fileNamePrefixed.split("/").length - 1];
                idx = fileNamePrefixed.lastIndexOf("/");
                path = idx == -1 ? "" : fileNamePrefixed.substring(0, idx);

                // Create the container
                if (entry.isDirectory()) {

                    if (path.indexOf("/") == -1) {
                        isMainUzippedFolderDoc = true;
                        path = "";
                    } else {
                        path = path.substring(0, path.lastIndexOf("/"));
                    }

                    File newFile = new File(outDirPath.toString() + File.separator + fileName);
                    newFile.mkdirs();

                    folderishDoc = session.createDocumentModel(parentDocPath + "/" + path, dcTitle, childFolderishType);
                    folderishDoc.setPropertyValue("dc:title", dcTitle);
                    folderishDoc = session.createDocument(folderishDoc);
                    folderishDoc = session.saveDocument(folderishDoc);

                    if (isMainUzippedFolderDoc && mainUnzippedFolderDoc == null) {
                        mainUnzippedFolderDoc = folderishDoc;
                        isMainUzippedFolderDoc = false;
                    }

                    continue;
                }

                // If not a directory, create the file on disk then import it
                // (and so, let Nuxeo and its configuration decide the type of doc. to create)
                File newFile = new File(outDirPath.toString() + File.separator + fileName);
                FileOutputStream fos = new FileOutputStream(newFile);
                InputStream zipEntryStream = zipFile.getInputStream(entry);
                while ((len = zipEntryStream.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();

                // Import
                FileBlob blob = new FileBlob(newFile);
                fileManager.createDocumentFromBlob(session, blob, parentDocPath + "/" + path, true, blob.getFilename());

                count += 1;
                if ((count % commitModulo) == 0) {
                    TransactionHelper.commitOrRollbackTransaction();
                    TransactionHelper.startTransaction();
                }

            } // while(sortedEntries.hasNext())

            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

        } catch (IOException e) {

            throw new NuxeoException("Error while unzipping and creating Documents", e);

        } finally {

            org.apache.commons.io.FileUtils.deleteQuietly(mainParentFolderOnDisk);

            try {
                zipFile.close();
            } catch (IOException e) {
                // Ignore;
            }
        }

        return mainUnzippedFolderDoc;
    }

    /*
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
