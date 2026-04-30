/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Ricardo Dias
 */

package org.nuxeo.utils.archive.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

public class TestArchive2HtmlConverter extends BaseConverterTest {

    protected static final String TEST_ARCHIVES = "TestArchives/";

    @Test
    public void testTarConverter() throws Exception {
        testConvertToMimeType("application/x-tar", "bla.tar");
    }

    @Test
    public void testTarGzConverter() throws Exception {
        testConvertToMimeType("application/gzip", "bla.tgz");
    }

    @Test
    public void testTarXzConverter() throws Exception {
        testConvertToMimeType("application/x-xz", "bla.tar.xz");
    }

    // --- Exotic formats: ignored until test archive files are provided in TestArchives/ ---

    @Ignore("Missing test file: bla.tar.lz4")
    @Test
    public void testTarLz4Converter() throws Exception {
        testConvertToMimeType("application/x-lz4", "bla.tar.lz4");
    }

    @Ignore("Missing test file: bla.jar")
    @Test
    public void testJarConverter() throws Exception {
        testConvertToMimeType("application/java-archive", "bla.jar");
    }

    @Ignore("Missing test file: bla.cpio")
    @Test
    public void testCpioConverter() throws Exception {
        testConvertToMimeType("application/x-cpio", "bla.cpio");
    }

    @Ignore("Missing test file: bla.arj")
    @Test
    public void testArjConverter() throws Exception {
        testConvertToMimeType("application/x-arj", "bla.arj");
    }

    @Ignore("Missing test file: bla.7z — also not streamable")
    @Test
    public void testSevenZip() throws Exception {
        testConvertToMimeType("application/x-7z-compressed", "bla.7z");
    }

    protected void testConvertToMimeType(String mimeType, String name) throws IOException {
        String converterName = cs.getConverterName(mimeType, "text/html");
        assertEquals("archive2html", converterName);

        checkConverterAvailability(converterName);

        BlobHolder htmlBH = getBlobFromPath(TEST_ARCHIVES + name, mimeType);

        Map<String, Serializable> parameters = Collections.emptyMap();

        BlobHolder result = cs.convert(converterName, htmlBH, parameters);
        assertNotNull(result);

        List<Blob> blobs = result.getBlobs();
        assertNotNull(blobs);
        assertTrue("Must contain at least 2 blobs", blobs.size() >= 2);

        Map<String, String> zipContent = new HashMap<>();
        try (InputStream in = getClass().getResourceAsStream("/test1.xml")) {
            zipContent.put("test1.xml", IOUtils.toString(in, "UTF-8"));
        }
        try (InputStream in = getClass().getResourceAsStream("/test2.xml")) {
            zipContent.put("test2.xml", IOUtils.toString(in, "UTF-8"));
        }

        String content = blobs.get(0).getString();
        assertTrue(content, content.contains("<li><a href=\"test1.xml\">test1.xml</a></li>"));
        assertTrue(content, content.contains("<li><a href=\"test2.xml\">test2.xml</a></li>"));

        for (int i = 1; i < blobs.size(); i++) {
            File file = blobs.get(i).getFile();
            String filename = file.getName();
            content = FileUtils.readFileToString(file, Charset.defaultCharset());
            if (zipContent.containsKey(file.getName())) {
                assertNotNull("No file entry for: " + filename, zipContent.get(file.getName()));
                zipContent.remove(file.getName());
            }
        }
        assertTrue("Unexpected remaining files: " + zipContent.keySet(), zipContent.isEmpty());
    }
}
