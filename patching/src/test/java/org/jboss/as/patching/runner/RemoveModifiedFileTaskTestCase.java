/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.patching.runner;

import static org.jboss.as.patching.HashUtils.hashFile;
import static org.jboss.as.patching.IoUtils.NO_CONTENT;
import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.patching.metadata.ModificationType.REMOVE;
import static org.jboss.as.patching.runner.PatchingAssert.assertFileDoesNotExist;
import static org.jboss.as.patching.runner.PatchingAssert.assertFileExists;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasBeenApplied;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasNotBeenApplied;
import static org.jboss.as.patching.runner.TestUtils.createPatchXMLFile;
import static org.jboss.as.patching.runner.TestUtils.createZippedPatchFile;
import static org.jboss.as.patching.runner.TestUtils.dump;
import static org.jboss.as.patching.runner.TestUtils.randomString;
import static org.jboss.as.patching.runner.TestUtils.touch;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.jboss.as.patching.ContentConflictsException;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.patching.tool.ContentVerificationPolicy;
import org.jboss.as.patching.tool.PatchTool;
import org.jboss.as.patching.tool.PatchingResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RemoveModifiedFileTaskTestCase extends AbstractTaskTestCase {

    private PatchTool runner;
    private File zippedPatch;
    private Patch patch;
    private ContentModification fileRemoved;
    private File removedFile;
    private byte[] expectedModifiedHash;


    @Before
    public void setUp() throws Exception{
        // with a file in it
        File binDir = mkdir(env.getInstalledImage().getJbossHome(), "bin");
        String fileName = "standalone.sh";
        removedFile = touch(binDir, fileName);
        dump(removedFile, "modified script to run standalone AS");
        expectedModifiedHash = hashFile(removedFile);
        // let's simulate that the file has been modified by the users by using a hash that is not the file checksum
        byte[] unmodifiedHash = randomString().getBytes(StandardCharsets.UTF_8);

        String patchID = randomString();
        File patchDir = mkdir(tempDir, patchID);

        File updatedFile = touch(patchDir, "misc", "bin", fileName);
        dump(updatedFile, "updated script");
        // build a one-off patch for the base installation
        // with 1 removed file
        fileRemoved = new ContentModification(new MiscContentItem(fileName, new String[] { "bin" }, NO_CONTENT), unmodifiedHash, REMOVE);

        patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .addContentModification(fileRemoved)
                .build();

        // create the patch
        createPatchXMLFile(patchDir, patch);
        zippedPatch = createZippedPatchFile(patchDir, patch.getPatchId());

        runner = newPatchTool();
    }

    @After
    public void tearDown() {
        super.tearDown();
        runner = null;
        zippedPatch = null;
        patch = null;
        fileRemoved = null;
        removedFile = null;
        expectedModifiedHash = null;
    }

    @Test
    public void testRemoveModifiedFileWithSTRICT() throws Exception {
        try {
            runner.applyPatch(zippedPatch, ContentVerificationPolicy.STRICT);
            fail("Patch shouldn't be applied - ContentVerificationPolicy.STRICT");
        } catch (ContentConflictsException e) {
            assertPatchHasNotBeenApplied(e, patch, fileRemoved.getItem(), env);

            /// file has not been modified in the AS7 installation
            assertFileExists(removedFile);
            assertArrayEquals(expectedModifiedHash, hashFile(removedFile));
        }

    }

    @Test
    public void testRemovedModifiedFileWithOVERRIDE_ALL() throws Exception {
        PatchingResult result = runner.applyPatch(zippedPatch, ContentVerificationPolicy.OVERRIDE_ALL);

        assertPatchHasBeenApplied(result, patch);

        /// file has been removed from the AS7 installation
        // and it's the new one
        assertFileDoesNotExist(removedFile);
        // the existing file has been backed up
        File backupFile = assertFileExists(env.getInstalledImage().getPatchHistoryDir(patch.getPatchId()), "misc", "bin", removedFile.getName());
        assertArrayEquals(expectedModifiedHash, hashFile(backupFile));
    }

    @Test
    public void testRemoveModifiedFileWithPRESERVE_ALL() throws Exception {
        try {
            runner.applyPatch(zippedPatch, ContentVerificationPolicy.PRESERVE_ALL);
            fail("Patch shouldn't be applied - ContentVerificationPolicy.PRESERVE_ALL");
        } catch (ContentConflictsException e) {
            assertPatchHasNotBeenApplied(e, patch, fileRemoved.getItem(), env);

            /// file has not been modified in the AS7 installation
            assertFileExists(removedFile);
            assertArrayEquals(expectedModifiedHash, hashFile(removedFile));
        }
    }
}
