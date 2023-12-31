/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.controller.client.helpers.standalone.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.as.controller.client.helpers.standalone.ServerUpdateActionResult.Result;
import org.jboss.as.controller.client.helpers.standalone.SimpleServerDeploymentActionResult;
import org.jboss.dmr.ModelNode;

/**
 * Adapts from Future<ModelNode> to Future<ServerDeploymentPlanResult>.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class ServerDeploymentPlanResultFuture implements Future<ServerDeploymentPlanResult> {

    private final Future<ModelNode> nodeFuture;
    private final DeploymentPlanImpl plan;

    ServerDeploymentPlanResultFuture(final DeploymentPlanImpl plan, final Future<ModelNode> nodeFuture) {
        this.plan = plan;
        this.nodeFuture = nodeFuture;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean cancelled = nodeFuture.cancel(mayInterruptIfRunning);
        if (cancelled) {
            plan.cleanup();
        } // else wait for finalize()
        return cancelled;
    }

    @Override
    public boolean isCancelled() {
        return nodeFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return nodeFuture.isDone();
    }

    @Override
    public ServerDeploymentPlanResult get() throws InterruptedException, ExecutionException {
        boolean cleanup = true;
        ModelNode node;
        try {
            node = nodeFuture.get();
        } catch (InterruptedException ie) {
            cleanup = false;  // still may be in progress, so wait for finalize()
            throw ie;
        } finally {
            if (cleanup) {
                plan.cleanup();
            }
        }
        return getResultFromNode(node.get(ClientConstants.RESULT));
    }

    @Override
    public ServerDeploymentPlanResult get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
        boolean cleanup = true;
        ModelNode node;
        try {
            node = nodeFuture.get(timeout, unit);
        } catch (InterruptedException ie) {
            cleanup = false;  // still may be in progress, so wait for finalize()
            throw ie;
        } catch (TimeoutException te) {
            cleanup = false;  // still may be in progress, so wait for finalize()
            throw te;
        } finally {
            if (cleanup) {
                plan.cleanup();
            }
        }
        return getResultFromNode(node.get(ClientConstants.RESULT));
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        plan.cleanup();
    }

    private ServerDeploymentPlanResult getResultFromNode(ModelNode planResultNode) {
        UUID planId = plan.getId();
        Map<UUID, ServerDeploymentActionResult> actionResults = new HashMap<UUID, ServerDeploymentActionResult>();
        List<DeploymentActionImpl> actions = plan.getDeploymentActionImpls();
        for (int i = 0; i < actions.size(); i++) {
            DeploymentActionImpl action = actions.get(i);
            UUID actionId = action.getId();
            ModelNode actionResultNode = planResultNode.get("step-" + (i + 1));
            actionResults.put(actionId, getActionResult(actionId, actionResultNode));
        }
        return new DeploymentPlanResultImpl(planId, actionResults);
    }

    private ServerDeploymentActionResult getActionResult(UUID actionId, ModelNode actionResultNode) {
        ServerDeploymentActionResult actionResult = null;
        String outcome = actionResultNode.hasDefined("outcome") ? actionResultNode.get("outcome").asString() : null;
        if (outcome == null || "cancelled".equals(outcome)) {
            actionResult = new SimpleServerDeploymentActionResult(actionId, Result.NOT_EXECUTED);
        } else if ("failed".equals(outcome)) {
            Exception e = actionResultNode.hasDefined("failure-description") ? new Exception(actionResultNode.get("failure-description").toString()) : null;
            if (actionResultNode.hasDefined("rolled-back") && actionResultNode.get("rolled-back").asBoolean()) {
                if (e == null) {
                    actionResult = new SimpleServerDeploymentActionResult(actionId, Result.ROLLED_BACK);
                } else {
                    actionResult = new SimpleServerDeploymentActionResult(actionId, Result.ROLLED_BACK, e);
                }
            } else {
                actionResult = new SimpleServerDeploymentActionResult(actionId, e);
            }
        } else {
            actionResult = new SimpleServerDeploymentActionResult(actionId, Result.EXECUTED);
        }
        // FIXME deal with shutdown possibilities
        return actionResult;
    }

}
