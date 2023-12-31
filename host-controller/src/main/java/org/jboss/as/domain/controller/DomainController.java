/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.domain.controller;

import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.capability.registry.ImmutableCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.controller.transform.Transformers;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * TODO this interface is now a mishmash of not really related stuff.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface DomainController {

    /**
     * {@link org.jboss.msc.service.ServiceName} under which a DomainController instance should be registered
     * with the service container of a Host Controller that is acting as the domain controller.
     */
    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("domain", "controller");

    /**
     * Gets the domain controller's current running mode.
     *
     * @return  the running mode
     */
    RunningMode getCurrentRunningMode();

    /**
     * Gets the local host controller info.
     *
     * @return the local host info
     */
     LocalHostControllerInfo getLocalHostInfo();

    /**
     * Registers a slave Host Controller with this domain controller.
     *
     *
     * @param hostName the name of the slave host
     * @param handler  handler for communications with the host
     * @param transformers transformation handler for converting resources and operations to forms appropriate for the slave
     * @param remoteConnectionId long identifying this specific connection to the host, or {@code null} if the host did not provide such an id
     * @param registerProxyController {@code true} if a proxy controller should be registered for the host; {@code false}
     *                                             if the host is in --admin-only mode and should not be visible to outside users
     * @throws SlaveRegistrationException  if there is a problem registering the host
     */
    void registerRemoteHost(final String hostName, final ManagementChannelHandler handler, final Transformers transformers,
                            Long remoteConnectionId, boolean registerProxyController) throws SlaveRegistrationException;

    /**
     * Check if a Host Controller is already registered with this domain controller.
     *
     * @param id the name of the host controller
     * @return <code>true</code> if there is such a host controller registered, <code>false</code> otherwise
     */
    boolean isHostRegistered(String id);

    /**
     * Unregisters a previously registered Host Controller.
     *
     * @param id the name of the previously
     *           registered Host Controller
     * @param remoteConnectionId long identifying the specific connection to the host, or {@code null}. If {@code null}
     *                           the host's registration will be removed regardless of any remote connection id
     *                           that was provided at registration. If not {@code null}, the registration will only
     *                           be removed if the currently registered id matches the given id
     * @param cleanUnregistration flag indication whether the host was cleanly unregistered
     */
    void unregisterRemoteHost(final String id, Long remoteConnectionId, boolean cleanUnregistration);

    /**
     * Asynchronously ping the slave host with the given {@code hostName} to validate its connection.
     * @param hostName the name of the slave host
     */
    void pingRemoteHost(String hostName);

    /**
     * Registers a running server in the domain model
     *
     * @param serverControllerClient client the controller can use to communicate with the server.
     */
    void registerRunningServer(final ProxyController serverControllerClient);

    /**
     * Unregisters a running server from the domain model
     *
     * @param serverName the name of the server
     */
    void unregisterRunningServer(String serverName);

    /**
     * Report to the domain controller that a server has been reported as unstable.
     * @param serverName  the name of the server
     */
    default void reportServerInstability(String serverName) {
        // default no-op because I'm tired of writing no-op impls in testsuite classes
    }

    /**
     * Get the operations needed to create the given profile.
     *
     * @param profileName the name of the profile
     *
     * @return the operations
     */
    ModelNode getProfileOperations(String profileName);

    /**
     * Gets the file repository backing this domain controller
     *
     * @return the file repository
     */
    HostFileRepository getLocalFileRepository();

    /**
     * Gets the file repository backing the master domain controller
     *
     * @return the file repository
     *
     * @throws IllegalStateException if the {@link #getLocalHostInfo() local host info}'s
     *          {@link LocalHostControllerInfo#isMasterDomainController()} method would return {@code true}
     *
     */
    HostFileRepository getRemoteFileRepository();

    /**
     * Stops this host controller
     */
    void stopLocalHost();

    /**
     * Stop this host controller with a specific exit code.
     *
     * @param exitCode the exit code passed to the ProcessController
     */
    void stopLocalHost(int exitCode);

    ExtensionRegistry getExtensionRegistry();

    ImmutableCapabilityRegistry getCapabilityRegistry();

    ExpressionResolver getExpressionResolver();

    void initializeMasterDomainRegistry(final ManagementResourceRegistration root,
            final ExtensibleConfigurationPersister configurationPersister, final ContentRepository contentRepository,
            final HostFileRepository fileRepository,
            final ExtensionRegistry extensionRegistry, final PathManagerService pathManager);

    void initializeSlaveDomainRegistry(final ManagementResourceRegistration root,
            final ExtensibleConfigurationPersister configurationPersister, final ContentRepository contentRepository,
            final HostFileRepository fileRepository, final LocalHostControllerInfo hostControllerInfo,
            final ExtensionRegistry extensionRegistry,
            final IgnoredDomainResourceRegistry ignoredDomainResourceRegistry, final PathManagerService pathManager);
}
