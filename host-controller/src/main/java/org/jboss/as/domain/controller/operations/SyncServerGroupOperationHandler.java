/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.domain.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;

import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.Transformers;
import org.jboss.as.domain.controller.operations.deployment.SyncModelParameters;

/**
 * This operation handler is only getting executed on a slave host-controller, synchronizing the model for a
 * single server or server-group, which gets executed when a part of the model is missing. This handler only works on
 * a subset of the domain model and takes the server-group and an optional socket-binding-group parameter into account,
 * filtering out any other information. Additionally this only gets used when the slave is automatically ignoring unused
 * configuration.
 *
 * @author Emanuel Muckenhuber
 */
public class SyncServerGroupOperationHandler extends SyncModelHandlerBase {

    private final String localHostName;
    private final Resource originalModel;
    private final SyncModelParameters parameters;

    public SyncServerGroupOperationHandler(String localHostName, Resource originalDomainModel,
                                           SyncModelParameters parameters) {
        super(parameters);
        this.localHostName = localHostName;
        this.originalModel = originalDomainModel;
        this.parameters = parameters;
    }

    /**
     * For the local model we include both the original as well as the remote model. The diff will automatically remove
     * not used configuration.
     *
     * @param context          the operation context
     * @param remote           the remote model
     * @param remoteExtensions the extension registry
     * @return
     */
    @Override
    Transformers.ResourceIgnoredTransformationRegistry createRegistry(OperationContext context, Resource remote, Set<String> remoteExtensions) {
        final ReadMasterDomainModelUtil.RequiredConfigurationHolder rc = new ReadMasterDomainModelUtil.RequiredConfigurationHolder();

        final PathElement host = PathElement.pathElement(HOST, localHostName);
        final Resource hostModel = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS.append(host));
        final Resource original = this.originalModel;

        // Process the required using the remote model to include content which may not be available locally
        ReadMasterDomainModelUtil.processHostModel(rc, remote, hostModel, parameters.getExtensionRegistry());
        // Process the original
        ReadMasterDomainModelUtil.processHostModel(rc, original, original.getChild(host), parameters.getExtensionRegistry());

        final Transformers.ResourceIgnoredTransformationRegistry delegate = new Transformers.ResourceIgnoredTransformationRegistry() {
            @Override
            public boolean isResourceTransformationIgnored(PathAddress address) {
                return parameters.getIgnoredResourceRegistry().isResourceExcluded(address);
            }
        };
        return ReadMasterDomainModelUtil.createServerIgnoredRegistry(rc, delegate);
    }
}
