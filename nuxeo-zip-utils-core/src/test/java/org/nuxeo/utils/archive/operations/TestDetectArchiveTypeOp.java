package org.nuxeo.utils.archive.operations;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URL;

import javax.inject.Inject;

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
import org.nuxeo.ecm.core.api.impl.blob.URLBlob;
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

//We don't have these test files in the resources, not even in the git history of all commits...
@Ignore
public class TestDetectArchiveTypeOp {

    public static final String VALID_ZIP = "valid-zip.zip";

    public static final String NOT_VALID_ZIP = "not-valid-zip.zip";

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    private URLBlob stream(String file) {
        URL input = getClass().getResource(file);
        return new URLBlob(input, file, null);
    }

    private void detectType(Blob input, String compress, String archive) throws OperationException {

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(input);
        Blob result = (Blob) automationService.run(ctx, DetectArchiveTypeOp.ID);

        assertEquals(input, result);
        assertEquals(archive, ctx.get(DetectArchiveTypeOp.ARCHIVE_TYPE));
        assertEquals(compress, ctx.get(DetectArchiveTypeOp.COMPRESS_TYPE));

    }

    @Test
    public void testZip() throws OperationException {
        File f = FileUtils.getResourceFileFromContext(VALID_ZIP);
        FileBlob blob = new FileBlob(f);
        detectType(blob, null, ArchiveStreamFactory.ZIP);
    }

    @Test
    public void test7z() throws OperationException {
        detectType(stream("/bla.7z"), null, ArchiveStreamFactory.SEVEN_Z);
    }

    @Test
    public void testAr() throws OperationException {
        detectType(stream("/bla.ar"), null, ArchiveStreamFactory.AR);
    }

    @Test
    public void testArj() throws OperationException {
        detectType(stream("/bla.arj"), null, ArchiveStreamFactory.ARJ);
    }

    @Test
    public void testCpio() throws OperationException {
        detectType(stream("/bla.cpio"), null, ArchiveStreamFactory.CPIO);
    }

    @Test
    public void testDump() throws OperationException {
        detectType(stream("/bla.dump"), null, ArchiveStreamFactory.DUMP);
    }

    @Test
    public void testJar() throws OperationException {
        detectType(stream("/bla.jar"), null, ArchiveStreamFactory.ZIP);
    }

    @Test
    public void testPack() throws OperationException {
        detectType(stream("/bla.pack"), CompressorStreamFactory.PACK200, ArchiveStreamFactory.ZIP);
    }

    @Test
    public void testDumpLz4() throws OperationException {
        detectType(stream("/bla.dump.lz4"), CompressorStreamFactory.LZ4_FRAMED, ArchiveStreamFactory.DUMP);
    }

    @Test
    public void testTar() throws OperationException {
        detectType(stream("/bla.tar"), null, ArchiveStreamFactory.TAR);
    }

    @Test
    public void testTarBz2() throws OperationException {
        detectType(stream("/bla.tar.bz2"), CompressorStreamFactory.BZIP2, ArchiveStreamFactory.TAR);
    }

    @Test
    public void testTarLz4() throws OperationException {
        detectType(stream("/bla.tar.lz4"), CompressorStreamFactory.LZ4_FRAMED, ArchiveStreamFactory.TAR);
    }

    @Test
    public void testTarDeflatez() throws OperationException {
        detectType(stream("/bla.tar.deflatez"), CompressorStreamFactory.DEFLATE, ArchiveStreamFactory.TAR);
    }

    @Test
    public void testTarLzma() throws OperationException {
        detectType(stream("/bla.tar.lzma"), CompressorStreamFactory.LZMA, ArchiveStreamFactory.TAR);
    }

    @Test
    public void testTarSz() throws OperationException {
        detectType(stream("/bla.tar.sz"), CompressorStreamFactory.SNAPPY_FRAMED, ArchiveStreamFactory.TAR);
    }

    @Test
    public void testTarXz() throws OperationException {
        detectType(stream("/bla.tar.xz"), CompressorStreamFactory.XZ, ArchiveStreamFactory.TAR);
    }

    @Test
    public void testTarZ() throws OperationException {
        detectType(stream("/bla.tar.Z"), CompressorStreamFactory.Z, ArchiveStreamFactory.TAR);
    }

    @Test
    public void testTarZst() throws OperationException {
        detectType(stream("/bla.tar.zst"), CompressorStreamFactory.ZSTANDARD, ArchiveStreamFactory.TAR);
    }

    @Test
    public void testTgz() throws OperationException {
        detectType(stream("/bla.tgz"), CompressorStreamFactory.GZIP, ArchiveStreamFactory.TAR);
    }

    @Test
    public void testTextBz2() throws OperationException {
        detectType(stream("/bla.txt.bz2"), CompressorStreamFactory.BZIP2, null);
    }

    @Test
    public void testUnixArj() throws OperationException {
        detectType(stream("/bla.unix.arj"), null, ArchiveStreamFactory.ARJ);
    }

    @Test
    public void testXmlBz2() throws OperationException {
        detectType(stream("/bla.xml.bz2"), CompressorStreamFactory.BZIP2, null);
    }

    @Test
    public void testZDump() throws OperationException {
        detectType(stream("/bla.z.dump"), null, ArchiveStreamFactory.DUMP);
    }

}
