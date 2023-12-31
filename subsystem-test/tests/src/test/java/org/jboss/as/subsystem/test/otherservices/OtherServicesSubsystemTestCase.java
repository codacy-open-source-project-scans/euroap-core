/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.subsystem.test.otherservices;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INET_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.io.File;

import org.jboss.as.network.SocketBinding;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.otherservices.subsystem.MyService;
import org.jboss.as.subsystem.test.otherservices.subsystem.OtherService;
import org.jboss.as.subsystem.test.otherservices.subsystem.OtherServicesSubsystemExtension;
import org.jboss.as.subsystem.test.otherservices.subsystem.PathUserService;
import org.jboss.as.subsystem.test.otherservices.subsystem.SocketBindingUserService;
import org.jboss.as.subsystem.test.otherservices.subsystem.SubsystemAddBlank;
import org.jboss.as.subsystem.test.otherservices.subsystem.SubsystemAddWithOtherService;
import org.jboss.as.subsystem.test.otherservices.subsystem.SubsystemAddWithPathUserService;
import org.jboss.as.subsystem.test.otherservices.subsystem.SubsystemAddWithSocketBindingUserService;
import org.jboss.as.subsystem.test.simple.subsystem.SimpleSubsystemExtension;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class OtherServicesSubsystemTestCase extends AbstractSubsystemTest {

    public OtherServicesSubsystemTestCase() {
        super(OtherServicesSubsystemExtension.SUBSYSTEM_NAME, new OtherServicesSubsystemExtension());
    }

    /**
     * Test that other services got added properly
     */
    @Test
    public void testOtherService() throws Exception {
        ((OtherServicesSubsystemExtension)getMainExtension()).setAddHandler(SubsystemAddWithOtherService.INSTANCE);

        //Parse the subsystem xml and install into the controller
        String subsystemXml =
                "<subsystem xmlns=\"" + SimpleSubsystemExtension.NAMESPACE + "\">" +
                "</subsystem>";
        KernelServices services = createKernelServicesBuilder(new ExtraServicesInit())
                .setSubsystemXml(subsystemXml)
                .build();

        //Read the whole model and make sure it looks as expected
        ModelNode model = services.readWholeModel();
        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(OtherServicesSubsystemExtension.SUBSYSTEM_NAME));

        Assert.assertNotNull(services.getContainer().getService(OtherService.NAME));
        Assert.assertNotNull(services.getContainer().getService(MyService.NAME));
        MyService myService = (MyService)services.getContainer().getService(MyService.NAME).getValue();
        Assert.assertNotNull(myService.otherValue.getValue());
    }

    /**
     * Test that system property got added properly
     */
    @Test
    public void testSystemProperty() throws Exception {
        ((OtherServicesSubsystemExtension)getMainExtension()).setAddHandler(SubsystemAddBlank.INSTANCE);

        //Parse the subsystem xml and install into the controller
        String subsystemXml =
                "<subsystem xmlns=\"" + SimpleSubsystemExtension.NAMESPACE + "\">" +
                "</subsystem>";
        KernelServices services = createKernelServicesBuilder(new SystemPropertiesInit())
                .setSubsystemXml(subsystemXml)
                .build();

        //Read the whole model and make sure it looks as expected
        ModelNode model = services.readWholeModel();
        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(OtherServicesSubsystemExtension.SUBSYSTEM_NAME));

        Assert.assertEquals("testing123", model.require(SYSTEM_PROPERTY).require("test123").require(VALUE).asString());
    }


    /**
     * Test that a port offset of 0 works correctly.
     */
    @Test
    public void testPortOffsetZero() throws Exception {
        ((OtherServicesSubsystemExtension)getMainExtension()).setAddHandler(SubsystemAddWithSocketBindingUserService.INSTANCE);

        //Parse the subsystem xml and install into the controller
        String subsystemXml =
                "<subsystem xmlns=\"" + SimpleSubsystemExtension.NAMESPACE + "\">" +
                "</subsystem>";
        KernelServices services = createKernelServicesBuilder(new PortOffsetZeroInit())
                .setSubsystemXml(subsystemXml)
                .build();


        //Read the whole model and make sure it looks as expected
        ModelNode model = services.readWholeModel();
        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(OtherServicesSubsystemExtension.SUBSYSTEM_NAME));

        ModelNode group = model.require(SOCKET_BINDING_GROUP).require(ControllerInitializer.SOCKET_BINDING_GROUP_NAME);
        Assert.assertEquals(8000, group.require(PORT_OFFSET).asInt());

        ModelNode bindings = group.require(SOCKET_BINDING);
        Assert.assertEquals(1, bindings.asList().size());
        Assert.assertEquals(0, group.require(SOCKET_BINDING).require("test2").require(PORT).asInt());

        ServiceController<?> controller = services.getContainer().getService(SocketBindingUserService.NAME);
        Assert.assertNotNull(controller);
        SocketBindingUserService service = (SocketBindingUserService)controller.getValue();
        SocketBinding socketBinding = service.socketBindingValue.getValue();
        Assert.assertEquals(8000, socketBinding.getSocketBindings().getPortOffset());
        Assert.assertFalse("fixed port", socketBinding.isFixedPort());
        Assert.assertEquals(0, socketBinding.getPort());
        // Port 0 must not be affected by port-offset
        Assert.assertEquals(0, socketBinding.getSocketAddress().getPort());
    }

    /**
     * Test that socket binding got added properly
     */
    @Test
    public void testSocketBinding() throws Exception {
        ((OtherServicesSubsystemExtension)getMainExtension()).setAddHandler(SubsystemAddWithSocketBindingUserService.INSTANCE);

        //Parse the subsystem xml and install into the controller
        String subsystemXml =
                "<subsystem xmlns=\"" + SimpleSubsystemExtension.NAMESPACE + "\">" +
                "</subsystem>";
        KernelServices services = createKernelServicesBuilder(new SocketBindingInit())
                .setSubsystemXml(subsystemXml)
                .build();


        //Read the whole model and make sure it looks as expected
        ModelNode model = services.readWholeModel();
        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(OtherServicesSubsystemExtension.SUBSYSTEM_NAME));

        ModelNode iface = model.require(INTERFACE).require(ControllerInitializer.INTERFACE_NAME);
        Assert.assertEquals("127.0.0.1", iface.require(INET_ADDRESS).asString());

        ModelNode group = model.require(SOCKET_BINDING_GROUP).require(ControllerInitializer.SOCKET_BINDING_GROUP_NAME);
        Assert.assertEquals(ControllerInitializer.INTERFACE_NAME, group.require(DEFAULT_INTERFACE).asString());
        Assert.assertEquals(ControllerInitializer.SOCKET_BINDING_GROUP_NAME, group.require(NAME).asString());
        Assert.assertEquals(0, group.require(PORT_OFFSET).asInt());

        ModelNode bindings = group.require(SOCKET_BINDING);
        Assert.assertEquals(3, bindings.asList().size());
        Assert.assertEquals(123, group.require(SOCKET_BINDING).require("test1").require(PORT).asInt());
        Assert.assertEquals(234, group.require(SOCKET_BINDING).require("test2").require(PORT).asInt());
        Assert.assertEquals(345, group.require(SOCKET_BINDING).require("test3").require(PORT).asInt());

        ServiceController<?> controller = services.getContainer().getService(SocketBindingUserService.NAME);
        Assert.assertNotNull(controller);
        SocketBindingUserService service = (SocketBindingUserService)controller.getValue();
        SocketBinding socketBinding = service.socketBindingValue.getValue();
        Assert.assertEquals(234, socketBinding.getPort());
        Assert.assertEquals("127.0.0.1", socketBinding.getAddress().getHostAddress());
    }


    /**
     * Test that paths got added properly
     */
    @Test
    public void testPath() throws Exception {
        ((OtherServicesSubsystemExtension)getMainExtension()).setAddHandler(SubsystemAddWithPathUserService.INSTANCE);

        //Parse the subsystem xml and install into the controller
        String subsystemXml =
                "<subsystem xmlns=\"" + SimpleSubsystemExtension.NAMESPACE + "\">" +
                "</subsystem>";
        KernelServices services = createKernelServicesBuilder(new PathInit())
                .setSubsystemXml(subsystemXml)
                .build();

        //Read the whole model and make sure it looks as expected
        ModelNode model = services.readWholeModel();
        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(OtherServicesSubsystemExtension.SUBSYSTEM_NAME));

        ModelNode path = model.require(PATH);
        Assert.assertEquals(2, path.asList().size());
        Assert.assertEquals("p1", path.require("p1").require("name").asString());
        Assert.assertEquals(new File(".").getAbsolutePath(), path.require("p1").require("path").asString());
        Assert.assertFalse(path.get("p1", "relative-to").isDefined());
        Assert.assertEquals("p2", path.require("p2").require("name").asString());
        Assert.assertEquals("target", path.require("p2").require("path").asString());
        Assert.assertEquals("p1", path.require("p2").require("relative-to").asString());

        ServiceController<?> controller = services.getContainer().getService(PathUserService.NAME);
        Assert.assertNotNull(controller);
        PathUserService service = (PathUserService)controller.getValue();
        Assert.assertEquals(new File(".", "target").getAbsolutePath(), service.pathValue.getValue());
    }

    private static class ExtraServicesInit extends AdditionalInitialization {
        @Override
        protected void addExtraServices(ServiceTarget target) {
            target.addService(OtherService.NAME, new OtherService()).install();
        }
    }

    private static class SystemPropertiesInit extends AdditionalInitialization {
        @Override
        protected void setupController(ControllerInitializer controllerInitializer) {
            controllerInitializer.addSystemProperty("test123", "testing123");
        }
    }

    private static class PortOffsetZeroInit extends AdditionalInitialization {
        @Override
        protected void setupController(ControllerInitializer controllerInitializer) {
            controllerInitializer.setPortOffset("8000");
            controllerInitializer.addSocketBinding("test2", 0);
        }
    }

    private static class SocketBindingInit extends AdditionalInitialization {
        @Override
        protected void setupController(ControllerInitializer controllerInitializer) {
            controllerInitializer.setBindAddress("127.0.0.1");
            controllerInitializer.addSocketBinding("test1", 123);
            controllerInitializer.addSocketBinding("test2", 234);
            controllerInitializer.addSocketBinding("test3", 345);
        }
    }

    private static class PathInit extends AdditionalInitialization {
        @Override
        protected void setupController(ControllerInitializer controllerInitializer) {
            File file = new File(".");
            controllerInitializer.addPath("p1", file.getAbsolutePath(), null);
            controllerInitializer.addPath("p2", "target", "p1");
        }
    }
}
