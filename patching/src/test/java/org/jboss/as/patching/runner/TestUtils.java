/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.patching.runner;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.jboss.as.patching.Constants.APP_CLIENT;
import static org.jboss.as.patching.Constants.BASE;
import static org.jboss.as.patching.Constants.BUNDLES;
import static org.jboss.as.patching.Constants.DOMAIN;
import static org.jboss.as.patching.Constants.INSTALLATION_METADATA;
import static org.jboss.as.patching.Constants.LAYERS;
import static org.jboss.as.patching.Constants.METADATA;
import static org.jboss.as.patching.Constants.MODULES;
import static org.jboss.as.patching.Constants.PATCHES;
import static org.jboss.as.patching.Constants.STANDALONE;
import static org.jboss.as.patching.Constants.SYSTEM;
import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.patching.IoUtils.newFile;
import static org.jboss.as.patching.IoUtils.safeClose;
import static org.jboss.as.patching.logging.PatchLogger.ROOT_LOGGER;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes.Name;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.ZipUtils;
import org.jboss.as.patching.installation.InstalledImage;
import org.jboss.as.patching.installation.PatchableTarget;
import org.jboss.as.patching.metadata.BundledPatch;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBundleXml;
import org.jboss.as.patching.metadata.PatchXml;
import org.jboss.as.protocol.StreamUtils;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012, Red Hat Inc
 */
public class TestUtils {

    public static String randomString() {
        return randomUUID().toString();
    }

    public static void tree(File dir) {
        if (!ROOT_LOGGER.isTraceEnabled()){
            return;
        }
        StringBuilder out = new StringBuilder();
        out.append(dir.getParentFile().getAbsolutePath()).append("\n");
        tree0(out, dir, 1, "  ");
        ROOT_LOGGER.trace(out.toString());
    }

