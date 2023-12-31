/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.subsystem.test.simple;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.simple.subsystem.SimpleService;
import org.jboss.as.subsystem.test.simple.subsystem.SimpleSubsystemExtension;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SimpleSubsystemTestCase extends AbstractSubsystemTest {

    public SimpleSubsystemTestCase() {
        super(SimpleSubsystemExtension.SUBSYSTEM_NAME, new SimpleSubsystemExtension());

    }

    /**
     * Tests that the xml is parsed into the correct operations
     */
    @Test
    public void testParseSubsystem() throws Exception {
        //Parse the subsystem xml into operations
        String subsystemXml =
                "<subsystem xmlns=\"" + SimpleSubsystemExtension.NAMESPACE + "\">" +
                "</subsystem>";
        List<ModelNode> operations = super.parse(subsystemXml);

        ///Check that we have the expected number of operations
        Assert.assertEquals(1, operations.size());

        //Check that each operation has the correct content
        ModelNode addSubsystem = operations.get(0);
        Assert.assertEquals(ADD, addSubsystem.get(OP).asString());
        PathAddress addr = PathAddress.pathAddress(addSubsystem.get(OP_ADDR));
        Assert.assertEquals(1, addr.size());
        PathElement element = addr.getElement(0);
        Assert.assertEquals(SUBSYSTEM, element.getKey());
        Assert.assertEquals(SimpleSubsystemExtension.SUBSYSTEM_NAME, element.getValue());
    }

    /**
     * Test that the model created from the xml looks as expected, and that the services are installed
     */
    @Test
    public void testInstallIntoController() throws Exception {
        //Parse the subsystem xml and install into the controller
        String subsystemXml =
                "<subsystem xmlns=\"" + SimpleSubsystemExtension.NAMESPACE + "\">" +
                "</subsystem>";
        KernelServices services = createKernelServicesBuilder(null)
                .setSubsystemXml(subsystemXml)
                .build();

        //Read the whole model and make sure it looks as expected
        ModelNode model = services.readWholeModel();
        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(SimpleSubsystemExtension.SUBSYSTEM_NAME));

        //Test that the service was installed
        services.getContainer().getRequiredService(SimpleService.NAME);

        //Check that all the resources were removed
        super.assertRemoveSubsystemResources(services);
        try {
            services.getContainer().getRequiredService(SimpleService.NAME);
            Assert.fail("Should not have found simple service");
        } catch (Exception expected) {
        }
    }

    /**
     * Test that the model created from the xml looks as expected, and that the services are NOT installed
     */
    @Test
    public void testInstallIntoControllerModelOnly() throws Exception {
        //Parse the subsystem xml and install into the controller
        String subsystemXml =
                "<subsystem xmlns=\"" + SimpleSubsystemExtension.NAMESPACE + "\">" +
                "</subsystem>";
        KernelServices services = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(subsystemXml)
                .build();

        //Read the whole model and make sure it looks as expected
        ModelNode model = services.readWholeModel();
        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(SimpleSubsystemExtension.SUBSYSTEM_NAME));

        //Test that the service was not installed
        Assert.assertNull(services.getContainer().getService(SimpleService.NAME));

        //Check that all the resources were removed
        super.assertRemoveSubsystemResources(services);
        try {
            services.getContainer().getRequiredService(SimpleService.NAME);
            Assert.fail("Should not have found simple service");
        } catch (Exception expected) {
        }
    }

    /**
     * Starts a controller with a given subsystem xml and then checks that a second
     * controller started with the xml marshalled from the first one results in the same model
     */
    @Test
    public void testParseAndMarshalModel() throws Exception {
        //Parse the subsystem xml and install into the first controller
        String subsystemXml =
                "<subsystem xmlns=\"" + SimpleSubsystemExtension.NAMESPACE + "\">" +
                "</subsystem>";
        KernelServices servicesA = createKernelServicesBuilder(null)
                .setSubsystemXml(subsystemXml)
                .build();
        //Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        String marshalled = servicesA.getPersistedSubsystemXml();

        //Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = createKernelServicesBuilder(null)
                .setSubsystemXml(marshalled)
                .build();
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }

    /**
     * Starts a controller with the given subsystem xml and then checks that a second
     * controller started with the operations from its describe action results in the same model
     */
    @Test
    public void testDescribeHandler() throws Exception {
        //Parse the subsystem xml and install into the first controller
        String subsystemXml =
                "<subsystem xmlns=\"" + SimpleSubsystemExtension.NAMESPACE + "\">" +
                "</subsystem>";
        KernelServices servicesA = createKernelServicesBuilder(null)
                .setSubsystemXml(subsystemXml)
                .build();
        //Get the model and the describe operations from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        ModelNode describeOp = new ModelNode();
        describeOp.get(OP).set(DESCRIBE);
        describeOp.get(OP_ADDR).set(
                PathAddress.pathAddress(
                        PathElement.pathElement(SUBSYSTEM, SimpleSubsystemExtension.SUBSYSTEM_NAME)).toModelNode());
        List<ModelNode> operations = checkResultAndGetContents(servicesA.executeOperation(describeOp)).asList();


        //Install the describe options from the first controller into a second controller
        KernelServices servicesB = createKernelServicesBuilder(null)
                .setBootOperations(operations)
                .build();
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }

    /**
     * Tests that we can trigger output of the model, i.e. that outputModel() works as it should
     */
    @Test
    public void testOutputModel() throws Exception {
        String subsystemXml =
                "<subsystem xmlns=\"" + SimpleSubsystemExtension.NAMESPACE + "\">" +
                "</subsystem>";

        ModelNode testModel = new ModelNode();
        testModel.get(SUBSYSTEM).get(SimpleSubsystemExtension.SUBSYSTEM_NAME).setEmptyObject();
        String triggered = outputModel(testModel);

        KernelServices services = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(subsystemXml)
                .build();
        //Get the model and the persisted xml from the controller
        services.readWholeModel();
        String marshalled = services.getPersistedSubsystemXml();

        Assert.assertEquals(marshalled, triggered);
        Assert.assertEquals(normalizeXML(marshalled), normalizeXML(triggered));
    }
}
