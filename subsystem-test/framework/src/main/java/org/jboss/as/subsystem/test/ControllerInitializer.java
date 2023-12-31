/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.subsystem.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INET_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.wildfly.common.Assert.checkNotNullParam;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.resource.InterfaceDefinition;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.controller.resources.SystemPropertyResourceDefinition;
import org.jboss.as.server.services.net.SocketBindingGroupResourceDefinition;
import org.jboss.as.server.services.net.SpecifiedInterfaceAddHandler;
import org.jboss.as.server.services.net.SpecifiedInterfaceRemoveHandler;
import org.jboss.dmr.ModelNode;

/**
 * Allows easy initialization of parts of the model that subsystems frequently need
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ControllerInitializer {

    public static final String INTERFACE_NAME = "test-interface";
    public static final String SOCKET_BINDING_GROUP_NAME = "test-socket-binding-group";
    protected volatile String bindAddress = "localhost";
    protected volatile String portOffset;
    protected final Map<String, String> systemProperties = new HashMap<String, String>();
    protected final Map<String, Integer> socketBindings = new HashMap<String, Integer>();
    protected final Map<String, OutboundSocketBinding> outboundSocketBindings = new HashMap<String, OutboundSocketBinding>();
    protected final Map<String, PathInfo> paths = new HashMap<String, PathInfo>();
    private volatile PathManagerService pathManager;
    private volatile TestControllerAccessor testControllerAccessor;

    /**
     * Sets the controller being created. Internal use only.
     *
     * @param testControllerAccessor the controller being created.
     */
    void setTestModelControllerAccessor(TestControllerAccessor testControllerAccessor) {
        this.testControllerAccessor = testControllerAccessor;
    }

    void setPathManger(PathManagerService pathManager) {
        this.pathManager = pathManager;
    }

    /**
     * Adds a system property to the model.
     * This initializes the system property part of the model with the operations to add it.
     *
     * @param key the system property name
     * @param value the system property value
     */
    public void addSystemProperty(String key, String value) {
        systemProperties.put(checkNotNullParam("key", key), checkNotNullParam("value", value));
    }

    /**
     * Sets the bindAddress that will be used for socket bindings.
     * The default is 'localhost'
     *
     * @param address the default bind address
     */
    public void setBindAddress(String address) {
        bindAddress = checkNotNullParam("address", address);
    }

    /**
     * Adds a socket binding to the model.
     * This initializes the interface and socket binding group part of the model with the operations to add it.
     *
     * @param name the socket binding name
     * @param port the socket binding port
     */
    public void addSocketBinding(String name, int port) {
        checkNotNullParam("name", name);
        if (port < 0) {
            throw new IllegalArgumentException("Null port");
        }

        socketBindings.put(name, port);
    }

    /**
     * Adds a remote outbound socket binding to the model.
     *
     * @param name the socket binding name
     * @param destinationHost The destination host
     * @param destinationPort the destination port
     */
    public void addRemoteOutboundSocketBinding(final String name, final String destinationHost, final int destinationPort) {
        checkNotNullParam("name", name);
        if (destinationPort < 0) {
            throw new IllegalArgumentException("Negative destination port");
        }
        if (destinationHost == null || destinationHost.trim().isEmpty()) {
            throw new IllegalArgumentException("Null or empty destination host");
        }

        outboundSocketBindings.put(name, new OutboundSocketBinding(destinationHost, destinationPort, true));
    }

    /**
     * Adds a path to the model
     * This initializes the path part of the model with the operations to add it.
     *
     * @param name the name of the path
     * @param path the absolute path, or the name of a path (if used with {@code relativeTo}
     * @param relativeTo a path relative to {@code path}
     */
    public void addPath(String name, String path, String relativeTo) {
        checkNotNullParam("name", name);
        checkNotNullParam("path", path);

        PathInfo pathInfo = new PathInfo(name, path, relativeTo);
        paths.put(name, pathInfo);
    }

    /**
     * Adds the port offset to the model (optional).
     *
     * @param portOffset the port offset ({@code null} means no offset will be added)
     */
    public void setPortOffset(String portOffset) {
        this.portOffset = portOffset;
    }

    /**
     * Called by framework to set up the model
     *
     * @param rootResource the root model resource
     * @param rootRegistration the root model registry
     */
    protected void initializeModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
        initializeSystemPropertiesModel(rootResource, rootRegistration);
        initializeSocketBindingsModel(rootResource, rootRegistration);
        initializePathsModel(rootResource, rootRegistration);
    }

    /**
     * Called by framework to get the additional boot operations
     *
     * @return the additional boot operations
     */
    protected List<ModelNode> initializeBootOperations(){
        List<ModelNode> ops = new ArrayList<ModelNode>();
        initializeSystemPropertiesOperations(ops);
        initializePathsOperations(ops);
        initializeSocketBindingsOperations(ops);
        initializeRemoteOutboundSocketBindingsOperations(ops);
        return ops;
    }

    /**
     * Initializes the system properties part of the model
     *
     * @param rootResource the root model resource
     * @param rootRegistration the root model registry
     */
    protected void initializeSystemPropertiesModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
        if (systemProperties.size() == 0) {
            return;
        }
        rootResource.getModel().get(SYSTEM_PROPERTY);
        rootRegistration.registerSubModel(SystemPropertyResourceDefinition.createForStandaloneServer(testControllerAccessor.getServerEnvironment()));
    }

    /**
     * Initializes the interface, socket binding group and socket binding part of the model
     *
     * @param rootResource the root model resource
     * @param rootRegistration the root model registry
     */
    protected void initializeSocketBindingsModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
        if (socketBindings.size() == 0 && outboundSocketBindings.isEmpty()) {
            return;
        }

        rootResource.getModel().get(INTERFACE);
        rootResource.getModel().get(SOCKET_BINDING_GROUP);
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        try {
             //= lookup.findConstructor(InterfaceDefinition.class, MethodType.methodType(void.class, InterfaceAddHandler.class, OperationStepHandler.class, boolean.class, boolean.class));
            MethodHandle handle = lookup.unreflectConstructor(InterfaceDefinition.class.getConstructors()[0]);
            InterfaceDefinition id = (InterfaceDefinition)handle.invoke(SpecifiedInterfaceAddHandler.INSTANCE,
                            SpecifiedInterfaceRemoveHandler.INSTANCE, true, false);
            rootRegistration.registerSubModel(id);
        } catch (Throwable e) {
            throw new RuntimeException("Could not register interface definition", e);
        }

        //TODO socket-binding-group currently lives in controller and the child RDs live in server so they currently need passing in from here
        rootRegistration.registerSubModel(SocketBindingGroupResourceDefinition.INSTANCE);
    }


    /**
     * Initializes the interface, socket binding group and socket binding part of the model
     *
     * @param rootResource the root model resource
     * @param rootRegistration the root model registry
     */
    protected void initializePathsModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
        if (paths.size() == 0) {
            return;
        }
        rootResource.getModel().get(PATH);
        PathResourceDefinition def = PathResourceDefinition.createSpecified(pathManager);
        if (rootRegistration.getSubModel(PathAddress.pathAddress(def.getPathElement())) != null) {
            //Older versions of core model tests seem to register this resource, while in newer it does not get registered,
            //so let's remove it here if it exists already
            rootRegistration.unregisterSubModel(def.getPathElement());
        }
        rootRegistration.registerSubModel(def);
    }

    /**
     * Creates the additional add system property operations
     *
     * @param ops the operations list to add our ops to
     */
    protected void initializeSystemPropertiesOperations(List<ModelNode> ops) {
        for (Map.Entry<String, String> prop : systemProperties.entrySet()) {
            ModelNode op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(SYSTEM_PROPERTY, prop.getKey())).toModelNode());
            op.get(VALUE).set(prop.getValue());
            ops.add(op);
        }
    }

    /**
     * Creates the additional add interface, socket binding group and socket binding operations
     *
     * @param ops the operations list to add our ops to
     */
    protected void initializeSocketBindingsOperations(List<ModelNode> ops) {
        if (socketBindings.isEmpty() && outboundSocketBindings.isEmpty()) {
            return;
        }

        //Add the interface


        /*ModelNode criteria = new ModelNode();
        criteria.get(INET_ADDRESS).set(bindAddress);
        ModelNode op = InterfaceAddHandler.getAddInterfaceOperation(
        PathAddress.pathAddress(PathElement.pathElement(INTERFACE, INTERFACE_NAME)).toModelNode(),
        criteria);*/
        ModelNode op = Util.createAddOperation(PathAddress.pathAddress(PathElement.pathElement(INTERFACE, INTERFACE_NAME)));
        op.get(INET_ADDRESS).set(bindAddress);
        ops.add(op);

        //Add the socket binding group
        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, SOCKET_BINDING_GROUP_NAME)).toModelNode());
        op.get(DEFAULT_INTERFACE).set(INTERFACE_NAME);
        if (portOffset != null) {
            op.get(PORT_OFFSET).set(portOffset);
        }
        ops.add(op);


        for (Map.Entry<String, Integer> binding : socketBindings.entrySet()) {
            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, SOCKET_BINDING_GROUP_NAME),
                    PathElement.pathElement(SOCKET_BINDING, binding.getKey())).toModelNode());
            op.get(PORT).set(binding.getValue());
            ops.add(op);
        }
    }

    /**
     * Creates and add to the <code>ops</code> the <code>add</code> operation for the
     * remote outbound socket configurations
     *
     * @param ops the operations list to add our ops to
     */
    protected void initializeRemoteOutboundSocketBindingsOperations(List<ModelNode> ops) {
        if (outboundSocketBindings.size() == 0) {
            return;
        }

        for (Map.Entry<String, OutboundSocketBinding> entry : outboundSocketBindings.entrySet()) {
            final OutboundSocketBinding binding = entry.getValue();
            if (!binding.isRemote()) {
                // skip local outbound socket bindings
                continue;
            }
            final String bindingName = entry.getKey();
            final ModelNode op = new ModelNode();
            op.get(OP).set(ADD);

            final PathAddress address = PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, SOCKET_BINDING_GROUP_NAME),
                    PathElement.pathElement(ModelDescriptionConstants.REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING, bindingName));
            op.get(OP_ADDR).set(address.toModelNode());
            // setup the other parameters for the add operation
            op.get(HOST).set(binding.getDestination());
            op.get(PORT).set(binding.getDestinationPort());
            // add the ADD operation to the operations list
            ops.add(op);
        }
    }

    protected void initializePathsOperations(List<ModelNode> ops) {
        if (paths.size() == 0) {
            return;
        }

        for (PathInfo path : paths.values()) {
            ModelNode op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(PATH, path.getName())).toModelNode());
            op.get(PATH).set(path.getPath());
            if (path.getRelativeTo() != null) {
                op.get(RELATIVE_TO).set(path.getRelativeTo());
            }
            ops.add(op);
        }
    }

    private static class PathInfo {
        private final String name;
        private final String path;
        private final String relativeTo;

        public PathInfo(String name, String path, String relativeTo) {
            this.name = name;
            this.path = path;
            this.relativeTo = relativeTo;
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        public String getRelativeTo() {
            return relativeTo;
        }
    }

    private static class OutboundSocketBinding {
        private final String destination;
        private final int port;
        private final boolean remote;

        OutboundSocketBinding(final String destination, final int port, final boolean remote) {
            this.destination = destination;
            this.port = port;
            this.remote = remote;
        }

        int getDestinationPort() {
            return this.port;
        }

        String getDestination() {
            return this.destination;
        }

        boolean isRemote() {
            return this.remote;
        }
    }

    public interface TestControllerAccessor {
        ServerEnvironment getServerEnvironment();
    }
}
