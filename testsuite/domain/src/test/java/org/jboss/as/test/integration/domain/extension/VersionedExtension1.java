/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.extension;

import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Version 1 of an extension.
 *
 * @author Emanuel Muckenhuber
 */
public class VersionedExtension1 extends VersionedExtensionCommon {

    private static final PathElement ORIGINAL = PathElement.pathElement("element", "renamed");

    private static final SubsystemInitialization TEST_SUBSYSTEM = new SubsystemInitialization(SUBSYSTEM_NAME, false);

    @Override
    public void initialize(final ExtensionContext context) {

        // Register the test subsystem
        final SubsystemInitialization.RegistrationResult result = TEST_SUBSYSTEM.initializeSubsystem(context, ModelVersion.create(1, 0, 0));

        final ManagementResourceRegistration registration = result.getResourceRegistration();
        // Register an element which is going to get renamed
        registration.registerSubModel(createResourceDefinition(ORIGINAL));
        registration.registerOperationHandler(getOperationDefinition("test"), new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                context.getResult().set(true);
            }
        });

        // No transformers for the first version of the model!
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        TEST_SUBSYSTEM.initializeParsers(context);
    }
}
