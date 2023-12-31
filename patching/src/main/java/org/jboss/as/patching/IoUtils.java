/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.patching;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipFile;

import org.jboss.as.patching.logging.PatchLogger;
import org.wildfly.common.Assert;

/**
 * @author Emanuel Muckenhuber
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 * @author <a href="http://jmesnil/net/">Jeff Mesnil</a> (c) 2012 Red Hat Inc
 */
public class IoUtils {

    public static byte[] NO_CONTENT = new byte[0];

    private static final int DEFAULT_BUFFER_SIZE = 65536;

    /**
     * Copy input stream to output stream and close them both
     *
     * @param is input stream
     * @param os output stream
     *
     * @throws IOException for any error
     */
    public static void copyStreamAndClose(InputStream is, OutputStream os) throws IOException {
        try {
            copyStream(is, os, DEFAULT_BUFFER_SIZE);
            // throw an exception if the close fails since some data might be lost
            is.close();
            os.close();
        }
        finally {
            // ...but still guarantee that they're both closed
            safeClose(is);
            safeClose(os);
        }
    }

    /**
     * Copy input stream to output stream without closing streams. Flushes output stream when done.
     *
     * @param is input stream
     * @param os output stream
     *
     * @throws IOException for any error
     */
    public static void copyStream(InputStream is, OutputStream os) throws IOException {
        copyStream(is, os, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Copy input stream to output stream without closing streams. Flushes output stream when done.
     *
     * @param is input stream
     * @param os output stream
     * @param bufferSize the buffer size to use
     *
     * @throws IOException for any error
     */
    private static void copyStream(InputStream is, OutputStream os, int bufferSize)
            throws IOException {
        Assert.checkNotNullParam("is", is);
        Assert.checkNotNullParam("os", os);
        byte[] buff = new byte[bufferSize];
        int rc;
        while ((rc = is.read(buff)) != -1) os.write(buff, 0, rc);
        os.flush();
    }

    public static void copyFile(File sourceFile, File targetFile) throws IOException {
        if (sourceFile.isDirectory()) {
            copyDir(sourceFile, targetFile);
        } else {
            File parent = targetFile.getParentFile();
            if (!parent.exists()) {
                if (!parent.mkdirs()) {
                    throw PatchLogger.ROOT_LOGGER.cannotCreateDirectory(parent.getAbsolutePath());
                }
            }
            try {
                Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw PatchLogger.ROOT_LOGGER.cannotCopyFiles(sourceFile.getAbsolutePath(), targetFile.getAbsolutePath(),
                        e.getMessage(), e);
            }
        }
    }

    private static void copyDir(File sourceDir, File targetDir) throws IOException {
        if (targetDir.exists()) {
            if (!targetDir.isDirectory()) {
                throw PatchLogger.ROOT_LOGGER.notADirectory(targetDir.getAbsolutePath());
            }
        } else if (!targetDir.mkdirs()) {
            throw PatchLogger.ROOT_LOGGER.cannotCreateDirectory(targetDir.getAbsolutePath());
        }

        File[] children = sourceDir.listFiles();
        if (children != null) {
            for (File child : children) {
                copyFile(child, new File(targetDir, child.getName()));
            }
        }
    }

    public static byte[] copy(final InputStream is, final File target) throws IOException {
        if(! target.getParentFile().exists()) {
            target.getParentFile().mkdirs(); // Hmm
        }
        try (final OutputStream os = new FileOutputStream(target)){
            return HashUtils.copyAndGetHash(is, os);
        } catch (IOException e) {
            throw PatchLogger.ROOT_LOGGER.cannotCopyFiles(is.toString(), target.getAbsolutePath(),
                    e.getMessage(), e);
        }
    }

    public static byte[] copy(File source, File target) throws IOException {
        try (final FileInputStream is = new FileInputStream(source)){
            return copy(is, target);
        }
    }

    public static void safeClose(final Closeable closeable) {
        if(closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                //
            }
        }
    }

    public static void safeClose(final ZipFile closeable) {
        if(closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                //
            }
        }
    }

    public static boolean recursiveDelete(File root) {
        if (root == null) {
            return true;
        }
        boolean ok = true;
        if (root.isDirectory()) {
            final File[] files = root.listFiles();
            for (File file : files) {
                ok &= recursiveDelete(file);
            }
            return ok && (root.delete() || !root.exists());
        } else {
            ok &= root.delete() || !root.exists();
        }
        return ok;
    }

    public static File mkdir(File parent, String... segments) throws IOException {
        File dir = parent;
        for (String segment : segments) {
            dir = new File(dir, segment);
        }
        dir.mkdirs();
        return dir;
    }

    /**
     * Return a new File object based on the baseDir and the segments.
     *
     * This method does not perform any operation on the file system.
     */
    public static File newFile(File baseDir, String... segments) {
        File f = baseDir;
        for (String segment : segments) {
            f = new File(f, segment);
        }
        return f;
    }

}
