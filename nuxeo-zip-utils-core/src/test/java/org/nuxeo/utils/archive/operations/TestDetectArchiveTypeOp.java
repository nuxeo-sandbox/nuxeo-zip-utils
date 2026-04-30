package org.nuxeo.utils.archive.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;

import jakarta.inject.Inject;

import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.thumbnail",
        "org.nuxeo.ecm.platform.picture.core",
        "org.nuxeo.ecm.platform.tag",
        "org.nuxeo.ecm.platform.commandline.executor",
        "org.nuxeo.ecm.platform.rendition.core",
        "nuxeo.zip.utils.nuxeo-zip-utils-core" })
public class TestDetectArchiveTypeOp {

    protected static final String TEST_ARCHIVES = "TestArchives/";

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    private FileBlob loadTestFile(String fileName) {
        File f = FileUtils.getResourceFileFromContext(TEST_ARCHIVES + fileName);
        return new FileBlob(f);
    }

    private void detectType(Blob input, String expectedCompress, String expectedArchive) throws OperationException {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(input);
        Blob result = (Blob) automationService.run(ctx, DetectArchiveTypeOp.ID);

        assertEquals(input, result);
        if (expectedArchive == null) {
            assertNull(ctx.get(DetectArchiveTypeOp.ARCHIVE_TYPE));
        } else {
            assertEquals(expectedArchive, ctx.get(DetectArchiveTypeOp.ARCHIVE_TYPE));
        }
        if (expectedCompress == null) {
            assertNull(ctx.get(DetectArchiveTypeOp.COMPRESS_TYPE));
        } else {
            assertEquals(expectedCompress, ctx.get(DetectArchiveTypeOp.COMPRESS_TYPE));
        }
    }

    @Test
    public void testZip() throws OperationException {
        detectType(loadTestFile("bla.zip"), null, ArchiveStreamFactory.ZIP);
    }

    @Test
    public void testTar() throws OperationException {
        detectType(loadTestFile("bla.tar"), null, ArchiveStreamFactory.TAR);
    }

    @Test
    public void testTgz() throws OperationException {
        detectType(loadTestFile("bla.tgz"), CompressorStreamFactory.GZIP, ArchiveStreamFactory.TAR);
    }

    @Test
    public void testTarBz2() throws OperationException {
        detectType(loadTestFile("bla.tar.bz2"), CompressorStreamFactory.BZIP2, ArchiveStreamFactory.TAR);
    }

    @Test
    public void testTarXz() throws OperationException {
        detectType(loadTestFile("bla.tar.xz"), CompressorStreamFactory.XZ, ArchiveStreamFactory.TAR);
    }

    // --- Exotic formats: ignored until test archive files are provided in TestArchives/ ---

    @Ignore("Missing test file: bla.7z")
    @Test
    public void test7z() throws OperationException {
        detectType(loadTestFile("bla.7z"), null, ArchiveStreamFactory.SEVEN_Z);
    }

    @Ignore("Missing test file: bla.ar")
    @Test
    public void testAr() throws OperationException {
        detectType(loadTestFile("bla.ar"), null, ArchiveStreamFactory.AR);
    }

    @Ignore("Missing test file: bla.arj")
    @Test
    public void testArj() throws OperationException {
        detectType(loadTestFile("bla.arj"), null, ArchiveStreamFactory.ARJ);
    }

    @Ignore("Missing test file: bla.cpio")
    @Test
    public void testCpio() throws OperationException {
        detectType(loadTestFile("bla.cpio"), null, ArchiveStreamFactory.CPIO);
    }

    @Ignore("Missing test file: bla.dump")
    @Test
    public void testDump() throws OperationException {
        detectType(loadTestFile("bla.dump"), null, ArchiveStreamFactory.DUMP);
    }

    @Ignore("Missing test file: bla.jar")
    @Test
    public void testJar() throws OperationException {
        detectType(loadTestFile("bla.jar"), null, ArchiveStreamFactory.ZIP);
    }

    @Ignore("Missing test file: bla.pack")
    @Test
    public void testPack() throws OperationException {
        detectType(loadTestFile("bla.pack"), CompressorStreamFactory.PACK200, ArchiveStreamFactory.ZIP);
    }

    @Ignore("Missing test file: bla.dump.lz4")
    @Test
    public void testDumpLz4() throws OperationException {
        detectType(loadTestFile("bla.dump.lz4"), CompressorStreamFactory.LZ4_FRAMED, ArchiveStreamFactory.DUMP);
    }

    @Ignore("Missing test file: bla.tar.deflatez")
    @Test
    public void testTarDeflatez() throws OperationException {
        detectType(loadTestFile("bla.tar.deflatez"), CompressorStreamFactory.DEFLATE, ArchiveStreamFactory.TAR);
    }

    @Ignore("Missing test file: bla.tar.lzma")
    @Test
    public void testTarLzma() throws OperationException {
        detectType(loadTestFile("bla.tar.lzma"), CompressorStreamFactory.LZMA, ArchiveStreamFactory.TAR);
    }

    @Ignore("Missing test file: bla.tar.sz")
    @Test
    public void testTarSz() throws OperationException {
        detectType(loadTestFile("bla.tar.sz"), CompressorStreamFactory.SNAPPY_FRAMED, ArchiveStreamFactory.TAR);
    }

    @Ignore("Missing test file: bla.tar.Z")
    @Test
    public void testTarZ() throws OperationException {
        detectType(loadTestFile("bla.tar.Z"), CompressorStreamFactory.Z, ArchiveStreamFactory.TAR);
    }

    @Ignore("Missing test file: bla.tar.zst")
    @Test
    public void testTarZst() throws OperationException {
        detectType(loadTestFile("bla.tar.zst"), CompressorStreamFactory.ZSTANDARD, ArchiveStreamFactory.TAR);
    }

    @Ignore("Missing test file: bla.tar.lz4")
    @Test
    public void testTarLz4() throws OperationException {
        detectType(loadTestFile("bla.tar.lz4"), CompressorStreamFactory.LZ4_FRAMED, ArchiveStreamFactory.TAR);
    }

    @Ignore("Missing test file: bla.txt.bz2")
    @Test
    public void testTextBz2() throws OperationException {
        detectType(loadTestFile("bla.txt.bz2"), CompressorStreamFactory.BZIP2, null);
    }

    @Ignore("Missing test file: bla.unix.arj")
    @Test
    public void testUnixArj() throws OperationException {
        detectType(loadTestFile("bla.unix.arj"), null, ArchiveStreamFactory.ARJ);
    }

    @Ignore("Missing test file: bla.xml.bz2")
    @Test
    public void testXmlBz2() throws OperationException {
        detectType(loadTestFile("bla.xml.bz2"), CompressorStreamFactory.BZIP2, null);
    }

    @Ignore("Missing test file: bla.z.dump")
    @Test
    public void testZDump() throws OperationException {
        detectType(loadTestFile("bla.z.dump"), null, ArchiveStreamFactory.DUMP);
    }
}
