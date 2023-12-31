/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.subsystem.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_DEFAULTS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_TRANSFORMED_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.global.ReadResourceHandler;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.as.controller.transform.TransformationTargetImpl;
import org.jboss.as.controller.transform.TransformerRegistry;
import org.jboss.as.controller.transform.Transformers;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
class ReadTransformedResourceOperation implements OperationStepHandler {
    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(READ_TRANSFORMED_RESOURCE_OPERATION, null)
            .setPrivateEntry()
            .build();
    private final TransformerRegistry transformerRegistry;
    private final ModelVersion coreModelVersion;
    private final ModelVersion subsystemModelVersion;

    ReadTransformedResourceOperation(final TransformerRegistry transformerRegistry, ModelVersion coreModelVersion, ModelVersion subsystemModelVersion) {
        this.transformerRegistry = transformerRegistry;
        this.coreModelVersion = coreModelVersion;
        this.subsystemModelVersion = subsystemModelVersion;
    }

    private ModelNode transformReadResourceResult(final OperationContext context, ModelNode original, String subsystem) throws OperationFailedException {
        ModelNode rootData = original.get(ModelDescriptionConstants.RESULT);

        Map<PathAddress, ModelVersion> subsystemVersions = new HashMap<>();
        subsystemVersions.put(PathAddress.EMPTY_ADDRESS.append(ModelDescriptionConstants.SUBSYSTEM, subsystem), subsystemModelVersion);

        final TransformationTarget target = TransformationTargetImpl.create(null, transformerRegistry, coreModelVersion,
                subsystemVersions, TransformationTarget.TransformationTargetType.SERVER);
        final Transformers transformers = Transformers.Factory.create(target);

        final ImmutableManagementResourceRegistration rr = context.getRootResourceRegistration();
        Resource root = TransformationUtils.modelToResource(rr, rootData, true);
        Transformers.TransformationInputs transformationInputs = Transformers.TransformationInputs.getOrCreate(context);
        Resource transformed = transformers.transformRootResource(transformationInputs, root);

        return Resource.Tools.readModel(transformed);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final String subsystem = operation.get(SUBSYSTEM).asString();
        final boolean includeDefaults = operation.get(INCLUDE_DEFAULTS).asBoolean(true);
        // Add a step to transform the result of a READ_RESOURCE.
        // Do this first, Stage.IMMEDIATE
        final ModelNode readResourceResult = new ModelNode();
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                ModelNode transformed = transformReadResourceResult(context, readResourceResult, subsystem);
                context.getResult().set(transformed);
            }
        }, OperationContext.Stage.MODEL, true);

        // Now add a step to do the READ_RESOURCE, also IMMEDIATE. This will execute *before* the one ^^^
        final ModelNode op = new ModelNode();
        op.get(OP).set(READ_RESOURCE_OPERATION);
        op.get(OP_ADDR).set(PathAddress.EMPTY_ADDRESS.toModelNode());
        op.get(RECURSIVE).set(true);
        op.get(INCLUDE_DEFAULTS).set(includeDefaults);
        context.addStep(readResourceResult, op, ReadResourceHandler.INSTANCE, OperationContext.Stage.MODEL, true);
    }
}
