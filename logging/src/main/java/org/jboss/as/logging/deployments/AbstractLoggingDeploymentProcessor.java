/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.logging.deployments;

import java.io.Closeable;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.logging.logging.LoggingLogger;
import org.jboss.as.logging.logmanager.WildFlyLogContextSelector;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.logmanager.LogContext;
import org.jboss.modules.Module;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
abstract class AbstractLoggingDeploymentProcessor implements DeploymentUnitProcessor {

    private static final AttachmentKey<LogContext> LOG_CONTEXT_KEY = AttachmentKey.create(LogContext.class);
    private static final AttachmentKey<LogContext> DEFAULT_LOG_CONTEXT_KEY = AttachmentKey.create(LogContext.class);

    final WildFlyLogContextSelector logContextSelector;

    AbstractLoggingDeploymentProcessor(final WildFlyLogContextSelector logContextSelector) {
        this.logContextSelector = logContextSelector;
    }

    @Override
    public final void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        // If the log context is already defined, skip the rest of the processing
        if (!hasRegisteredLogContext(deploymentUnit)) {
            if (deploymentUnit.hasAttachment(Attachments.DEPLOYMENT_ROOT)) {
                // don't process sub-deployments as they are processed by processing methods
                final ResourceRoot root = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
                if (SubDeploymentMarker.isSubDeployment(root)) return;
                processDeployment(phaseContext, deploymentUnit, root);
                // If we still don't have a context registered on the root deployment, register the current context.
                // This is done to avoid any logging from the root deployment to have access to a sub-deployments log
                // context. For example any library logging from a EAR/lib should use the EAR's configured log context,
                // not a log context from a WAR or EJB library.
                if (!hasRegisteredLogContext(deploymentUnit) && !deploymentUnit.hasAttachment(DEFAULT_LOG_CONTEXT_KEY)) {
                    // Register the current log context as this could be an embedded server or overridden another way
                    registerLogContext(deploymentUnit, DEFAULT_LOG_CONTEXT_KEY, deploymentUnit.getAttachment(Attachments.MODULE), LogContext.getLogContext());
                }
            }
        }
    }

    @Override
    public final void undeploy(final DeploymentUnit context) {
        // don't process sub-deployments as they are processed by processing methods
        final ResourceRoot root = context.getAttachment(Attachments.DEPLOYMENT_ROOT);
        if (SubDeploymentMarker.isSubDeployment(root)) return;
        // Remove any log context selector references
        final Module module = context.getAttachment(Attachments.MODULE);
        // Remove either the default log context or a defined log context. It's safe to attempt to remove a
        // nonexistent context.
        unregisterLogContext(context, DEFAULT_LOG_CONTEXT_KEY, module);
        unregisterLogContext(context, LOG_CONTEXT_KEY, module);
        // Unregister all sub-deployments
        final List<DeploymentUnit> subDeployments = getSubDeployments(context);
        for (DeploymentUnit subDeployment : subDeployments) {
            final Module subDeploymentModule = subDeployment.getAttachment(Attachments.MODULE);
            // Sub-deployment should never have a default log context
            unregisterLogContext(subDeployment, LOG_CONTEXT_KEY, subDeploymentModule);
        }
    }

    /**
     * Processes the deployment.
     *
     * @param phaseContext   the phase context
     * @param deploymentUnit the deployment unit
     * @param root           the root resource
     *
     * @throws DeploymentUnitProcessingException if an error occurs during processing
     */
    protected abstract void processDeployment(DeploymentPhaseContext phaseContext, DeploymentUnit deploymentUnit, ResourceRoot root) throws DeploymentUnitProcessingException;

    void registerLogContext(final DeploymentUnit deploymentUnit, final Module module, final LogContext logContext) {
        // If the default log context is registered we need to remove it and unregister before we register a defined log
        // context
        unregisterLogContext(deploymentUnit, DEFAULT_LOG_CONTEXT_KEY, module);
        registerLogContext(deploymentUnit, LOG_CONTEXT_KEY, module, logContext);
    }

    private void registerLogContext(final DeploymentUnit deploymentUnit, final AttachmentKey<LogContext> attachmentKey, final Module module, final LogContext logContext) {
        LoggingLogger.ROOT_LOGGER.tracef("Registering LogContext %s for deployment %s", logContext, deploymentUnit.getName());
        if (WildFlySecurityManager.isChecking()) {
            WildFlySecurityManager.doUnchecked(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    logContextSelector.registerLogContext(module.getClassLoader(), logContext);
                    return null;
                }
            });
        } else {
            logContextSelector.registerLogContext(module.getClassLoader(), logContext);
        }
        // Add the log context to the sub-deployment unit for later removal
        deploymentUnit.putAttachment(attachmentKey, logContext);
    }

    private void unregisterLogContext(final DeploymentUnit deploymentUnit, final AttachmentKey<LogContext> attachmentKey, final Module module) {
        final LogContext logContext = deploymentUnit.removeAttachment(attachmentKey);
        if (logContext != null) {
            final boolean success;
            if (WildFlySecurityManager.isChecking()) {
                success = WildFlySecurityManager.doUnchecked(new PrivilegedAction<Boolean>() {
                    @Override
                    public Boolean run() {
                        return logContextSelector.unregisterLogContext(module.getClassLoader(), logContext);
                    }
                });
            } else {
                success = logContextSelector.unregisterLogContext(module.getClassLoader(), logContext);
            }
            if (success) {
                LoggingLogger.ROOT_LOGGER.tracef("Removed LogContext '%s' from '%s'", logContext, module);
            } else {
                LoggingLogger.ROOT_LOGGER.logContextNotRemoved(logContext, deploymentUnit.getName());
            }
        }
    }

    static List<DeploymentUnit> getSubDeployments(final DeploymentUnit deploymentUnit) {
        if (deploymentUnit.hasAttachment(Attachments.SUB_DEPLOYMENTS)) {
            return new ArrayList<>(deploymentUnit.getAttachmentList(Attachments.SUB_DEPLOYMENTS));
        }
        return Collections.emptyList();
    }

    static void safeClose(final Closeable closable) {
        if (closable != null) try {
            closable.close();
        } catch (Exception e) {
            // no-op
        }
    }

    /**
     * Checks the deployment to see if it has a registered {@link org.jboss.logmanager.LogContext log context}.
     *
     * @param deploymentUnit the deployment unit to check
     *
     * @return {@code true} if the deployment unit has a log context, otherwise {@code false}
     */
    static boolean hasRegisteredLogContext(final DeploymentUnit deploymentUnit) {
        return deploymentUnit.hasAttachment(LOG_CONTEXT_KEY);
    }
}
