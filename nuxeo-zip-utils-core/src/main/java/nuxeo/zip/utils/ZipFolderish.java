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
 *     Thibaud ARguillere
 */
package nuxeo.zip.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.runtime.api.Framework;

/**
 * Zip a Folderish document and all its children/grand children/...
 * <p>
 * If a non-folderish document has no blob, it is simply ignored. <b> The <ode>run</code> methods look for a blob in
 * "file=content". It is possible to use an Automation Chain/Operation that will then be called instead of fetching
 * file:content, the chain receives the document as input and must return a blob (or null)
 * <p>
 * When the blob returned is null, nothing is onde. A null blob will be either returned by the callback, or because the
 * currenty processed document has no "file" schema or its "file:content field is null.
 * <p>
 * It is also possible to override the additional WHERE clause used to find children (a typical "not a verion, not a
 * proxy, not trashed, not hidden"). See <code>DEFAULT_NXQL_WHERE_FOR_GET_CHILDREN</code.
 * <p>
 * It is possible to ask to zip only the first level of the folder.
 * <p>
 * <b>WARNING</b>: In this first implementation (10.2), it is likely the code will not support big folders, with a lot
 * of family.
 * <p>
 * <b>WARNING</b>: It is stating the obvious to say that the zipping can take a lot of time if the bobs are big, and/or
 * the number of items to zip is big. When the operaiton will take time, and to avoid client timeouts, it is recommended
 * to run the code asynchrnously.
 *
 * @since 10.2
 */
public class ZipFolderish {

    public static final String DEFAULT_FOLDERISH = "Folder";

    // Added with an AND after SELECT * FROM Document WHERE ecm:parentId = 'id of parent'
    public static final String DEFAULT_NXQL_WHERE_FOR_GET_CHILDREN = "ecm:mixinType != 'HiddenInNavigation' AND ecm:isVersion = 0 AND ecm:isProxy = 0 AND ecm:isTrashed = 0";

    protected DocumentModel mainDocument;

    protected CoreSession coreSession;

    protected String getBlobCallbackChain = null;

    // Added with an AND after SELECT * FROM Document WHERE ecm:parentId = 'id of parent'
    protected String getChildrenWhereClause = DEFAULT_NXQL_WHERE_FOR_GET_CHILDREN;

    // This is what makes this class not able to process big folders. This map store the title of documents and their
    // paths in the zip. When adding a new element, we test if the path already existt and if yes, we must change the
    // title used in the zip.
    // Checlkin i the entry exist in the zip would have the same impact in memory, unless building somethign slow that
    // uses the zip stream to loop and check all entries.
    protected List<String> pathsInZip = new ArrayList<String>();

    public ZipFolderish(DocumentModel docToZip) {

        mainDocument = docToZip;
        coreSession = mainDocument.getCoreSession();
    }

    /**
     * Shortcut for <code>run(false)</code>
     *
     * @return the zipped content
     * @throws IOException
     * @since 10.2
     */
    public Blob run() throws IOException {
        return run(false);
    }

