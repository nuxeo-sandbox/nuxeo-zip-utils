/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thibaud Arguillere
 */
package nuxeo.zip.utils.operations;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

import nuxeo.zip.utils.UnzipToDocuments;

/** Extracts an archive and creates Documents matching the zip's tree structure. */
@Operation(id = UnzipToDocumentsOp.ID, category = Constants.CAT_BLOB, label = "ZipUtils: Unzip to Documents", description = ""
        + "Extracts an archive and imports the files as Documents, creating the same structure."
        + " Note that in all cases the operation creates a root Document at the target."
        + " When input is a Blob, the target parameter is required. When input is a Document, the blob is read using the xpath,"
        + " from file:content by default, and the target is the parent of input."
        + " The operation does nothing if the input is null."
        + " The name and title of the root document is the name of the archive file or the name of the root folder in the archive,"
        + " by default. You can specify your own name with the mainFolderishName parameter."
        + " The operation creates Folder Documents by default for any folders in the archive. You can change the type of the root"
        + " Document that is created, as well as any children, using mainFolderishType and folderishType, respectively."
        + " The value of commitModulo is used to commit the transaction incrementally during the import. With the default value"
        + " fo 100, the transaction is committed for every 100 files imported."
        + " With regards to mapRoot: sometimes a zip file contains a single root folder and, thus, you want the root Document to"
        + " be this folder - use mapRoot = true in this case. Other times the root Document is just a container to contain all"
        + " the extracted content - use mapRoot = false in this case."
        + " Returns the created root Folderish Document.")
public class UnzipToDocumentsOp {

    public static final String ID = "ZipUtils.UnzipToDocuments";

    public static final String CONTEXT_VAR_NAME = "zipInfo_isZip";

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    @Param(name = "target", required = false)
    protected DocumentModel target;

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath = "file:content";

    @Param(name = "folderishType", required = false, values = { "Folder" })
    protected String folderishType = "Folder";

    @Param(name = "commitModulo", required = false, values = { "100" })
    protected Integer commitModulo = 100;

    @Param(name = "mainFolderishType", required = false, values = { "Folder" })
    protected String mainFolderishType = "Folder";

    @Param(name = "mainFolderishName", required = false)
    protected String mainFolderishName;

    @Param(name = "mapRoot", required = false, values = { "false" })
    protected Boolean mapRoot = false;

    @OperationMethod
    public DocumentModel run(DocumentModel input) {
        if (input == null) {
            return null;
        }
        if (target == null) {
            target = session.getDocument(input.getParentRef());
        }
        if (StringUtils.isBlank(xpath)) {
            xpath = "file:content";
        }
        var zipBlob = (Blob) input.getPropertyValue(xpath);
        return doUnzip(zipBlob);
    }

    @OperationMethod
    public DocumentModel run(Blob input) {
        if (input == null) {
            return null;
        }
        if (target == null) {
            throw new IllegalArgumentException("The target parameter cannot be empty with BLOB input.");
        }
        return doUnzip(input);
    }

    protected DocumentModel doUnzip(Blob zipBlob) {
        var unzipToDocs = new UnzipToDocuments(target, zipBlob);
        unzipToDocs.setChildFolderishType(folderishType);
        unzipToDocs.setCommitModulo(commitModulo);
        unzipToDocs.setRootFolderishType(mainFolderishType);
        unzipToDocs.setRootFolderishName(mainFolderishName);
        unzipToDocs.setMapRoot(mapRoot);
        return unzipToDocs.run();
    }
}
