/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.patching.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.jboss.as.patching.installation.InstalledIdentity;

/**
 * A validating iterator over the patch history.
 *
 * @author Alexey Loubyansky
 * @author Emanuel Muckenhuber
 */
public interface PatchHistoryIterator {

    /**
     * Returns {@code true} if the iteration has more elements.
     * (In other words, returns {@code true} if {@link #next} would
     * return an element rather than throwing an exception.)
     *
     * @return {@code true} if the iteration has more elements
     */
    boolean hasNext();

    /**
     * Retrieve the next patch element without validating the patch nor
     * changing the state of the iterator.
     *
     * @return the next patch
     */
    String peek();

    /**
     * Returns the next validated patch using the default validator
     *
     * @return the next patch in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    String next();

    /**
     * Returns the next validated patch.
     *
     * @param context the validation context which should be used for this node
     * @return the next patch in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    String next(PatchingValidationErrorHandler context);

    public static final class Builder {

        public static Builder create(final InstalledIdentity identity) {
            return new Builder(identity);
        }

        private final InstalledIdentity identity;
        private final PatchHistoryValidations.PatchingArtifactStateHandlers handlers = new PatchHistoryValidations.PatchingArtifactStateHandlers();
        private PatchingValidationErrorHandler errorHandler = PatchingValidationErrorHandler.DEFAULT;

        private Builder(final InstalledIdentity identity) {
            this.identity = identity;
        }

        public void setErrorHandler(PatchingValidationErrorHandler errorHandler) {
            this.errorHandler = errorHandler;
        }

        public <P extends PatchingArtifact.ArtifactState, S extends PatchingArtifact.ArtifactState> void addStateHandler(PatchingArtifact<P, S> artifact, PatchingArtifactStateHandler<S> handler) {
            handlers.put(artifact, handler);
        }

        public PatchHistoryIterator iterator() {
            // Process the patches in from the latest to the oldest
            final List<String> patches = new ArrayList<>(identity.getAllInstalledPatches());
            Collections.reverse(patches);
            final int size = patches.size();
            final BasicArtifactProcessor processor = new BasicArtifactProcessor(identity, errorHandler, handlers);
            final PatchHistoryIterator iterator = new PatchHistoryIterator() {
                int idx = 0;

                @Override
                public boolean hasNext() {
                    return idx != size;
                }

                @Override
                public String peek() {
                    int i = idx + 1;
                    if (i >= size) {
                        throw new NoSuchElementException();
                    }
                    return patches.get(idx);
                }

                @Override
                public String next() {
                    return next(errorHandler);
                }

                @Override
                public String next(PatchingValidationErrorHandler context) {
                    int i = idx;
                    if (i >= size) {
                        throw new NoSuchElementException();
                    }
                    idx = i + 1;
                    final String patch = patches.get(i);
                    final int nextIdx = i + 1;
                    String nextPatch = null;
                    if (nextIdx < size) {
                        nextPatch = patches.get(nextIdx);
                    }
                    final PatchingArtifacts.PatchID patchID = new PatchingArtifacts.PatchID(patch, nextPatch);
                    processor.processRoot(PatchingArtifacts.HISTORY_RECORD, patchID, context);
                    return patch;
                }
            };
            return iterator;
        }

    }

}