    private static void tree0(StringBuilder out, File dir, int indent, String tab) {
        StringBuilder shift = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            shift.append(tab);
        }
        out.append(shift + dir.getName() + "\n");
        for (File child : dir.listFiles()) {
            if (child.isDirectory()) {
                tree0(out, child, indent + 1, tab);
            } else {
                out.append(shift + tab + child.getName() + "\n");
            }
        }
    }

    public static File touch(File baseDir, String... segments) throws IOException {
        File f = baseDir;
        for (String segment : segments) {
            f = new File(f, segment);
        }
        f.getParentFile().mkdirs();
        f.createNewFile();
        return f;
    }

    public static void dump(File f, String content) throws IOException {
        try (final OutputStream os = new FileOutputStream(f)){
            os.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static File createModuleXmlFile(File mainDir, String moduleSpec, String... resources) throws IOException {
        final int c1 = moduleSpec.lastIndexOf(':');
        final String name;
        final String slot;
        if (c1 != -1) {
            name = moduleSpec.substring(0, c1);
            slot = moduleSpec.substring(c1 + 1);
        } else {
            name = moduleSpec;
            slot = "main";
        }

        StringBuilder content = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        content.append(format("<module xmlns=\"urn:jboss:module:1.2\" name=\"%s\" slot=\"%s\">\n", name, slot));
        content.append("  <resources>\n");
        content.append("    <resource-root path=\".\"/>\n");
        for (String resource : resources) {
            content.append(format("    <resource-root path=\"%s\"/>\n", resource));
        }
        content.append("  </resources>\n");
        content.append("</module>\n");
        ROOT_LOGGER.trace(content);
        File moduleXMLFile = touch(mainDir, "module.xml");
        dump(moduleXMLFile, content.toString());
        return moduleXMLFile;
    }

    public static File createModule0(final File baseDir, final String moduleName, final String... resourcesContents) throws IOException {
        final ContentTask task = new ContentTask() {
            @Override
            public String[] writeContent(File mainDir) throws IOException {
                String[] resourceFileNames = new String[resourcesContents.length];
                for (int i = 0; i < resourcesContents.length; i++) {
                    String content = resourcesContents[i];
                    File f = File.createTempFile("test", i + ".tmp", mainDir);
                    String fileName = f.getName();
                    resourceFileNames[i] = fileName;
                    dump(f, content);
                }
                return resourceFileNames;
            }
        };
        return createModule0(baseDir, moduleName, task);
    }

    public static File createModule0(final File baseDir, final String moduleName, final ContentTask task) throws IOException {
        final File main = createModuleRoot(baseDir, moduleName);
        final String[] resources = task.writeContent(main);
        createModuleXmlFile(main, moduleName, resources);
        return main.getParentFile();
    }

    public static File createModule1(File baseDir, String moduleName, String... resourceFileNames) throws IOException {
        File mainDir = createModuleRoot(baseDir, moduleName);
        createModuleXmlFile(mainDir, moduleName, resourceFileNames);
        return mainDir.getParentFile();
    }

    public static File createModuleRoot(File baseDir, String moduleSpec) throws IOException {
        final File dir = getModuleRoot(baseDir, moduleSpec);
        if (!dir.mkdirs() && !dir.exists()) {
            throw new IOException("failed to create " + dir.getAbsolutePath());
        }
        return dir;
    }

    public static File getModuleRoot(final File baseDir, final String moduleSpec) {
        final int c1 = moduleSpec.lastIndexOf(':');
        final String name;
        final String slot;
        if (c1 != -1) {
            name = moduleSpec.substring(0, c1);
            slot = moduleSpec.substring(c1 + 1);
        } else {
            name = moduleSpec;
            slot = "main";
        }
        // assert slot.equals(ModuleItem.MAIN_SLOT); // update to support other slots too
        final String[] segments = name.split("\\.");
        assert segments.length > 0;
        File dir = baseDir;
        for (String segment : segments) {
            dir = new File(dir, segment);
        }
        dir = new File(dir, slot);
        return dir;
    }

    public static void createPatchXMLFile(File dir, Patch patch) throws Exception {
        createPatchXMLFile(dir, patch, false);
    }

    public static void createPatchXMLFile(File dir, Patch patch, boolean logContent) throws Exception {
        Path patchXMLfile = dir.toPath().resolve("patch.xml");
        try (Writer fos = Files.newBufferedWriter(patchXMLfile, StandardCharsets.UTF_8)){
            PatchXml.marshal(fos, patch);
        }

        if(logContent) {
            final byte[] bytes = Files.readAllBytes(dir.toPath().resolve("patch.xml"));
            System.out.println(new String(bytes, StandardCharsets.UTF_8));

        }
    }

    public static void createPatchBundleXMLFile(File dir, final List<BundledPatch.BundledPatchEntry> patches) throws Exception {
        File bundleXMLFile = new File(dir, "patches.xml");
        FileOutputStream fos = new FileOutputStream(bundleXMLFile);
        try {
            PatchBundleXml.marshal(fos, new BundledPatch() {
                @Override
                public List<BundledPatchEntry> getPatches() {
                    return patches;
                }
            });
        } finally {
            safeClose(fos);
        }
    }

    public static File createZippedPatchFile(File sourceDir, String zipFileName) {
        //tree(sourceDir);
        File zipFile = new File(sourceDir.getParent(), zipFileName + ".zip");
        ZipUtils.zip(sourceDir, zipFile);
        return zipFile;
    }

    /**
     * Creates (a part of) the distribution on the filesystem necessary to the run the tests.
     *
     * @param env  the directory structure to be created
     * @param identity  the identity name
     * @param productName  release name
     * @param productVersion  release version
     * @return  the bin directory
     * @throws Exception  if anything goes wrong
     */
    public static File createInstalledImage(DirectoryStructure env, String identity, String productName, String productVersion) throws Exception {
        // start from a base installation
        // with a file in it
        File binDir = mkdir(env.getInstalledImage().getJbossHome(), "bin");

        // create product.conf
        File productConf = new File(binDir, "product.conf");
        assertTrue("Failed to create product.conf", productConf.createNewFile());
        Properties props = new Properties();
        props.setProperty("slot", identity);

        try (final Writer writer = Files.newBufferedWriter(productConf.toPath(), StandardCharsets.UTF_8)) {
            props.store(writer, null);
        }

        // create the product module
        final File modulesDir = newFile(env.getInstalledImage().getModulesDir(), SYSTEM, LAYERS, BASE);
        if(!modulesDir.exists()) {
            modulesDir.mkdirs();
        }
        final File moduleDir = TestUtils.createModule1(modulesDir, "org.jboss.as.product:" + identity, "product.jar");

        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue(Name.MANIFEST_VERSION.toString(), "xxx");
        manifest.getMainAttributes().putValue("JBoss-Product-Release-Name", productName);
        manifest.getMainAttributes().putValue("JBoss-Product-Release-Version", productVersion);

        final File moduleJar = new File(new File(moduleDir, identity), "product.jar");
        JarOutputStream jar = null;
        try {
            jar = new JarOutputStream(new FileOutputStream(moduleJar), manifest);
            jar.flush();
        } finally {
            StreamUtils.safeClose(jar);
        }
        return binDir;
    }

    public static File[] getModuleRoot(final PatchableTarget target) {
        try {
            return PatchUtils.getModulePath(target.getDirectoryStructure(), target.loadTargetInfo());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create the legacy patch environment based on the default layout.
     *
     * @param jbossHome the $JBOSS_HOME
     * @return the patch environment
     * @deprecated see {@linkplain org.jboss.as.patching.installation.InstallationManager}
     */
    @Deprecated
    public static DirectoryStructure createLegacyTestStructure(final File jbossHome) {
        final File appClient = new File(jbossHome, APP_CLIENT);
        final File bundles = new File(jbossHome, BUNDLES);
        final File domain = new File(jbossHome, DOMAIN);
        final File modules = new File(jbossHome, MODULES);
        final File installation = new File(jbossHome, Constants.INSTALLATION);
        final File patches = new File(modules, PATCHES);
        final File standalone = new File(jbossHome, STANDALONE);
        return new LegacyDirectoryStructure(new InstalledImage() {

            @Override
            public File getJbossHome() {
                return jbossHome;
            }

            @Override
            public File getBundlesDir() {
                return bundles;
            }

            @Override
            public File getModulesDir() {
                return modules;
            }

            @Override
            public File getInstallationMetadata() {
                return installation;
            }

            @Override
            public File getLayersConf() {
                return new File(getModulesDir(), Constants.LAYERS_CONF);
            }

            @Override
            public File getPatchesDir() {
                return patches;
            }

            @Override
            public File getPatchHistoryDir(String patchId) {
                return newFile(getInstallationMetadata(), PATCHES, patchId);
            }

            @Override
            public File getAppClientDir() {
                return appClient;
            }

            @Override
            public File getDomainDir() {
                return domain;
            }

            @Override
            public File getStandaloneDir() {
                return standalone;
            }
        });
    }

    public interface ContentTask {

        /**
         * Write the content.
         *
         * @param target the target file
         * @return the created resources
         * @throws IOException for any error
         */
        String[] writeContent(File target) throws IOException;

    }

    static class LegacyDirectoryStructure extends DirectoryStructure {
        private final InstalledImage image;
        LegacyDirectoryStructure(final InstalledImage image) {
            this.image = image;
        }

        @Override
        public InstalledImage getInstalledImage() {
            return image;
        }

        public File getPatchesMetadata() {
            return new File(getInstalledImage().getPatchesDir(), METADATA);
        }

        @Override
        public File getInstallationInfo() {
            return new File(getPatchesMetadata(), INSTALLATION_METADATA);
        }

        public File getPatchDirectory(final String patchId) {
            return new File(getInstalledImage().getPatchesDir(), patchId);
        }

        @Override
        public File getBundlesPatchDirectory(final String patchId) {
            return new File(getPatchDirectory(patchId), BUNDLES);
        }

        @Override
        public File getModulePatchDirectory(final String patchId) {
            return new File(getPatchDirectory(patchId), MODULES);
        }

        @Override
        public File getBundleRepositoryRoot() {
            return getInstalledImage().getBundlesDir();
        }

        @Override
        public File getModuleRoot() {
            return getInstalledImage().getModulesDir();
        }
    }
}
