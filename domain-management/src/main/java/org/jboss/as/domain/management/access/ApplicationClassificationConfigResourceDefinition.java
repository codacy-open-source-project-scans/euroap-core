/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.domain.management.access;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.APPLIES_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CLASSIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.parsing.Attribute.APPLICATION;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.management.AccessConstraintKey;
import org.jboss.as.controller.access.management.AccessConstraintUtilization;
import org.jboss.as.controller.access.management.AccessConstraintUtilizationRegistry;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.as.domain.management._private.DomainManagementResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ApplicationClassificationConfigResourceDefinition extends SimpleResourceDefinition {

    public static PathElement PATH_ELEMENT = PathElement.pathElement(CLASSIFICATION);

    public static SimpleAttributeDefinition DEFAULT_APPLICATION = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.DEFAULT_APPLICATION, ModelType.BOOLEAN, false)
            .setStorageRuntime()
            .setRuntimeServiceNotRequired()
            .build();

    public static SimpleAttributeDefinition CONFIGURED_APPLICATION = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.CONFIGURED_APPLICATION, ModelType.BOOLEAN, true)
            .setXmlName(APPLICATION.getLocalName())
//            .setAllowExpression(true)
            .build();

    ApplicationClassificationConfigResourceDefinition() {
        super(PATH_ELEMENT, DomainManagementResolver.getResolver("core.access-control.constraint.application-classification-config"));
    }

    static ResourceEntry createResource(ApplicationTypeConfig applicationType, String configType, String name, AccessConstraintUtilizationRegistry registry) {
        return new ApplicationTypeConfigResource(PathElement.pathElement(ModelDescriptionConstants.CLASSIFICATION, name),
                applicationType, configType, registry);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(DEFAULT_APPLICATION, ApplicationTypeConfigReadAttributeHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(CONFIGURED_APPLICATION, ApplicationTypeConfigReadAttributeHandler.INSTANCE, ApplicationTypeConfigWriteAttributeHandler.INSTANCE);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new AccessConstraintAppliesToResourceDefinition());
    }

    private static class ApplicationTypeConfigReadAttributeHandler implements OperationStepHandler {

        static final ApplicationTypeConfigReadAttributeHandler INSTANCE = new ApplicationTypeConfigReadAttributeHandler();

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final String attribute = operation.require(NAME).asString();
            final ApplicationTypeConfigResource resource = (ApplicationTypeConfigResource)context.readResource(PathAddress.EMPTY_ADDRESS);
            final ApplicationTypeConfig applicationType = resource.applicationType;
            Boolean result = null;
            if (attribute.equals(DEFAULT_APPLICATION.getName())) {
                result = applicationType.isDefaultApplication();
            } else if (attribute.equals(CONFIGURED_APPLICATION.getName())) {
                result = applicationType.getConfiguredApplication();
            }

            context.getResult();
            if (result != null) {
                context.getResult().set(result);
            }
        }
    }

    private static class ApplicationTypeConfigWriteAttributeHandler implements OperationStepHandler {

        static final ApplicationTypeConfigWriteAttributeHandler INSTANCE = new ApplicationTypeConfigWriteAttributeHandler();

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final ModelNode value = operation.require(VALUE);
            final ApplicationTypeConfigResource resource = (ApplicationTypeConfigResource) context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
            final ModelNode modelNode = resource.getModel();
            // record model values for rollback handler
            final ModelNode configuredApplication = modelNode.get(ModelDescriptionConstants.CONFIGURED_APPLICATION);

            final ApplicationTypeConfig classification = resource.applicationType;
            classification.setConfiguredApplication(readValue(context, value, CONFIGURED_APPLICATION));

            context.completeStep(new OperationContext.RollbackHandler() {

                @Override
                public void handleRollback(OperationContext context, ModelNode operation) {
                    try {
                        classification.setConfiguredApplication(readValue(context, configuredApplication, CONFIGURED_APPLICATION));
                    } catch (OperationFailedException e) {
                        // Should not happen since configured value is retrieved from resource.
                        throw new RuntimeException(e);
                    }

                }
            });
        }

        private Boolean readValue(OperationContext context, ModelNode value, AttributeDefinition definition) throws OperationFailedException {
            if (value.isDefined()) {
                return definition.resolveValue(context, value).asBoolean();
            }
            return null;
        }
    }

    private static class ApplicationTypeConfigResource extends AbstractClassificationResource {
        private final ApplicationTypeConfig applicationType;
        private final String configType;
        private final AccessConstraintUtilizationRegistry registry;

        ApplicationTypeConfigResource(PathElement pathElement, ApplicationTypeConfig classification, String configType, AccessConstraintUtilizationRegistry registry) {
            super(pathElement);
            this.applicationType = classification;
            this.configType = configType;
            this.registry = registry;
        }

        @Override
        public ModelNode getModel() {
            ModelNode model = new ModelNode();
            model.get(CONFIGURED_APPLICATION.getName()).set(getBoolean(applicationType.getConfiguredApplication()));
            return model;
        }

        @Override
        public void writeModel(ModelNode newModel) {
            // Called on a slave host controller during boot
            if (newModel.hasDefined(CONFIGURED_APPLICATION.getName())) {
                boolean b = newModel.get(CONFIGURED_APPLICATION.getName()).asBoolean();
                applicationType.setConfiguredApplication(b);
            }
        }

        @Override
        public boolean isModelDefined() {
            return true;
        }

        private ModelNode getBoolean(Boolean booleanValue) {
            if (booleanValue == null) {
                return new ModelNode();
            }
            return new ModelNode(booleanValue);
        }


        @Override
        public Set<String> getChildTypes() {
            return registry != null ? Collections.singleton(APPLIES_TO) : Collections.<String>emptySet();
        }


        @Override
        ResourceEntry getChildEntry(String type, String name) {
            if (registry != null && APPLIES_TO.equals(type)) {
                Map<PathAddress, AccessConstraintUtilization> utilizations = getAccessConstraintUtilizations();
                for (AccessConstraintUtilization acu : utilizations.values()) {
                    if (name.equals(acu.getPathAddress().toCLIStyleString())) {
                        return AccessConstraintAppliesToResourceDefinition.createResource(acu);
                    }
                }
            }
            return null;
        }

        @Override
        public Set<String> getChildrenNames(String type) {
            if (registry != null && APPLIES_TO.equals(type)) {
                Map<PathAddress, AccessConstraintUtilization> utilizations = getAccessConstraintUtilizations();
                Set<String> result = new LinkedHashSet<String>();
                for (PathAddress pa : utilizations.keySet()) {
                    result.add(pa.toCLIStyleString());
                }
                return result;
            }
            return Collections.emptySet();
        }

        @Override
        public Set<ResourceEntry> getChildren(String childType) {
            if (registry != null && APPLIES_TO.equals(childType)) {
                Map<PathAddress, AccessConstraintUtilization> utilizations = getAccessConstraintUtilizations();
                Set<ResourceEntry> result = new LinkedHashSet<ResourceEntry>();
                for (AccessConstraintUtilization acu : utilizations.values()) {
                    result.add(AccessConstraintAppliesToResourceDefinition.createResource(acu));
                }
                return result;
            }
            return Collections.emptySet();
        }

        private Map<PathAddress, AccessConstraintUtilization> getAccessConstraintUtilizations() {
            boolean core = ModelDescriptionConstants.CORE.equals(configType);
            AccessConstraintKey key =
                    new AccessConstraintKey(ModelDescriptionConstants.APPLICATION_CLASSIFICATION,
                            core, core? null : configType, getPathElement().getValue());
            return registry.getAccessConstraintUtilizations(key);
        }
    }
}
