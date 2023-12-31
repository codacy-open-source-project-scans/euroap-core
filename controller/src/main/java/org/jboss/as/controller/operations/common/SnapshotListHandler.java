/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.controller.operations.common;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.AuthorizationResult;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.persistence.ConfigurationPersister.SnapshotInfo;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * An operation that lists the snapshots taken of the current configuration
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class SnapshotListHandler implements OperationStepHandler {

    private static final String OPERATION_NAME = "list-snapshots";

    private static final SimpleAttributeDefinition DIRECTORY = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.DIRECTORY, ModelType.STRING)
            .setRequired(true)
            .build();
    private static final AttributeDefinition NAMES = new StringListAttributeDefinition.Builder(ModelDescriptionConstants.NAMES)
            .build();

    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME, ControllerResolver.getResolver("snapshot"))
            .setReplyParameters(DIRECTORY, NAMES)
            .setReadOnly()
            .setRuntimeOnly()
            .withFlag(OperationEntry.Flag.MASTER_HOST_CONTROLLER_ONLY)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SNAPSHOTS)
            .build();

    private final ConfigurationPersister persister;

    public SnapshotListHandler(ConfigurationPersister persister) {
        this.persister = persister;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        AuthorizationResult authorizationResult = context.authorize(operation);
        if (authorizationResult.getDecision() == AuthorizationResult.Decision.DENY) {
            throw ControllerLogger.ROOT_LOGGER.unauthorized(operation.get(OP).asString(), context.getCurrentAddress(), authorizationResult.getExplanation());
        }

        try {
            SnapshotInfo info = persister.listSnapshots();
            ModelNode result = context.getResult();
            result.get(ModelDescriptionConstants.DIRECTORY).set(info.getSnapshotDirectory());
            result.get(ModelDescriptionConstants.NAMES).setEmptyList();
            for (String name : info.names()) {
                result.get(ModelDescriptionConstants.NAMES).add(name);
            }
        } catch (Exception e) {
            throw new OperationFailedException(e);
        }
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

}
