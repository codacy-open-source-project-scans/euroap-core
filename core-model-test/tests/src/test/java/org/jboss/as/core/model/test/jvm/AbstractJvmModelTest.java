/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.core.model.test.jvm;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.KernelServicesBuilder;
import org.jboss.as.core.model.test.ModelInitializer;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.ValueExpression;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class AbstractJvmModelTest extends AbstractCoreModelTest {

    private final TestModelType type;
    private final boolean server;

    protected AbstractJvmModelTest(TestModelType type, boolean server) {
        this.type = type;
        this.server = server;
    }


    @Test
    public void testReadResourceDescription() throws Exception {
        KernelServices kernelServices = createKernelServicesBuilder(type).build();

        //Just make sure we can read it all
        ModelNode op = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        op.get(RECURSIVE).set(true);
        kernelServices.executeForResult(op);
    }

    @Test
    public void testEmptyJvmAdd() throws Exception {
        doEmptyJvmAdd();
    }

    @Test
    public void testWriteType() throws Exception {
        KernelServices kernelServices = doEmptyJvmAdd();
        ModelNode value = new ModelNode("IBM");
        Assert.assertEquals(value, writeTest(kernelServices, "type", value));
    }

    @Test
    public void testWriteAgentLib() throws Exception {
        KernelServices kernelServices = doEmptyJvmAdd();
        ModelNode value = new ModelNode("abc");
        Assert.assertEquals(value, writeTest(kernelServices, "agent-lib", value));
    }

    @Test
    public void testWriteAgentPath() throws Exception {
        KernelServices kernelServices = doEmptyJvmAdd();
        ModelNode value = new ModelNode("abc");
        Assert.assertEquals(value, writeTest(kernelServices, "agent-path", value));
    }

    @Test
    public void testWriteEnvClasspathIgnored() throws Exception {
        KernelServices kernelServices = doEmptyJvmAdd();
        ModelNode value = ModelNode.TRUE;
        Assert.assertEquals(value, writeTest(kernelServices, "env-classpath-ignored", value));
    }

    @Test
    public void testWriteJavaAgent() throws Exception {
        KernelServices kernelServices = doEmptyJvmAdd();
        ModelNode value = new ModelNode("abc");
        Assert.assertEquals(value, writeTest(kernelServices, "java-agent", value));
    }

    @Test
    public void testWriteJavaHome() throws Exception {
        KernelServices kernelServices = doEmptyJvmAdd();
        ModelNode value = new ModelNode("abc");
        Assert.assertEquals(value, writeTest(kernelServices, "java-home", value));
    }

    @Test
    public void testWriteStackSize() throws Exception {
        KernelServices kernelServices = doEmptyJvmAdd();
        ModelNode value = new ModelNode("abc");
        Assert.assertEquals(value, writeTest(kernelServices, "stack-size", value));
    }

    @Test
    public void testWriteHeapSize() throws Exception {
        KernelServices kernelServices = doEmptyJvmAdd();
        ModelNode value = new ModelNode("abc");
        Assert.assertEquals(value, writeTest(kernelServices, "heap-size", value));
    }

    @Test
    public void testWriteMaxHeapSize() throws Exception {
        KernelServices kernelServices = doEmptyJvmAdd();
        ModelNode value = new ModelNode("abc");
        Assert.assertEquals(value, writeTest(kernelServices, "max-heap-size", value));
    }

    @Test
    public void testWritePermGenSize() throws Exception {
        KernelServices kernelServices = doEmptyJvmAdd();
        ModelNode value = new ModelNode("abc");
        Assert.assertEquals(value, writeTest(kernelServices, "permgen-size", value));
    }

    @Test
    public void testWriteMaxPermGenSize() throws Exception {
        KernelServices kernelServices = doEmptyJvmAdd();
        ModelNode value = new ModelNode("abc");
        Assert.assertEquals(value, writeTest(kernelServices, "max-permgen-size", value));
    }

    @Test
    public void testWriteBadType() throws Exception {
        KernelServices kernelServices = doEmptyJvmAdd();
        ModelNode op = createWriteAttributeOperation("type", new ModelNode("XXX"));
        kernelServices.executeForFailure(op);
    }

    @Test
    public void testWriteJvmOptions() throws Exception {
        KernelServices kernelServices = doEmptyJvmAdd();
        ModelNode value = new ModelNode().add("-Xmx100m").add("-Xms30m");
        Assert.assertEquals(value, writeTest(kernelServices, "jvm-options", value));
    }

    @Test
    public void testWriteJvmOptionsWithExpression() throws Exception {
        KernelServices kernelServices = doEmptyJvmAdd();
        String expression = "-Xmx${my.xmx:100}m";
        ModelNode value = new ModelNode().add(expression);
        Assert.assertEquals(new ModelNode().add(new ModelNode().set(new ValueExpression(expression))), writeTest(kernelServices, "jvm-options", value));
    }

    @Test
    public void testWriteEnvironmentVariables() throws Exception {
        KernelServices kernelServices = doEmptyJvmAdd();
        ModelNode value = new ModelNode();
        value.get("ENV1").set("one");
        value.get("ENV2").set("two");
        Assert.assertEquals(value, writeTest(kernelServices, "environment-variables", value));
    }

    @Test
    public void testFullAdd() throws Exception {
        ModelNode typeValue = new ModelNode("IBM");
        ModelNode agentLibValue = new ModelNode("agentLib");
        ModelNode envClasspathIgnored = ModelNode.TRUE;
        ModelNode javaAgentValue = new ModelNode("javaAgent");
        ModelNode javaHomeValue = new ModelNode("javaHome");
        ModelNode stackSizeValue = new ModelNode("stackSize");
        ModelNode heapSizeValue = new ModelNode("heapSize");
        ModelNode maxHeapSizeValue = new ModelNode("maxHeapSize");
        ModelNode permgenSizeValue = new ModelNode("permGenSize");
        ModelNode maxPermSizeValue = new ModelNode("maxPermSize");
        ModelNode jvmOptionsValue = new ModelNode().add("-Xmx100m").add("-Xms30m");
        ModelNode environmentVariablesValue = new ModelNode();
        environmentVariablesValue.get("ENV1").set("one");
        environmentVariablesValue.get("ENV2").set("two");

        ModelNode debugEnabledValue = ModelNode.TRUE;
        ModelNode debugOptionsValue = new ModelNode("debugOptions");

        ModelNode op = createOperation(ADD);
        op.get("type").set(typeValue);
        op.get("agent-lib").set(agentLibValue);
        op.get("env-classpath-ignored").set(envClasspathIgnored);
        op.get("java-agent").set(javaAgentValue);
        op.get("java-home").set(javaHomeValue);
        op.get("stack-size").set(stackSizeValue);
        op.get("heap-size").set(heapSizeValue);
        op.get("max-heap-size").set(maxHeapSizeValue);
        op.get("permgen-size").set(permgenSizeValue);
        op.get("max-permgen-size").set(maxPermSizeValue);
        op.get("jvm-options").set(jvmOptionsValue);
        op.get("environment-variables").set(environmentVariablesValue);
        if (server) {
            op.get("debug-enabled").set(debugEnabledValue);
            op.get("debug-options").set(debugOptionsValue);
        }

        KernelServices kernelServices = createKernelServicesBuilder(type)
                .setBootOperations(Collections.singletonList(op))
                .setModelInitializer(getModelInitializer(), null)
                .build();
        Assert.assertTrue(kernelServices.isSuccessfulBoot());

        ModelNode resource = getJvmResource(kernelServices);

        Assert.assertEquals(typeValue, resource.get("type"));
        Assert.assertEquals(agentLibValue, resource.get("agent-lib"));
        Assert.assertEquals(envClasspathIgnored, resource.get("env-classpath-ignored"));
        Assert.assertEquals(javaAgentValue, resource.get("java-agent"));
        Assert.assertEquals(javaHomeValue, resource.get("java-home"));
        Assert.assertEquals(stackSizeValue, resource.get("stack-size"));
        Assert.assertEquals(heapSizeValue, resource.get("heap-size"));
        Assert.assertEquals(maxHeapSizeValue, resource.get("max-heap-size"));
        Assert.assertEquals(permgenSizeValue, resource.get("permgen-size"));
        Assert.assertEquals(maxPermSizeValue, resource.get("max-permgen-size"));
        Assert.assertEquals(jvmOptionsValue, resource.get("jvm-options"));
        Assert.assertEquals(environmentVariablesValue, resource.get("environment-variables"));
        if (server) {
            Assert.assertEquals(debugEnabledValue, resource.get("debug-enabled"));
            Assert.assertEquals(debugOptionsValue, resource.get("debug-options"));
        }
    }

    @Test
    public void testAddSameJvmOption() throws Exception {
        KernelServices kernelServices = doEmptyJvmAdd();
        kernelServices.executeForResult(createAddJvmOptionOperation("-Xoption"));

        Assert.assertEquals(new ModelNode().add("-Xoption"), getJvmResource(kernelServices).get("jvm-options"));
        kernelServices.executeForFailure(createAddJvmOptionOperation("-Xoption"));
        Assert.assertEquals(new ModelNode().add("-Xoption"), getJvmResource(kernelServices).get("jvm-options"));
    }

    @Test
    public void testAddJvmOptionWithExpression() throws Exception {
        KernelServices kernelServices = doEmptyJvmAdd();
        kernelServices.executeForResult(createAddJvmOptionOperation("-X${myoption:option}"));

        ModelNode result = getJvmResource(kernelServices).get("jvm-options");
        Assert.assertEquals(ModelType.EXPRESSION, result.get(0).getType());
        Assert.assertEquals("-X${myoption:option}", result.get(0).asString());
    }

    @Test
    public void testRemoveJvmOption() throws Exception {
        KernelServices kernelServices = doEmptyJvmAdd();
        kernelServices.executeForResult(createAddJvmOptionOperation("-Xoption1"));
        kernelServices.executeForResult(createAddJvmOptionOperation("-Xoption2"));
        kernelServices.executeForResult(createAddJvmOptionOperation("-Xoption3"));
        ModelNode resource = getJvmResource(kernelServices);
        Assert.assertEquals(new ModelNode().add("-Xoption1").add("-Xoption2").add("-Xoption3"), resource.get("jvm-options"));

        kernelServices.executeForResult(createRemoveJvmOptionOperation("-Xoption2"));
        Assert.assertEquals(new ModelNode().add("-Xoption1").add("-Xoption3"), getJvmResource(kernelServices).get("jvm-options"));
        kernelServices.executeForResult(createRemoveJvmOptionOperation("-Xoption2"));
        Assert.assertEquals(new ModelNode().add("-Xoption1").add("-Xoption3"), getJvmResource(kernelServices).get("jvm-options"));
        kernelServices.executeForResult(createRemoveJvmOptionOperation("-Xoption1"));
        Assert.assertEquals(new ModelNode().add("-Xoption3"), getJvmResource(kernelServices).get("jvm-options"));
        kernelServices.executeForResult(createRemoveJvmOptionOperation("-Xoption3"));
        Assert.assertEquals(new ModelNode().setEmptyList(), getJvmResource(kernelServices).get("jvm-options"));
    }

    protected ModelNode createWriteAttributeOperation(String name, ModelNode value) {
        ModelNode op = createOperation(WRITE_ATTRIBUTE_OPERATION);
        op.get(NAME).set(name);
        op.get(VALUE).set(value);
        return op;
    }

    protected ModelNode createAddJvmOptionOperation(String option) {
        ModelNode op = createOperation("add-jvm-option");
        op.get("jvm-option").set(option);
        return op;
    }

    protected ModelNode createRemoveJvmOptionOperation(String option) {
        ModelNode op = createOperation("remove-jvm-option");
        op.get("jvm-option").set(option);
        return op;
    }

    protected KernelServices doEmptyJvmAdd() throws Exception {
        List<ModelNode> bootOps = new ArrayList<ModelNode>();

        bootOps.add(createOperation(ADD));

        KernelServices kernelServices = createKernelServicesBuilder(type)
                .setBootOperations(bootOps)
                .setModelInitializer(getModelInitializer(), null)
                .build();
        Assert.assertTrue(kernelServices.isSuccessfulBoot());

        ModelNode resource = getJvmResource(kernelServices);
        Assert.assertTrue(resource.keys().size() > 0);
        for (String key : resource.keys()) {
            Assert.assertFalse(resource.hasDefined(key));
        }
        return kernelServices;
    }

    protected ModelInitializer getModelInitializer() {
        return null;
    }


    protected ModelNode writeTest(KernelServices kernelServices, String name, ModelNode value) throws Exception {
        kernelServices.executeForResult(createWriteAttributeOperation(name, value));

        ModelNode resource = getJvmResource(kernelServices);

        Assert.assertTrue(resource.keys().size() > 0);
        for (String key : resource.keys()) {
            boolean isApartFrom = key.equals(name);
            if (!isApartFrom) {
                Assert.assertFalse(resource.hasDefined(key));
            } else {
                Assert.assertTrue(resource.hasDefined(key));
            }
        }

        return resource.get(name);
    }

    protected ModelNode createOperation(String name, ModelNode addr) {
        ModelNode op = new ModelNode();
        op.get(OP).set(name);
        op.get(OP_ADDR).set(addr);
        return op;
    }

    protected ModelNode createOperation(String name) {
        return createOperation(name, getPathAddress("test"));
    }

    protected abstract ModelNode getPathAddress(String jvmName, String... subaddress);

    protected KernelServicesBuilder createKernelServicesBuilder() {
        return createKernelServicesBuilder(type);
    }

    protected ModelNode getJvmResource(KernelServices kernelServices) throws Exception {
        ModelNode model = kernelServices.readWholeModel(true);
        PathAddress addr = PathAddress.pathAddress(getPathAddress("test"));
        return ModelTestUtils.getSubModel(model, addr);
    }

    protected void checkFullJvm(ModelNode full) {
        Assert.assertTrue(full.isDefined());

        Assert.assertEquals("agentLib", full.get("agent-lib").asString());
        Assert.assertEquals("agentPath", full.get("agent-path").asString());
        if (server) {
            Assert.assertEquals(true, full.get("debug-enabled").asBoolean());
            Assert.assertEquals("debugOptions", full.get("debug-options").asString());
        } else {
            Assert.assertFalse(full.get("debug-enabled").isDefined());
            Assert.assertFalse(full.get("debug-options").isDefined());
        }

        Assert.assertEquals(true, full.get("env-classpath-ignored").asBoolean());
        Assert.assertEquals("heapSize", full.get("heap-size").asString());
        Assert.assertEquals("javaAgent", full.get("java-agent").asString());
        Assert.assertEquals("javaHome", full.get("java-home").asString());
        Assert.assertEquals("maxHeapSize", full.get("max-heap-size").asString());
        Assert.assertEquals("maxPermGenSize", full.get("max-permgen-size").asString());
        Assert.assertEquals("stackSize", full.get("stack-size").asString());
        Assert.assertEquals("SUN", full.get("type").asString());

        List<ModelNode> options = full.get("jvm-options").asList();
        Assert.assertEquals(3, options.size());
        Assert.assertEquals("option1", options.get(0).asString());
        Assert.assertEquals("option2", options.get(1).asString());
        Assert.assertEquals("option3", options.get(2).asString());

        List<ModelNode> environment = full.get("environment-variables").asList();
        Assert.assertEquals(2, environment.size());
        Assert.assertEquals("name1", environment.get(0).asProperty().getName());
        Assert.assertEquals("value1", environment.get(0).asProperty().getValue().asString());
        Assert.assertEquals("name2", environment.get(1).asProperty().getName());
        Assert.assertEquals("value2", environment.get(1).asProperty().getValue().asString());

        Assert.assertEquals("command-prefix", full.get("launch-command").asString());
    }

    protected void checkEmptyJvm(ModelNode empty) {
        Assert.assertTrue(empty.isDefined());
        Assert.assertTrue(empty.isDefined());
        Assert.assertTrue(empty.keys().size() > 1);
        for (String key : empty.keys()) {
            Assert.assertFalse(empty.get(key).isDefined());
        }
    }
}
