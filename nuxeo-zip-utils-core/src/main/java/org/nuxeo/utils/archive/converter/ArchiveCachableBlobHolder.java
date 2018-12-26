/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.utils.archive.converter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.examples.Expander;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.StringEscapeUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.MimetypeNotFoundException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.utils.archive.utils.ArchiveUtils;

/**
 * Cachable implementation of an archive file.
 *
 * @author dbrown
 */
public class ArchiveCachableBlobHolder extends SimpleCachableBlobHolder {

    private static final Log log = LogFactory.getLog(ArchiveCachableBlobHolder.class);

    protected Blob archiveBlob;

    protected MimetypeRegistry mimeTypeService;

    protected String key;

    public ArchiveCachableBlobHolder() {
    }

    public ArchiveCachableBlobHolder(Blob archiveBlob) {
        this.archiveBlob = archiveBlob;
    }

    public Blob getBlob(String path)
            throws IOException, MimetypeNotFoundException, MimetypeDetectionException, ConversionException {
        String filePath = key + path;
        File file = new File(filePath);
        Blob blob = Blobs.createBlob(file);
        String mimeType = getMimeTypeService().getMimetypeFromBlob(blob);
        blob.setMimeType(mimeType);
        blob.setFilename(path);
        return blob;
    }

    @Override
    public Blob getBlob() {
        return archiveBlob;
    }

    @Override
    public List<Blob> getBlobs() {
        if (blobs == null) {
            try {
                load(key);
            } catch (IOException e) {
                throw new NuxeoException(e);
            }
        }
        return blobs;
    }

    @Override
    public void load(String path) throws IOException {
        blobs = new ArrayList<>();
        File base = new File(path);
        try {
            if (base.isDirectory()) {
                addDirectoryToList(base, "");
            } else {
                File file = new File(path);
                String mimeType = getMimeType(file);
                Blob mainBlob = Blobs.createBlob(file, mimeType, null, file.getName());
                blobs.add(mainBlob);
            }

            orderIndexPageFirst(blobs);
        } catch (ConversionException e) {
            throw new RuntimeException("Blob loading from cache failed", e.getCause());
        }
    }

    @Override
    public String persist(String basePath) throws IOException {
        Path path = new Path(basePath);
        path = path.append(getHash());
        File dir = new File(path.toString());
        dir.mkdir();

        // Expand the archive
        Expander expand = new Expander();
        try (BufferedInputStream bin = ArchiveUtils.unwrap(archiveBlob)) {
            expand.expand(bin, dir);
        } catch (ArchiveException e) {
            throw new IOException(e);
        }
        key = dir.getAbsolutePath();

        // Check if creating an index.html file is needed
        load(path.toString());
        if (blobs != null && !blobs.get(0).getFilename().contains("index.html")) {
            log.info("Any index.html file found, generate a listing as index page.");
            File index = new File(dir, "index.html");
            if (index.createNewFile()) {
                Blob indexBlob = createIndexBlob();
                blobs.add(0, indexBlob);
                FileUtils.writeByteArrayToFile(index, indexBlob.getByteArray());
            } else {
                log.info("Unable to create index.html file");
            }
        }

        return key;
    }

    public String getMimeType(File file) throws ConversionException {
        try {
            return getMimeTypeService().getMimetypeFromFile(file);
        } catch (ConversionException e) {
            throw new ConversionException("Could not get MimeTypeRegistry", e);
        } catch (MimetypeNotFoundException | MimetypeDetectionException e) {
            return "application/octet-stream";
        }
    }

    public MimetypeRegistry getMimeTypeService() throws ConversionException {
        if (mimeTypeService == null) {
            mimeTypeService = Framework.getService(MimetypeRegistry.class);
        }
        return mimeTypeService;
    }

    protected Blob createIndexBlob() {
        StringBuilder page = new StringBuilder("<html><body>");
        page.append("<h1>")
            .append(StringEscapeUtils.escapeEcmaScript(StringEscapeUtils.escapeHtml4(archiveBlob.getFilename())))
            .append("</h1>");
        page.append("<ul>");
        for (Blob blob : blobs) {
            String fn = StringEscapeUtils.escapeEcmaScript(StringEscapeUtils.escapeHtml4(blob.getFilename()));
            page.append("<li><a href=\"").append(fn).append("\">");
            page.append(fn);
            page.append("</a></li>");
        }
        page.append("</ul></body></html>");
        return Blobs.createBlob(page.toString());
    }
}
