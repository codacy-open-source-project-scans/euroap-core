/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.controller.client.helpers.domain;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Encapsulates a related set of actions a {@link DomainDeploymentManager} should
 * take to change the content deployed in the servers associated with one or
 * more server groups.
 *
 * @author Brian Stansberry
 */
public interface DeploymentSetPlan {

    /**
     * Gets the unique id of the deployment set plan.
     *
     * @return the id. Will not be <code>null</code>
     */
    UUID getId();

    /**
     * Gets the list of deploy, replace and undeploy actions that are part
     * of the deployment plan.
     *
     * @return  the actions. Will not be <code>null</code>
     */
    List<DeploymentAction> getDeploymentActions();

    /**
     * Gets whether all <code>deploy</code>, <code>undeploy</code>, <code>replace</code>
     * or <code>remove</code> operations associated with the deployment set plan
     * should be rolled back in case of a failure in any of them.
     *
     * @return <code>true</code> if all operations should be rolled back if
     *         any of them fail
     */
    boolean isRollback();

    /**
     * Gets whether the deployment set plan is organized around a shutdown of the server.
     *
     * @return <code>true</code> if the plan will be organized around a shutdown,
     *         <code>false</code> otherwise
     */
    boolean isShutdown();

    /**
     * Gets whether the deployment set plan is organized around
     * a graceful shutdown of the server, where potentially long-running in-process
     * work is given time to complete before shutdown proceeds.
     *
     * @return <code>true</code> if the plan will be organized around a graceful shutdown,
     *         <code>false</code> otherwise
     */
    boolean isGracefulShutdown();

    /**
     * Gets the maximum period, in ms, the deployment set plan is configured to
     * wait for potentially long-running in-process work ito complete before
     * shutdown proceeds.
     *
     * @return the period in ms, or <code>-1</code> if {@link #isGracefulShutdown()}
     *         would return <code>false</code>
     */
    long getGracefulShutdownTimeout();

    /**
     * Gets the configuration of how the {@link #getDeploymentActions() deployment
     * actions} are to be applied to the server groups in the domain. Each
     * {@link ServerGroupDeploymentPlan} in the returned data structure specifies
     * how the actions are to be applied to the servers within a particular server group. The data
     * structure itself is a list of sets of ServerGroupDeploymentPlans. Each
     * set indicates a collection of server groups to which actions can be
     * applied concurrently. Each element in the overall list delineates actions
     * should be applied in series.
     * <p>So, for example, assume we the overall deployment
     * set plan is intended to apply deployments to 3 server groups: <code>A</code>,
     * <code>B</code> and <code>C</code>. Assume elements within curly braces
     * represent and set and elements within brackets represent an item in a list:
     * <ul>
     * <li> <code>[{A,B}],[{C}]</code> would describe a plan to concurrently
     * execute the deployment actions on server groups A and B and then when A
     * and B are complete, continue on to server group C.</li>
     * <li> <code>[{A}],[{B}],[{C}]</code> would describe a plan to
     * execute the deployment actions on server group A, and then when A
     * is complete, continue on to server group B and then to C.</li>
     * <li> <code>[{A,B,C}]</code> would describe a plan to concurrently
     * execute the deployment actions on server groups A and B and C.</li>
     * </ul>
     * </p>
     *
     * @return data structure representing how the {@link #getDeploymentActions() deployment
     * actions} are to be applied to the server groups in the domain.
     */
    List<Set<ServerGroupDeploymentPlan>> getServerGroupDeploymentPlans();

}
