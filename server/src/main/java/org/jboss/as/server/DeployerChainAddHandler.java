/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.server;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.server.deployment.DeployerChainsService;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.RegisteredDeploymentUnitProcessor;
import org.jboss.as.server.deployment.Services;
import org.jboss.as.server.logging.ServerLogger;
import org.jboss.dmr.ModelNode;

/**
 * @author John Bailey
 */
public class DeployerChainAddHandler implements OperationStepHandler {
    static final String NAME = "add-deployer-chains";
    public static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(NAME, ControllerResolver.getResolver())
            .setPrivateEntry()
            .build();
    public static final DeployerChainAddHandler INSTANCE = new DeployerChainAddHandler();

    public static void addDeploymentProcessor(final String subsystemName, Phase phase, int priority, DeploymentUnitProcessor processor) {
        final EnumMap<Phase, Set<RegisteredDeploymentUnitProcessor>> deployerMap = INSTANCE.deployerMap;
        Set<RegisteredDeploymentUnitProcessor> registeredDeploymentUnitProcessors = deployerMap.get(phase);
        RegisteredDeploymentUnitProcessor registeredDeploymentUnitProcessor = new RegisteredDeploymentUnitProcessor(priority, processor, subsystemName);
        if(registeredDeploymentUnitProcessors.contains(registeredDeploymentUnitProcessor)) {
            throw ServerLogger.ROOT_LOGGER.duplicateDeploymentUnitProcessor(priority, processor.getClass());
        }
        registeredDeploymentUnitProcessors.add(registeredDeploymentUnitProcessor);
    }

    static ModelNode OPERATION = new ModelNode();
    static {
        OPERATION.get(ModelDescriptionConstants.OP).set(NAME);
        OPERATION.get(ADDRESS).setEmptyList();
    }

    // This map is concurrently read by multiple threads but will only
    // be written by a single thread, the boot thread
    private final EnumMap<Phase, Set<RegisteredDeploymentUnitProcessor>> deployerMap;

    private DeployerChainAddHandler() {
        final EnumMap<Phase, Set<RegisteredDeploymentUnitProcessor>> map = new EnumMap<Phase, Set<RegisteredDeploymentUnitProcessor>>(Phase.class);
        for (Phase phase : Phase.values()) {
            map.put(phase, new ConcurrentSkipListSet<RegisteredDeploymentUnitProcessor>());
        }
        this.deployerMap = map;
    }

    /** This is only public so AbstractSubsystemTest can use it; otherwise it would be package-protected. */
    public void clearDeployerMap() {
        for (Set<RegisteredDeploymentUnitProcessor> set : deployerMap.values()) {
            set.clear();
        }
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if(context.isNormalServer()) {

            // Our real work needs to run after all RUNTIME steps that add DUPs have run. ServerService adds
            // this operation at the end of the boot op list, so our MODEL stage step is last, and
            // this RUNTIME step we are about to add should therefore be last as well *at the time
            // we register it*. However, other RUNTIME steps can themselves add new RUNTIME steps that
            // will then come after this one. So we do the same -- add a RUNTIME step that adds another
            // RUNTIME step that does the real work.
            // Theoretically this kind of "predecessor runtime step adds another runtime step, so we have to
            // add one to come later" business could go on forever. But any operation that does that with
            // a DUP-add step is just broken and should just find another way.

            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

                    context.addStep(new FinalRuntimeStepHandler(), OperationContext.Stage.RUNTIME);
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }


    private class FinalRuntimeStepHandler implements OperationStepHandler {

        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

            final EnumMap<Phase, List<RegisteredDeploymentUnitProcessor>> finalDeployers = new EnumMap<Phase, List<RegisteredDeploymentUnitProcessor>>(Phase.class);
            final List<RegisteredDeploymentUnitProcessor> processorList = new ArrayList<RegisteredDeploymentUnitProcessor>(256);
            for (Phase phase : Phase.values()) {
                processorList.clear();
                final Set<RegisteredDeploymentUnitProcessor> processorSet = deployerMap.get(phase);
                for (RegisteredDeploymentUnitProcessor processor : processorSet) {
                    processorList.add(processor);
                }
                finalDeployers.put(phase, new ArrayList<RegisteredDeploymentUnitProcessor>(processorList));
            }
            DeployerChainsService.addService(context.getServiceTarget(), finalDeployers);

            context.completeStep(new OperationContext.RollbackHandler() {
                @Override
                public void handleRollback(OperationContext context, ModelNode operation) {
                    context.removeService(Services.JBOSS_DEPLOYMENT_CHAINS);
                }
            });
        }
    }

}
