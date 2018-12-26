package org.nuxeo.utils.archive.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.nuxeo.ecm.core.api.Blob;

public class ArchiveUtils {

    @SuppressWarnings("resource")
    public static BufferedInputStream unwrap(Blob input) throws IOException, ArchiveException {
        InputStream blobStream = new BufferedInputStream(input.getStream());
        try {
            // Unwrap...
            blobStream = CompressorStreamFactory.getSingleton().createCompressorInputStream(blobStream);
            blobStream = new BufferedInputStream(blobStream);
        } catch (Exception ce) {
            // Ignore
        }
        return (BufferedInputStream) blobStream;
    }

    public static ArchiveInputStream open(Blob input) throws IOException, ArchiveException {
        BufferedInputStream blobStream = unwrap(input);
        return new ArchiveStreamFactory().createArchiveInputStream(blobStream);
    }

}
