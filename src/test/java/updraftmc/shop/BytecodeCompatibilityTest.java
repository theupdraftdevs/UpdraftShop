package updraftmc.shop;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the one thing that is silent when it goes wrong: a jar whose bytecode is newer
 * than the server it targets.
 *
 * <p>Paper 1.21.11 runs on Java 21 and rejects class files above major version 65. A
 * build that compiles at a higher language level produces a jar that passes every other
 * check and then fails to load on the server with {@code UnsupportedClassVersionError}.
 */
class BytecodeCompatibilityTest {

    /** Major version 65 is Java 21, which is what Paper 1.21.11 runs. */
    private static final int JAVA_21_MAJOR = 65;

    @Test
    @DisplayName("the built jar targets Java 21 bytecode so Paper 1.21.11 can load it")
    void jarTargetsJava21() throws Exception {
        File jar = newestJar();

        assertNotNull(jar, "no jar found in build/libs, run 'gradlew jar' first");

        int major = 0;

        try (JarFile file = new JarFile(jar)) {
            var entries = file.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                if (entry.getName().endsWith(".class")) {
                    major = Math.max(major, majorVersion(file, entry));
                }
            }
        }

        assertTrue(major > 0, "no class files found in " + jar.getName());
        assertEquals(JAVA_21_MAJOR, major,
                "jar bytecode is major " + major + ", but Paper 1.21.11 only loads up to "
                        + JAVA_21_MAJOR + " (Java 21)");
    }

    @Test
    @DisplayName("no class file is preview marked, which would need a flag at runtime")
    void noPreviewMarkedClasses() throws Exception {
        File jar = newestJar();

        assertNotNull(jar, "no jar found in build/libs");

        try (JarFile file = new JarFile(jar)) {
            var entries = file.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                if (!entry.getName().endsWith(".class")) {
                    continue;
                }

                int minor = minorVersion(file, entry);

                // A preview feature stamps minor version 65535, and the JVM then refuses
                // to load the class unless --enable-preview is passed.
                assertTrue(minor != 65535,
                        entry.getName() + " uses a preview feature and will not load on a "
                                + "normal server JVM");
            }
        }
    }

    private static int majorVersion(JarFile file, JarEntry entry) throws Exception {
        byte[] header = readHeader(file, entry);
        return ((header[6] & 0xFF) << 8) | (header[7] & 0xFF);
    }

    private static int minorVersion(JarFile file, JarEntry entry) throws Exception {
        byte[] header = readHeader(file, entry);
        return ((header[4] & 0xFF) << 8) | (header[5] & 0xFF);
    }

    private static byte[] readHeader(JarFile file, JarEntry entry) throws Exception {
        try (var in = file.getInputStream(entry)) {
            byte[] header = new byte[8];
            int read = 0;

            while (read < header.length) {
                int count = in.read(header, read, header.length - read);

                if (count < 0) {
                    throw new IllegalStateException("truncated class file " + entry.getName());
                }

                read += count;
            }

            return header;
        }
    }

    /**
     * @return the highest versioned jar in build/libs, so a stale jar from an earlier
     *         build is not the thing under test
     */
    private static File newestJar() {
        File[] jars = new File("build/libs").listFiles(
                (dir, name) -> name.endsWith(".jar") && !name.contains("sources"));

        if (jars == null || jars.length == 0) {
            return null;
        }

        File newest = jars[0];

        for (File jar : jars) {
            if (jar.lastModified() > newest.lastModified()) {
                newest = jar;
            }
        }

        return newest;
    }
}
