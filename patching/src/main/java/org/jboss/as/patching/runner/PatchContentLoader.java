/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.patching.runner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.metadata.BundleItem;
import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.ContentType;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.ModuleItem;

/**
 * Content loader for patch contents. When applying a patch the content is loaded from the patch itself; for rollbacks
 * the content will be loaded from the patch history.
 *
 * @author Emanuel Muckenhuber
 */
public abstract class PatchContentLoader {

    public static final String MODULES = Constants.MODULES;
    public static final String BUNDLES = Constants.BUNDLES;
    public static final String MISC = Constants.MISC;

    public static PatchContentLoader create(final File root) {
        final File miscRoot = new File(root, PatchContentLoader.MISC);
        final File bundlesRoot = new File(root, PatchContentLoader.BUNDLES);
        final File modulesRoot = new File(root, PatchContentLoader.MODULES);
        return PatchContentLoader.create(miscRoot, bundlesRoot, modulesRoot);
    }

    public static PatchContentLoader create(final File miscRoot, final File bundlesRoot, final File modulesRoot) {
        return new BasicContentLoader(miscRoot, bundlesRoot, modulesRoot);
    }

    /**
     * Open a new content stream.
     *
     * @param item the content item
     * @return the content stream
     */
    InputStream openContentStream(final ContentItem item) throws IOException {
        final File file = getFile(item);
        if (file == null) {
            throw new IllegalStateException();
        }
        return new FileInputStream(file);
    }

    /**
     * Get a patch content file.
     *
     * @param item the content item
     * @return the file
     */
    public abstract File getFile(final ContentItem item);

    public static File getMiscPath(final File miscRoot, final MiscContentItem item) {
        if (miscRoot == null) {
            throw new IllegalStateException();
        }
        File file = miscRoot;
        for (final String path : item.getPath()) {
            file = new File(file, path);
        }
        file = new File(file, item.getName());
        return file;
    }

    public static File getModulePath(File root, ModuleItem item) {
        return getModulePath(root, item.getName(), item.getSlot());
    }

    static File getModulePath(File root, String name, String slot) {
        if (root == null) {
            throw new IllegalStateException();
        }
        final String[] ss = name.split("\\.");
        File file = root;
        for (final String s : ss) {
            file = new File(file, s);
        }
        return new File(file, slot);
    }

    static class BasicContentLoader extends PatchContentLoader {
        private final File miscRoot;
        private final File bundlesRoot;
        private final File modulesRoot;

        BasicContentLoader(File miscRoot, File bundlesRoot, File modulesRoot) {
            this.miscRoot = miscRoot;
            this.bundlesRoot = bundlesRoot;
            this.modulesRoot = modulesRoot;
        }

        @Override
        public File getFile(ContentItem item) {
            final ContentType content = item.getContentType();
            switch (content) {
                case MODULE:
                    return getModulePath((ModuleItem) item);
                case MISC:
                    return getMiscPath((MiscContentItem) item);
                case BUNDLE:
                    return getBundlePath((BundleItem) item);
                default:
                    throw new IllegalStateException();
            }
        }

        File getMiscPath(final MiscContentItem item) {
            return getMiscPath(miscRoot, item);
        }

        File getModulePath(final ModuleItem item) {
            return getModulePath(modulesRoot, item);
        }

        File getBundlePath(final BundleItem item) {
            return getModulePath(bundlesRoot, item.getName(), item.getSlot());
        }
    }

}
