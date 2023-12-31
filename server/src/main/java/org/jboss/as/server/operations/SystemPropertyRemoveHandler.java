/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.server.operations;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.ProcessEnvironmentSystemPropertyUpdater;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Handler for system property remove operations.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SystemPropertyRemoveHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = REMOVE;

    public static final SystemPropertyRemoveHandler INSTANCE = new SystemPropertyRemoveHandler(null);

    public static ModelNode getOperation(ModelNode address, String name) {
        ModelNode op = Util.getEmptyOperation(OPERATION_NAME, address);
        op.get(NAME).set(name);
        return op;
    }

    private final ProcessEnvironmentSystemPropertyUpdater systemPropertyUpdater;

    /**
     * Create the SystemPropertyRemoveHandler
     * @param systemPropertyUpdater the process environment to use to validate changes. May be {@code null}
     */
    public SystemPropertyRemoveHandler(ProcessEnvironmentSystemPropertyUpdater systemPropertyUpdater) {
        this.systemPropertyUpdater = systemPropertyUpdater;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final ModelNode model = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        context.removeResource(PathAddress.EMPTY_ADDRESS);

        final String name = PathAddress.pathAddress(operation.get(OP_ADDR)).getLastElement().getValue();
        final String oldValue = model.hasDefined(VALUE) ? model.get(VALUE).asString() : null;

        final boolean applyToRuntime = systemPropertyUpdater != null &&
                systemPropertyUpdater.isRuntimeSystemPropertyUpdateAllowed(name, oldValue, context.isBooting());
        final boolean reload = !applyToRuntime && context.getProcessType().isServer();

        if (applyToRuntime) {
            WildFlySecurityManager.clearPropertyPrivileged(name);
            if (systemPropertyUpdater != null) {
                systemPropertyUpdater.systemPropertyUpdated(name, null);
            }
        } else if (reload) {
            context.reloadRequired();
        }

        context.completeStep(new OperationContext.RollbackHandler() {
            @Override
            public void handleRollback(OperationContext context, ModelNode operation) {
                if (applyToRuntime) {
                    WildFlySecurityManager.setPropertyPrivileged(name, oldValue);
                    if (systemPropertyUpdater != null) {
                        systemPropertyUpdater.systemPropertyUpdated(name, oldValue);
                    }
                } else if (reload) {
                    context.revertReloadRequired();
                }
            }
        });
    }
}
