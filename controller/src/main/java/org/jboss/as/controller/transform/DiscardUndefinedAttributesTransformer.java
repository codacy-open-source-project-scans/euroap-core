/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.controller.transform;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Transformer that hides new attributes from legacy slaves if the attribute value is undefined. A defined value
 * leads to a log warning or an {@link OperationFailedException} unless the resource is ignored by the target.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class DiscardUndefinedAttributesTransformer implements ResourceTransformer, OperationTransformer {

    private final Set<String> attributeNames;
    private final OperationTransformer writeAttributeTransformer = new WriteAttributeTransformer();
    private final OperationTransformer undefineAttributeTransformer = new UndefineAttributeTransformer();

    public DiscardUndefinedAttributesTransformer(AttributeDefinition... attributes) {
        this(namesFromDefinitions(attributes));
    }

    private static Set<String> namesFromDefinitions(AttributeDefinition... attributes) {
        final Set<String> names = new HashSet<String>();
        for (final AttributeDefinition def : attributes) {
            names.add(def.getName());
        }
        return names;
    }

    public DiscardUndefinedAttributesTransformer(String... attributeNames) {
        this(new HashSet<String>(Arrays.asList(attributeNames)));
    }

    public DiscardUndefinedAttributesTransformer(Set<String> attributeNames) {
        this.attributeNames = attributeNames;
    }

    public OperationTransformer getWriteAttributeTransformer() {
        return writeAttributeTransformer;
    }

    public OperationTransformer getUndefineAttributeTransformer() {
        return undefineAttributeTransformer;
    }


    @Override
    public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
        transformResourceInt(context, address, resource);

        final ResourceTransformationContext childContext = context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);
        childContext.processChildren(resource);
    }

    @Override
    public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address,
                                                   final ModelNode operation) throws OperationFailedException {

        final Set<String> problems = checkModelNode(operation);
        final boolean reject = problems != null;
        final OperationRejectionPolicy rejectPolicy;
        if (reject) {
            rejectPolicy = new OperationRejectionPolicy() {
                @Override
                public boolean rejectOperation(ModelNode preparedResult) {
                    // Reject successful operations
                    return true;
                }

                @Override
                public String getFailureDescription() {
                    return context.getLogger().getAttributeWarning(address, operation, problems);
                }
            };
        } else {
            rejectPolicy = OperationTransformer.DEFAULT_REJECTION_POLICY;
        }
        // Return untransformed
        return new TransformedOperation(operation, rejectPolicy, OperationResultTransformer.ORIGINAL_RESULT);
    }

    private void transformResourceInt(TransformationContext context, PathAddress address,
                                      Resource resource) throws OperationFailedException {

        Set<String> problems = checkModelNode(resource.getModel());
        if (problems != null) {
            if (context.getTarget().isIgnoredResourceListAvailableAtRegistration()) {
                // Slave is 7.2.x or higher and we know this resource is not ignored
                List<String> msg = Collections.singletonList(context.getLogger().getAttributeWarning(address, null, problems));

                final TransformationTarget tgt = context.getTarget();
                final String legacyHostName = tgt.getHostName();
                final ModelVersion coreVersion = tgt.getVersion();
                final String subsystemName = findSubsystemName(address);
                final ModelVersion usedVersion = subsystemName == null ? coreVersion : tgt.getSubsystemVersion(subsystemName);

                // Target is  7.2.x or higher so we should throw an error
                if (subsystemName != null) {
                    throw ControllerLogger.ROOT_LOGGER.rejectAttributesSubsystemModelResourceTransformer(address, legacyHostName, subsystemName, usedVersion, msg);
                }
                throw ControllerLogger.ROOT_LOGGER.rejectAttributesCoreModelResourceTransformer(address, legacyHostName, usedVersion, msg);
            } else {
                // 7.1.x slave; resource *may* be ignored so we can't fail; just log
                context.getLogger().logAttributeWarning(address, problems);
            }
        }
    }


    private static String findSubsystemName(PathAddress pathAddress) {
        for (PathElement element : pathAddress) {
            if (element.getKey().equals(SUBSYSTEM)) {
                return element.getValue();
            }
        }
        return null;
    }

    private Set<String> checkModelNode(ModelNode modelNode) {

        Set<String> problems = null;
        for (String attr : attributeNames) {
            if (modelNode.has(attr)) {
                if (modelNode.hasDefined(attr)) {
                    if (problems == null) {
                        problems = new HashSet<String>();
                    }
                    problems.add(attr);
                } else {
                    modelNode.remove(attr);
                }
            }
        }
        return problems;
    }

    private class WriteAttributeTransformer implements OperationTransformer {

        @Override
        public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation) throws OperationFailedException {
            final String attribute = operation.require(NAME).asString();
            boolean ourAttribute = attributeNames.contains(attribute);
            final boolean rejectResult = ourAttribute && operation.hasDefined(VALUE);
            if (rejectResult) {
                // Create the rejection policy
                final OperationRejectionPolicy rejectPolicy = new OperationRejectionPolicy() {
                    @Override
                    public boolean rejectOperation(ModelNode preparedResult) {
                        // Reject successful operations
                        return true;
                    }

                    @Override
                    public String getFailureDescription() {
                        return context.getLogger().getAttributeWarning(address, operation, attribute);
                    }
                };
                return new TransformedOperation(operation, rejectPolicy, OperationResultTransformer.ORIGINAL_RESULT);
            } else if (ourAttribute) {
                // It's an attribute the slave doesn't understand, but the new value is "undefined"
                // Just discard this operation
                return OperationTransformer.DISCARD.transformOperation(context, address, operation);
            }
            // Not relevant to us
            return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
        }
    }

    private class UndefineAttributeTransformer implements OperationTransformer {

        @Override
        public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation) throws OperationFailedException {
            final String attribute = operation.require(NAME).asString();
            if (attributeNames.contains(attribute)) {
                // It's an attribute the slave doesn't understand, but the new value is "undefined"
                // Just discard this operation
                return OperationTransformer.DISCARD.transformOperation(context, address, operation);
            }
            // Not relevant to us
            return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
        }
    }
}
