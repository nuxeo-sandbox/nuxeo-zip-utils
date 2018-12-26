package org.nuxeo.utils.archive.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class ArchiveTypes {

    private static final String TYPES = "/archive/mime.properties";

    private static final Properties types = new Properties();

    static {
        try (InputStream in = ArchiveTypes.class.getResourceAsStream(TYPES)) {
            types.load(in);
        } catch (IOException e) {
            // Ignore
        }

    }

    public ArchiveTypes() {
        super();
    }
    
    public static Set<Object> mimeTypes() {
        return types.values().stream().distinct().collect(Collectors.toSet());
    }
    
    public static String mimeType(String type) {
        return types.getProperty(type);
    }

}
