/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.patching.tests;

import static org.jboss.as.patching.Constants.BASE;
import static org.jboss.as.patching.Constants.BUNDLES;
import static org.jboss.as.patching.Constants.LAYERS;
import static org.jboss.as.patching.Constants.MODULES;
import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.patching.runner.TestUtils.randomString;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.PatchingException;
import org.jboss.as.patching.installation.InstallationManager;
import org.jboss.as.patching.installation.InstallationManagerImpl;
import org.jboss.as.patching.installation.InstalledIdentity;
import org.jboss.as.patching.installation.PatchableTarget;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchXml;
import org.jboss.as.patching.tool.ContentVerificationPolicy;
import org.jboss.as.patching.tool.PatchTool;
import org.jboss.as.patching.tool.PatchingResult;
import org.jboss.as.version.ProductConfig;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

/**
 * @author Emanuel Muckenhuber
 */
public class AbstractPatchingTest {

    protected static final String PRODUCT_NAME = "Product";
    protected static final String PRODUCT_VERSION = "6.2.0.GA";
    protected static final String JBOSS_INSTALLATION = "jboss-installation";

    private static final String SYSTEM_TEMP_DIR = System.getProperty("java.io.tmpdir");

    protected File tempDir;
    private ProductConfig productConfig;
    private InstallationManager installationManager;

    @Before
    public void setUp() throws IOException {
        tempDir = mkdir(new File(SYSTEM_TEMP_DIR), randomString());
        final File jbossHome = mkdir(tempDir, JBOSS_INSTALLATION);
        mkdir(jbossHome, MODULES, "system", LAYERS, BASE);
        mkdir(jbossHome, BUNDLES, "system", LAYERS, BASE);

        productConfig = new ProductConfig(PRODUCT_NAME, PRODUCT_VERSION, "main");
    }

    @After
    public void tearDown() {
        if (!IoUtils.recursiveDelete(tempDir)) {
            tempDir.deleteOnExit();
        }
    }

    protected InstallationManager updateInstallationManager() throws IOException {
        this.installationManager = loadInstallationManager();
        return installationManager;
    }

    protected InstallationManager loadInstallationManager() throws IOException {
        final File jbossHome = new File(tempDir, JBOSS_INSTALLATION);
        final File modules = new File(jbossHome, MODULES);
        return InstallationManager.load(jbossHome, Collections.singletonList(modules), Collections.emptyList(), productConfig);
    }

    PatchableTarget getLayer(final String layerName) {
        return installationManager.getDefaultIdentity().getLayer(layerName);
    }

    /**
     * Create the default builder and install the base layer.
     *
     * @return a patching test builder
     * @throws IOException
     */
    protected PatchingTestBuilder createDefaultBuilder() throws IOException {
        final File jbossHome = new File(tempDir, JBOSS_INSTALLATION);
        final File modules = new File(jbossHome, "modules");
        installLayer(modules, null, false, Constants.BASE);
        installationManager = loadInstallationManager();
        return createBuilder();
    }

    /**
     * Create the default builder, installing a few layers.
     *
     * @param layers the layers
     * @return the patching test builder
     * @throws IOException
     */
    protected PatchingTestBuilder createDefaultBuilder(String... layers) throws IOException {
        final File jbossHome = new File(tempDir, JBOSS_INSTALLATION);
        final File modules = new File(jbossHome, "modules");
        final File layersConf = new File(modules, "layers.conf");
        installLayer(modules, layersConf, false, layers);
        installationManager = loadInstallationManager();
        return createBuilder();
    }

    protected PatchingTestBuilder createBuilder() {
        return new PatchingTestBuilder(tempDir);
    }

    protected PatchingResult apply(final PatchingTestStepBuilder builder) throws PatchingException {
        return apply(builder, ContentVerificationPolicy.STRICT, PatchStepAssertions.APPLY);
    }

    protected PatchingResult apply(final PatchingTestStepBuilder builder, final ContentVerificationPolicy verificationPolicy) throws PatchingException {
        return apply(builder, verificationPolicy, PatchStepAssertions.APPLY);
    }

