/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.patching.runner;

import static org.jboss.as.patching.IoUtils.NO_CONTENT;
import static org.jboss.as.patching.IoUtils.copy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.as.patching.HashUtils;
import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.PatchingException;
import org.jboss.as.patching.logging.PatchLogger;
import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.ModificationCondition;
import org.jboss.as.patching.metadata.ModificationType;

/**
 * Task for removing a file. In case the removed file is a directory, this task will create multiple rollback operation
 * for each file in this folder.
 *
 * @author Emanuel Muckenhuber
 */
class FileRemoveTask implements PatchingTask {

    private final MiscContentItem item;
    private final File target;
    private final File backup;
    private final PatchingTaskDescription description;

    private final List<ContentModification> rollback = new ArrayList<ContentModification>();

    FileRemoveTask(PatchingTaskDescription description, File target, File backup) {
        this.target = target;
        this.backup = backup;
        this.description = description;
        this.item = description.getContentItem(MiscContentItem.class);
    }

    @Override
    public ContentItem getContentItem() {
        return item;
    }

    @Override
    public boolean isRelevant(PatchingTaskContext context) throws PatchingException {
        final ModificationCondition condition = description.getModification().getCondition();
        if(condition == null) {
            return true;
        }
        return condition.isSatisfied(context);
    }

    @Override
    public boolean prepare(PatchingTaskContext context) throws IOException {
        // we create the backup in any case, since it is possible that the task
        // will be processed anyhow if the user specified OVERRIDE_ALL policy.
        // If the task is undone, the patch history will be deleted (including this backup).
        backup(target, backup, Arrays.asList(item.getPath()), rollback, context);
        // See if the hash matches the metadata

        boolean isEmptyDirectory = false;
        if (target.isDirectory()) {
            final File[] children = target.listFiles();
            if (children == null || children.length == 0) {
                isEmptyDirectory = true;
            }
        }

        final byte[] expected = description.getModification().getTargetHash();
        final byte[] actual = isEmptyDirectory ? NO_CONTENT : HashUtils.hashFile(target);
        return Arrays.equals(expected, actual);
    }

    @Override
    public void execute(PatchingTaskContext context) throws IOException {
        // delete the file or directory recursively
        boolean ok = IoUtils.recursiveDelete(target);
        for(ContentModification mod : rollback) {
            // Add the rollback (add actions)
            // We skip the change - misc files are reused in the processed patch
            context.recordChange(null, mod);
        }
        if(! ok) {
            throw PatchLogger.ROOT_LOGGER.failedToDelete(target.getAbsolutePath());
        }
    }

    void backup(final File root, final File backupLocation, final List<String> path, final List<ContentModification> rollback, final PatchingTaskContext context) throws IOException {
        if(!root.exists()) {
            // Perhaps an error condition?
        } else if(root.isDirectory()) {
            final File[] files = root.listFiles();
            final String rootName = root.getName();
            if(files == null || files.length == 0) {
                // Create empty directory
                rollback.add(createRollbackItem(rootName, path, NO_CONTENT, true));
            } else {
                final List<String> newPath = new ArrayList<String>(path);
                newPath.add(rootName);
                for (File file : files) {
                    final String name = file.getName();
                    final File newBackupLocation = new File(backupLocation, name);
                    backup(file, newBackupLocation, newPath, rollback, context);
                }
            }
        } else {
            // Copy and record the backup action
            final byte[] hash = copy(root, backupLocation);
            rollback.add(createRollbackItem(root.getName(), path, hash, false));
        }
    }

    static ContentModification createRollbackItem(String name, List<String> path,  byte[] backupHash, boolean directory) {
        final MiscContentItem backupItem = new MiscContentItem(name, path, backupHash, directory);
        return new ContentModification(backupItem, NO_CONTENT, ModificationType.ADD);
    }
}
