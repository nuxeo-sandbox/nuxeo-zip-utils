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
 *     Thibaud Arguillere
 */
package nuxeo.zip.utils;

import java.io.File;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

/**
 * @since 9.10
 */
public class TestUtils {

    public static DocumentModel createDocumentFromFile(CoreSession inSession, String parentPath, String inType,
            String inResourceFilePath) {

        File f = FileUtils.getResourceFileFromContext(inResourceFilePath);

        DocumentModel doc = inSession.createDocumentModel(parentPath, f.getName(), inType);
        doc.setPropertyValue("dc:title", f.getName());
        doc.setPropertyValue("file:content", new FileBlob(f));
        return inSession.createDocument(doc);

    }

    public static DocumentModel createDocumentFromFile(CoreSession inSession, DocumentModel inParent, String inType,
            String inResourceFilePath) {

        return createDocumentFromFile(inSession, inParent.getPathAsString(), inType, inResourceFilePath);

    }

    /*
     * Using thumb:thumbnail here to avoid creating a dedicated schema. <p> WRNING: Thetest ust deploy the correct
     * bindles.
     */
    public static DocumentModel setThumbnailFieldFromFile(DocumentModel inDoc, String inResourceFilePath) {

        File f = FileUtils.getResourceFileFromContext(inResourceFilePath);
        FileBlob blob = new FileBlob(f);

        inDoc.addFacet("Thumbnail");
        inDoc.setPropertyValue("thumb:thumbnail", blob);

        return inDoc;
    }

    public static DocumentModel createFolder(CoreSession inSession, DocumentModel inParent, String inTitle) {

        DocumentModel doc = inSession.createDocumentModel(inParent.getPathAsString(), inTitle, "Folder");
        doc.setPropertyValue("dc:title", inTitle);
        return inSession.createDocument(doc);

    }
}