    protected PatchingResult apply(final PatchingTestStepBuilder builder, final ContentVerificationPolicy verificationPolicy, final PatchStepAssertions assertions) throws PatchingException {
        final Patch patch = builder.build();
        final File installation = new File(tempDir, JBOSS_INSTALLATION);
        try {
            assertions.before(installation, patch, installationManager);
        } catch (IOException e) {
            throw new PatchingException(e);
        }
        // Write patch
        writePatch(builder.getPatchDir(), patch);
        // Create the patch tool and apply the patch
        final PatchTool patchTool = PatchTool.Factory.create(installationManager);
        final PatchingResult result = patchTool.applyPatch(builder.getPatchDir(), verificationPolicy);
        result.commit();
        final InstalledIdentity identity = ((InstallationManagerImpl)installationManager).getInstalledIdentity(patch.getIdentity().getName(), null);
        Assert.assertTrue(identity.getAllInstalledPatches().contains(patch.getPatchId()));
        try {
            assertions.after(installation, patch, installationManager);
        } catch (IOException e) {
            throw new PatchingException(e);
        }
        return result;
    }

    protected PatchingResult rollback(PatchingTestStepBuilder step) throws PatchingException {
        return rollback(step, ContentVerificationPolicy.STRICT);
    }

    protected PatchingResult rollback(PatchingTestStepBuilder step, ContentVerificationPolicy verificationPolicy) throws PatchingException {
        return rollback(step, verificationPolicy, PatchStepAssertions.ROLLBACK);
    }

    protected PatchingResult rollback(final PatchingTestStepBuilder builder, final ContentVerificationPolicy verificationPolicy, final PatchStepAssertions assertions) throws PatchingException {
        return rollback(builder, verificationPolicy, assertions, false);
    }

    protected PatchingResult rollback(final PatchingTestStepBuilder builder, final ContentVerificationPolicy verificationPolicy, final PatchStepAssertions assertions, boolean rollbackTo) throws PatchingException {
        final Patch patch = builder.build();
        final File installation = new File(tempDir, JBOSS_INSTALLATION);
        try {
            assertions.before(installation, patch, installationManager);
        } catch (IOException e) {
            throw new PatchingException(e);
        }
        final String patchId = patch.getPatchId();
        final PatchTool patchTool = PatchTool.Factory.create(installationManager);
        final PatchingResult result = patchTool.rollback(patchId, verificationPolicy, rollbackTo, false);
        result.commit();
        final InstalledIdentity identity = installationManager.getInstalledIdentity(patch.getIdentity().getName(), null);
        Assert.assertFalse(identity.getAllInstalledPatches().contains(patch.getPatchId()));
        try {
            assertions.after(installation, patch, installationManager);
        } catch (IOException e) {
            throw new PatchingException(e);
        }
        return result;
    }

    protected static void writePatch(final File patchRoot, final Patch patch) throws PatchingException {
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(new File(patchRoot, PatchXml.PATCH_XML));
            PatchXml.marshal(os, patch);
        } catch (Exception e) {
            throw new PatchingException(e);
        } finally {
            IoUtils.safeClose(os);
        }
    }

    private static void installLayer(File baseDir, File layerConf, boolean excludeBase, String... layers) throws IOException {
        for (String layer : layers) {
            IoUtils.mkdir(baseDir, "system", "layers", layer);
        }
        if (layerConf != null) {
            Properties props = new Properties();
            StringBuilder str = new StringBuilder();
            for (int i = 0; i < layers.length; i++) {
                if (i > 0) {
                    str.append(',');
                }
                str.append(layers[i]);
            }
            props.put(Constants.LAYERS, str.toString());
            props.put(Constants.EXCLUDE_LAYER_BASE, String.valueOf(excludeBase));
            final FileOutputStream os = new FileOutputStream(layerConf);
            try {
                props.store(os, "");
            } finally {
                IoUtils.safeClose(os);
            }
        }
    }
}