    /**
     * Zip the content of the folder with its hierarchy:
     * <li>
     * <ul>
     * Recursively seaches for sub folders and export the documents which have a non null blob
     * </ul>
     * <ul>
     * The blob added to the zip file is either the blob returned by the chain callback or the one if "file:content", if
     * any (and if the document to zip has the "file" shcema)
     * </ul>
     * <ul>
     * Null blobs are handled (ignored, not trying to zip a null)
     * </ul>
     * </li>
     * <p>
     * To load the children of a folderish, an NXQL query is performed. By default, it filters the documents which are
     * hidden, or a version, or a proxy or in the trash. This can be overriden by calling
     * <code>setGetBlolbCallbackChain</code> <i>before</i> <code>run</code>
     * <p>
     * When <code>doNotCreateMainFolder</code> is true, the first level in the zip archive is let empty, hiearchy starts
     * whith children directly.
     * <p>
     *
     * @param doNotCreateMainFolder
     * @return the zipped content
     * @throws IOException
     * @since 10.2
     */
    public Blob run(boolean doNotCreateMainFolder) throws IOException {

        if(!mainDocument.isFolder()) {
            return null;
        }

        Blob finalZip = Blobs.createBlobWithExtension(".zip");

        finalZip.setFilename(mainDocument.getTitle() + ".zip");
        finalZip.setMimeType("application/zip");

        File finalZipFile = finalZip.getFile();

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(finalZipFile))) {

            String currentPath;

            currentPath = "";
            if (!doNotCreateMainFolder) {
                currentPath = mainDocument.getTitle() + "/";
            }

            if (!currentPath.isEmpty()) {
                ZipUtils._putDirectoryEntry(currentPath, zipOut);
            }

            processFolderish(mainDocument, zipOut, currentPath);

        }

        return finalZip;

    }

    protected void processFolderish(DocumentModel folderishDoc, ZipOutputStream zipOut, String currentPath)
            throws IOException {

        String nxql;
        String docTitle;
        String uuid;
        Blob blob;
        String pathInZip;
        int countForAvoidDuplicates = 1;
        DocumentModel folderish;
        DocumentModel doc;

        if (!currentPath.endsWith("/")) {
            currentPath += "/";
        }

        // ==============================================
        // Process folderish children
        // ==============================================
        nxql = "SELECT ecm:uuid, dc:title FROM Document WHERE ecm:parentId = '" + folderishDoc.getId() + "'";
        nxql += " AND ecm:mixinType = 'Folderish'";
        nxql += " AND " + getChildrenWhereClause;

        try (IterableQueryResult result = coreSession.queryAndFetch(nxql, NXQL.NXQL)) {
            for (Map<String, Serializable> map : result) {
                docTitle = (String) map.get("dc:title");
                pathInZip = currentPath + docTitle + "/";
                if (pathsInZip.contains(pathInZip)) {
                    countForAvoidDuplicates += 1;
                    docTitle += "-" + countForAvoidDuplicates;
                    pathInZip = currentPath + docTitle + "/";
                }
                pathsInZip.add(pathInZip);

                // _putDirectoryEntry adds the terminating "/" in the zip
                pathInZip = StringUtils.removeEnd(pathInZip, "/");
                ZipUtils._putDirectoryEntry(pathInZip, zipOut);
                pathInZip += "/";

                // Recursive call
                // TODO: Do not fetch the folder, use the UID as a parameter in the function, like:
                // processFolderish((String) map.get("ecm:uuid"), , zipOut, pathInZip);
                uuid = (String) map.get("ecm:uuid");
                folderish = coreSession.getDocument(new IdRef(uuid));
                processFolderish(folderish, zipOut, pathInZip);
            }
        }

        // ==============================================
        // Process non-folderish children
        // ==============================================
        // OPtimizaiton dea: do not get UID when there is no callback chain
        // Looks like we can't get file:clontent using queryAndFetch...
        // So, let's just get uid and fecth each doc then.
        // TODO: certaing room for optimization here...
        // nxql = "SELECT ecm:uuid, file:content FROM Document WHERE ecm:parentId = '" + folderishDoc.getId() + "'";
        nxql = "SELECT ecm:uuid FROM Document WHERE ecm:parentId = '" + folderishDoc.getId() + "'";
        nxql += " AND ecm:mixinType != 'Folderish'";
        nxql += " AND " + getChildrenWhereClause;

        try (IterableQueryResult result = coreSession.queryAndFetch(nxql, NXQL.NXQL)) {
            for (Map<String, Serializable> map : result) {

                uuid = (String) map.get("ecm:uuid");
                doc = coreSession.getDocument(new IdRef(uuid));
                blob = getDocumentBlob(doc);
                // It is ok to have a null blob
                if (blob == null) {
                    continue;
                }

                docTitle = blob.getFilename();
                pathInZip = currentPath + docTitle;
                if (pathsInZip.contains(pathInZip)) {
                    countForAvoidDuplicates += 1;
                    String baseName = FilenameUtils.getBaseName(docTitle);
                    String ext = FilenameUtils.getExtension(docTitle);
                    docTitle = baseName + "-" + countForAvoidDuplicates + "." + ext;
                    pathInZip = currentPath + docTitle;
                }
                pathsInZip.add(pathInZip);

                ZipUtils._putFileEntry(blob.getFile(), pathInZip, zipOut);
            }
        }

    }

    protected Blob getDocumentBlob(DocumentModel doc) {

        Blob result = null;

        if (StringUtils.isBlank(getBlobCallbackChain)) {
            if (doc.hasSchema("file")) {
                result = (Blob) doc.getPropertyValue("file:content");
            }
        } else {
            result = getBlobFromCallbackChain(doc);
        }

        return result;

    }

    protected Blob getBlobFromCallbackChain(DocumentModel doc) throws NuxeoException {

        Blob result = null;

        AutomationService as = Framework.getService(AutomationService.class);

        OperationContext ctx = new OperationContext();
        ctx.setInput(doc);
        ctx.setCoreSession(coreSession);
        OperationChain chain = new OperationChain("ZipFolder_GetBlob_Callback");
        chain.add(getBlobCallbackChain);

        try {
            result = (Blob) as.run(ctx, chain);
        } catch (OperationException e) {
            throw new NuxeoException("Failed to run the getBlobCallbackChain " + getBlobCallbackChain, e);
        }

        return result;
    }

    /**
     * Chain/Operation to call when getting the blob of a document. Receives the document as input, must return a Blob
     * (or null). If the chainId is null or empty, no callbackc chain is used.
     *
     * @param chainId
     * @since 10.2
     */
    public void setGetBlolbCallbackChain(String chainId) {

        getBlobCallbackChain = StringUtils.isBlank(chainId) ? null : chainId;

    }

    /**
     * NXQL to add to the WHERE clause for filtering children.
     * <p>
     * If <code>nxql</code> is null or empty, the filter is reste tio its default
     * (<code>DEFAULT_NXQL_WHERE_FOR_GET_CHILDREN</code>)
     *
     * @param nxql
     * @since 10.2
     */
    public void setGetCchildrenWhereClause(String nxql) {

        getChildrenWhereClause = StringUtils.isBlank(nxql) ? DEFAULT_NXQL_WHERE_FOR_GET_CHILDREN : nxql;

    }

}
