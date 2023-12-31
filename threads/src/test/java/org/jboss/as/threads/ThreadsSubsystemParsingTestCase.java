/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.threads;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.threads.CommonAttributes.ALLOW_CORE_TIMEOUT;
import static org.jboss.as.threads.CommonAttributes.BLOCKING_BOUNDED_QUEUE_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.BLOCKING_QUEUELESS_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.BOUNDED_QUEUE_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.CORE_THREADS;
import static org.jboss.as.threads.CommonAttributes.GROUP_NAME;
import static org.jboss.as.threads.CommonAttributes.HANDOFF_EXECUTOR;
import static org.jboss.as.threads.CommonAttributes.KEEPALIVE_TIME;
import static org.jboss.as.threads.CommonAttributes.MAX_THREADS;
import static org.jboss.as.threads.CommonAttributes.PRIORITY;
import static org.jboss.as.threads.CommonAttributes.QUEUELESS_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.QUEUE_LENGTH;
import static org.jboss.as.threads.CommonAttributes.SCHEDULED_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.THREADS;
import static org.jboss.as.threads.CommonAttributes.THREAD_FACTORY;
import static org.jboss.as.threads.CommonAttributes.THREAD_NAME_PATTERN;
import static org.jboss.as.threads.CommonAttributes.TIME;
import static org.jboss.as.threads.CommonAttributes.UNBOUNDED_QUEUE_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.UNIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ThreadsSubsystemParsingTestCase extends AbstractSubsystemTest {
    public ThreadsSubsystemParsingTestCase() {
        super(ThreadsExtension.SUBSYSTEM_NAME, new ThreadsExtension());
    }
/*

    static ModelNode profileAddress = new ModelNode();

    static {
        profileAddress.add("profile", "test");
    }
*/

    private KernelServices services;
    private ModelNode model;

  /*  private ServiceContainer container;
    private ModelController controller;

    @Before
    public void setupController() throws InterruptedException {
        container = ServiceContainer.Factory.create("test");
        ServiceTarget target = container.subTarget();
        ModelControllerService svc = new ModelControllerService();
        ServiceBuilder<ModelController> builder = target.addService(ServiceName.of("ModelController"), svc);
        builder.install();
        svc.latch.await(30, TimeUnit.SECONDS);
        controller = svc.getValue();
        ModelNode setup = Util.getEmptyOperation("setup", new ModelNode());
        controller.execute(setup, null, null, null);
    }

    @After
    public void shutdownServiceContainer() {
        if (container != null) {
            container.shutdown();
            try {
                container.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                container = null;
            }
        }
    }*/

    @Test
    public void testGetModelDescription() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(RECURSIVE).set(true);
        operation.get(OPERATIONS).set(true);
        services = createKernelServicesBuilder(createAdditionalInitialization()).build();

        ModelNode result = services.executeForResult(operation);

        ModelNode threadsDescription = result.get(CHILDREN, SUBSYSTEM, MODEL_DESCRIPTION, THREADS);
        assertTrue(threadsDescription.isDefined());

        ModelNode threadFactoryDescription = threadsDescription.get(CHILDREN, THREAD_FACTORY, MODEL_DESCRIPTION, "*");
        assertEquals(ModelType.STRING, threadFactoryDescription.require(ATTRIBUTES).require(NAME).require(TYPE).asType());
        assertEquals(ModelType.STRING, threadFactoryDescription.require(ATTRIBUTES).require(GROUP_NAME).require(TYPE).asType());
        assertEquals(ModelType.STRING, threadFactoryDescription.require(ATTRIBUTES).require(THREAD_NAME_PATTERN).require(TYPE)
                .asType());
        assertEquals(ModelType.INT, threadFactoryDescription.require(ATTRIBUTES).require(PRIORITY).require(TYPE).asType());

        ModelNode blockingBoundedQueueThreadPoolDesc = threadsDescription.get(CHILDREN, BLOCKING_BOUNDED_QUEUE_THREAD_POOL, MODEL_DESCRIPTION, "*");
        assertEquals(ModelType.STRING, blockingBoundedQueueThreadPoolDesc.require(ATTRIBUTES).require(NAME).require(TYPE).asType());
        assertEquals(ModelType.STRING, blockingBoundedQueueThreadPoolDesc.require(ATTRIBUTES).require(THREAD_FACTORY).require(TYPE)
                .asType());

        assertEquals(ModelType.INT, blockingBoundedQueueThreadPoolDesc.require(ATTRIBUTES).require(MAX_THREADS).require(TYPE)
                .asType());
        assertEquals(ModelType.INT, blockingBoundedQueueThreadPoolDesc.require(ATTRIBUTES).require(CORE_THREADS).require(TYPE)
                .asType());
        assertEquals(ModelType.INT, blockingBoundedQueueThreadPoolDesc.require(ATTRIBUTES).require(QUEUE_LENGTH).require(TYPE)
                .asType());
        assertEquals(ModelType.OBJECT, blockingBoundedQueueThreadPoolDesc.require(ATTRIBUTES).require(KEEPALIVE_TIME).require(TYPE)
                .asType());
        assertEquals(ModelType.LONG, blockingBoundedQueueThreadPoolDesc.require(ATTRIBUTES).require(KEEPALIVE_TIME).require(VALUE_TYPE)
                .require(TIME).require(TYPE).asType());
        assertEquals(ModelType.STRING,
                blockingBoundedQueueThreadPoolDesc.require(ATTRIBUTES).require(KEEPALIVE_TIME).require(VALUE_TYPE).require(UNIT)
                        .require(TYPE).asType());
        assertEquals(ModelType.BOOLEAN, blockingBoundedQueueThreadPoolDesc.require(ATTRIBUTES).require(ALLOW_CORE_TIMEOUT)
                .require(TYPE).asType());
        assertFalse(blockingBoundedQueueThreadPoolDesc.require(ATTRIBUTES).has(HANDOFF_EXECUTOR));

        ModelNode boundedQueueThreadPoolDesc = threadsDescription.get(CHILDREN, BOUNDED_QUEUE_THREAD_POOL, MODEL_DESCRIPTION, "*");
        assertEquals(ModelType.STRING, boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(NAME).require(TYPE).asType());
        assertEquals(ModelType.STRING, boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(THREAD_FACTORY).require(TYPE)
                .asType());

        assertEquals(ModelType.INT, boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(MAX_THREADS).require(TYPE)
                .asType());
        assertEquals(ModelType.INT, boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(CORE_THREADS).require(TYPE)
                .asType());
        assertEquals(ModelType.INT, boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(QUEUE_LENGTH).require(TYPE)
                .asType());
        assertEquals(ModelType.OBJECT, boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(KEEPALIVE_TIME).require(TYPE)
                .asType());
        assertEquals(ModelType.LONG, boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(KEEPALIVE_TIME).require(VALUE_TYPE)
                .require(TIME).require(TYPE).asType());
        assertEquals(ModelType.STRING,
                boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(KEEPALIVE_TIME).require(VALUE_TYPE).require(UNIT)
                        .require(TYPE).asType());
        assertEquals(ModelType.BOOLEAN, boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(ALLOW_CORE_TIMEOUT)
                .require(TYPE).asType());
        assertEquals(ModelType.STRING, boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(HANDOFF_EXECUTOR).require(TYPE)
                .asType());

        ModelNode blockingQueueLessThreadPoolDesc = threadsDescription.get(CHILDREN, BLOCKING_QUEUELESS_THREAD_POOL, MODEL_DESCRIPTION, "*");
        assertEquals(ModelType.STRING, blockingQueueLessThreadPoolDesc.require(ATTRIBUTES).require(NAME).require(TYPE).asType());
        assertEquals(ModelType.STRING, blockingQueueLessThreadPoolDesc.require(ATTRIBUTES).require(THREAD_FACTORY).require(TYPE)
                .asType());
        assertEquals(ModelType.INT, blockingQueueLessThreadPoolDesc.require(ATTRIBUTES).require(MAX_THREADS).require(TYPE).asType());
        assertEquals(ModelType.LONG, blockingQueueLessThreadPoolDesc.require(ATTRIBUTES).require(KEEPALIVE_TIME).require(VALUE_TYPE)
                .require(TIME).require(TYPE).asType());
        assertEquals(ModelType.STRING,
                blockingQueueLessThreadPoolDesc.require(ATTRIBUTES).require(KEEPALIVE_TIME).require(VALUE_TYPE).require(UNIT)
                        .require(TYPE).asType());
        assertFalse(blockingQueueLessThreadPoolDesc.require(ATTRIBUTES).has(HANDOFF_EXECUTOR));

        ModelNode queueLessThreadPoolDesc = threadsDescription.get(CHILDREN, QUEUELESS_THREAD_POOL, MODEL_DESCRIPTION, "*");
        assertEquals(ModelType.STRING, queueLessThreadPoolDesc.require(ATTRIBUTES).require(NAME).require(TYPE).asType());
        assertEquals(ModelType.STRING, queueLessThreadPoolDesc.require(ATTRIBUTES).require(THREAD_FACTORY).require(TYPE)
                .asType());
        assertEquals(ModelType.INT, queueLessThreadPoolDesc.require(ATTRIBUTES).require(MAX_THREADS).require(TYPE).asType());
        assertEquals(ModelType.LONG, queueLessThreadPoolDesc.require(ATTRIBUTES).require(KEEPALIVE_TIME).require(VALUE_TYPE)
                .require(TIME).require(TYPE).asType());
        assertEquals(ModelType.STRING,
                queueLessThreadPoolDesc.require(ATTRIBUTES).require(KEEPALIVE_TIME).require(VALUE_TYPE).require(UNIT)
                        .require(TYPE).asType());
        assertEquals(ModelType.STRING, queueLessThreadPoolDesc.require(ATTRIBUTES).require(HANDOFF_EXECUTOR).require(TYPE)
                .asType());

        ModelNode scheduledThreadPoolDesc = threadsDescription.get(CHILDREN, SCHEDULED_THREAD_POOL, MODEL_DESCRIPTION, "*");
        assertEquals(ModelType.STRING, scheduledThreadPoolDesc.require(ATTRIBUTES).require(NAME).require(TYPE).asType());
        assertEquals(ModelType.STRING, scheduledThreadPoolDesc.require(ATTRIBUTES).require(THREAD_FACTORY).require(TYPE)
                .asType());
        assertEquals(ModelType.INT, scheduledThreadPoolDesc.require(ATTRIBUTES).require(MAX_THREADS).require(TYPE).asType());
        assertEquals(ModelType.LONG, scheduledThreadPoolDesc.require(ATTRIBUTES).require(KEEPALIVE_TIME).require(VALUE_TYPE)
                .require(TIME).require(TYPE).asType());
        assertEquals(ModelType.STRING,
                scheduledThreadPoolDesc.require(ATTRIBUTES).require(KEEPALIVE_TIME).require(VALUE_TYPE).require(UNIT)
                        .require(TYPE).asType());

        ModelNode unboundedThreadPoolDesc = threadsDescription.get(CHILDREN, UNBOUNDED_QUEUE_THREAD_POOL, MODEL_DESCRIPTION,
                "*");
        assertEquals(ModelType.STRING, unboundedThreadPoolDesc.require(ATTRIBUTES).require(NAME).require(TYPE).asType());
        assertEquals(ModelType.STRING, unboundedThreadPoolDesc.require(ATTRIBUTES).require(THREAD_FACTORY).require(TYPE)
                .asType());
        assertEquals(ModelType.INT, unboundedThreadPoolDesc.require(ATTRIBUTES).require(MAX_THREADS).require(TYPE).asType());
        assertEquals(ModelType.LONG, unboundedThreadPoolDesc.require(ATTRIBUTES).require(KEEPALIVE_TIME).require(VALUE_TYPE)
                .require(TIME).require(TYPE).asType());
        assertEquals(ModelType.STRING,
                unboundedThreadPoolDesc.require(ATTRIBUTES).require(KEEPALIVE_TIME).require(VALUE_TYPE).require(UNIT)
                        .require(TYPE).asType());

    }

    @Test
    public void testSimpleThreadFactory() throws Exception {
        List<ModelNode> updates = createSubSystem("<thread-factory name=\"test-factory\"/>");
        assertEquals(2, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadFactory = subsystem.require("thread-factory");
        assertEquals(1, threadFactory.keys().size());
    }

    @Test
    public void testSimpleThreadFactoryInvalidPriorityValue() throws Exception {
        try {
            createSubSystem("<thread-factory name=\"test-factory\" priority=\"12\"/>");
            fail("Expected failure for invalid priority");
        } catch (XMLStreamException e) {
        }
    }

    @Test
    public void testFullThreadFactory() throws Exception {
        List<ModelNode> updates = createSubSystem("<thread-factory name=\"test-factory\"" + "   group-name=\"test-group\""
                + "   thread-name-pattern=\"test-pattern\"" + "   priority=\"5\"/>");

        executeForResult(updates.get(0));
        executeForResult(updates.get(1));

        checkFullTreadFactory();
    }

    private void checkFullTreadFactory() {

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadFactory = subsystem.require("thread-factory");
        assertEquals(1, threadFactory.keys().size());
        assertEquals("test-group", threadFactory.require("test-factory").require("group-name").asString());
        assertEquals("test-pattern", threadFactory.require("test-factory").require("thread-name-pattern").asString());
        assertEquals(5, threadFactory.require("test-factory").require("priority").asInt());
    }

    @Test
    public void testSeveralThreadFactories() throws Exception {
        List<ModelNode> updates = createSubSystem("<thread-factory name=\"test-factory\" group-name=\"A\"/>"
                + "<thread-factory name=\"test-factory1\" group-name=\"B\"/>");
        assertEquals(3, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadFactory = subsystem.require("thread-factory");
        assertEquals(2, threadFactory.keys().size());
        assertEquals("A", threadFactory.require("test-factory").require("group-name").asString());
        assertEquals("B", threadFactory.require("test-factory1").require("group-name").asString());
    }

    @Test
    public void testSimpleUnboundedQueueThreadPool() throws Exception {
        List<ModelNode> updates = createSubSystem("<unbounded-queue-thread-pool name=\"test-pool\"><max-threads count=\"1\"/></unbounded-queue-thread-pool>");
        assertEquals(2, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("unbounded-queue-thread-pool");
        assertEquals(1, threadPool.keys().size());
    }

    @Test
    public void testSimpleUnboundedQueueThreadPool1_0() throws Exception {
        List<ModelNode> updates = createSubSystem("<unbounded-queue-thread-pool name=\"test-pool\">"
                + "   <max-threads count=\"1\" per-cpu=\"2\"/>" + "</unbounded-queue-thread-pool>", Namespace.THREADS_1_0);
        assertEquals(2, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("unbounded-queue-thread-pool");
        assertEquals(1, threadPool.keys().size());
    }

    @Test
    public void testFullUnboundedQueueThreadPool() throws Exception {
        List<ModelNode> updates = createSubSystem(
                "<thread-factory name=\"test-factory\"/>" +
                        "<unbounded-queue-thread-pool name=\"test-pool\">" +
                        "   <max-threads count=\"100\"/>" +
                        "   <keepalive-time time=\"1000\" unit=\"MILLISECONDS\"/>" +
                        "   <thread-factory name=\"test-factory\"/>" +
                        "</unbounded-queue-thread-pool>");

        executeForResult(updates.get(0));
        executeForResult(updates.get(1));
        executeForResult(updates.get(2));

        checkFullUnboundedThreadPool();
    }

    @Test
    public void testFullUnboundedQueueThreadPool1_0() throws Exception {
        List<ModelNode> updates = createSubSystem(
                "<thread-factory name=\"test-factory\"/>" +
                        "<unbounded-queue-thread-pool name=\"test-pool\">" +
                        "   <max-threads count=\"100\" per-cpu=\"0\"/>" +
                        "   <keepalive-time time=\"1000\" unit=\"MILLISECONDS\"/>" +
                        "   <thread-factory name=\"test-factory\"/>" +
                        "   <properties>" +
                        "      <property name=\"propA\" value=\"valueA\"/>" +
                        "      <property name=\"propB\" value=\"valueB\"/>" +
                        "   </properties>" +
                        "</unbounded-queue-thread-pool>", Namespace.THREADS_1_0);

        executeForResult(updates.get(0));
        executeForResult(updates.get(1));
        executeForResult(updates.get(2));

        checkFullUnboundedThreadPool();
    }

    private void checkFullUnboundedThreadPool() throws Exception {
        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("unbounded-queue-thread-pool");
        assertEquals(1, threadPool.keys().size());
        assertEquals(100, threadPool.require("test-pool").require(MAX_THREADS).asInt());
        assertEquals(1000L, threadPool.require("test-pool").require(KEEPALIVE_TIME).require(TIME).asLong());
        assertEquals("MILLISECONDS", threadPool.require("test-pool").require(KEEPALIVE_TIME).require(UNIT).asString());
    }

    @Test
    public void testSeveralUnboundedQueueThreadPools() throws Exception {
        List<ModelNode> updates = createSubSystem("<unbounded-queue-thread-pool name=\"test-poolA\"><max-threads count=\"1\"/></unbounded-queue-thread-pool>"
                + "<unbounded-queue-thread-pool name=\"test-poolB\"><max-threads count=\"2\"/></unbounded-queue-thread-pool>");
        assertEquals(3, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadFactory = subsystem.require("unbounded-queue-thread-pool");
        assertEquals(2, threadFactory.keys().size());
    }

    @Test
    public void testSeveralUnboundedQueueThreadPools1_0() throws Exception {
        List<ModelNode> updates = createSubSystem("<unbounded-queue-thread-pool name=\"test-poolA\">"
                + "   <max-threads count=\"1\" per-cpu=\"2\"/>" + "</unbounded-queue-thread-pool>"
                + "<unbounded-queue-thread-pool name=\"test-poolB\">" + "   <max-threads count=\"1\" per-cpu=\"2\"/>"
                + "</unbounded-queue-thread-pool>", Namespace.THREADS_1_0);
        assertEquals(3, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadFactory = subsystem.require("unbounded-queue-thread-pool");
        assertEquals(2, threadFactory.keys().size());
    }

    @Test
    public void testSimpleScheduledThreadPool() throws Exception {
        List<ModelNode> updates = createSubSystem("<scheduled-thread-pool name=\"test-pool\"><max-threads count=\"1\"/></scheduled-thread-pool>");
        assertEquals(2, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("scheduled-thread-pool");
        assertEquals(1, threadPool.keys().size());
    }

    @Test
    public void testSimpleScheduledThreadPool1_0() throws Exception {
        List<ModelNode> updates = createSubSystem("<scheduled-thread-pool name=\"test-pool\">"
                + "   <max-threads count=\"1\" per-cpu=\"2\"/>" + "</scheduled-thread-pool>", Namespace.THREADS_1_0);
        assertEquals(2, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("scheduled-thread-pool");
        assertEquals(1, threadPool.keys().size());
    }

    @Test
    public void testFullScheduledThreadPool() throws Exception {
        List<ModelNode> updates = createSubSystem(
                "<thread-factory name=\"test-factory\"/>" +
                        "<scheduled-thread-pool name=\"test-pool\">" +
                        "   <max-threads count=\"100\"/>" +
                        "   <keepalive-time time=\"1000\" unit=\"MILLISECONDS\"/>" +
                        "   <thread-factory name=\"test-factory\"/>" +
                        "</scheduled-thread-pool>");

        executeForResult(updates.get(0));
        executeForResult(updates.get(1));
        executeForResult(updates.get(2));

        checkFullScheduledThreadPool();
    }

    @Test
    public void testFullScheduledThreadPool1_0() throws Exception {
        List<ModelNode> updates = createSubSystem(
                "<thread-factory name=\"test-factory\"/>" +
                        "<scheduled-thread-pool name=\"test-pool\">" +
                        "   <max-threads count=\"100\" per-cpu=\"0\"/>" +
                        "   <keepalive-time time=\"1000\" unit=\"MILLISECONDS\"/>" +
                        "   <thread-factory name=\"test-factory\"/>" +
                        "   <properties>" +
                        "      <property name=\"propA\" value=\"valueA\"/>" +
                        "      <property name=\"propB\" value=\"valueB\"/>" +
                        "   </properties>" +
                        "</scheduled-thread-pool>", Namespace.THREADS_1_0);

        executeForResult(updates.get(0));
        executeForResult(updates.get(1));
        executeForResult(updates.get(2));

        checkFullScheduledThreadPool();
    }

    private void checkFullScheduledThreadPool() throws Exception {
        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("scheduled-thread-pool");
        assertEquals(1, threadPool.keys().size());
        assertEquals(100, threadPool.require("test-pool").require(MAX_THREADS).asInt());
        assertEquals(1000L, threadPool.require("test-pool").require(KEEPALIVE_TIME).get(TIME).asLong());
        assertEquals("MILLISECONDS", threadPool.require("test-pool").require(KEEPALIVE_TIME).get(UNIT).asString());
    }

    @Test
    public void testSeveralScheduledThreadPools() throws Exception {
        List<ModelNode> updates = createSubSystem("<scheduled-thread-pool name=\"test-poolA\"><max-threads count=\"1\"/></scheduled-thread-pool>"
                + "<scheduled-thread-pool name=\"test-poolB\"><max-threads count=\"1\"/></scheduled-thread-pool>");
        assertEquals(3, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadFactory = subsystem.require("scheduled-thread-pool");
        assertEquals(2, threadFactory.keys().size());
    }

    @Test
    public void testSeveralScheduledThreadPools1_0() throws Exception {
        List<ModelNode> updates = createSubSystem("<scheduled-thread-pool name=\"test-poolA\">"
                + "   <max-threads count=\"1\" per-cpu=\"2\"/>" + "</scheduled-thread-pool>"
                + "<scheduled-thread-pool name=\"test-poolB\">" + "   <max-threads count=\"1\" per-cpu=\"2\"/>"
                + "</scheduled-thread-pool>", Namespace.THREADS_1_0);
        assertEquals(3, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadFactory = subsystem.require("scheduled-thread-pool");
        assertEquals(2, threadFactory.keys().size());
    }

    @Test
    public void testSimpleQueuelessThreadPool() throws Exception {
        List<ModelNode> updates = createSubSystem("<queueless-thread-pool name=\"test-pool\"><max-threads count=\"1\"/></queueless-thread-pool>");
        assertEquals(2, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("queueless-thread-pool");
        assertEquals(1, threadPool.keys().size());
    }

    @Test
    public void testSimpleQueuelessThreadPool1_0() throws Exception {
        List<ModelNode> updates = createSubSystem("<queueless-thread-pool name=\"test-pool\">"
                + "   <max-threads count=\"1\" per-cpu=\"2\"/>" + "</queueless-thread-pool>",
                Namespace.THREADS_1_0);
        assertEquals(2, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("queueless-thread-pool");
        assertEquals(1, threadPool.keys().size());
    }

    @Test
    public void testFullQueuelessThreadPool() throws Exception {
        List<ModelNode> updates = createSubSystem(
                "<thread-factory name=\"test-factory\"/>" +
                        "<queueless-thread-pool name=\"other\"><max-threads count=\"1\"/></queueless-thread-pool>" +
                        "<queueless-thread-pool name=\"test-pool\">" +
                        "   <max-threads count=\"100\"/>" +
                        "   <keepalive-time time=\"1000\" unit=\"MILLISECONDS\"/>" +
                        "   <thread-factory name=\"test-factory\"/>" +
                        "   <handoff-executor name=\"other\"/>" +
                        "</queueless-thread-pool>");

        executeForResult(updates.get(0));
        executeForResult(updates.get(1));
        executeForResult(updates.get(2));
        executeForResult(updates.get(3));

        checkFullQueuelessThreadPool();

    }

    @Test
    public void testFullQueuelessThreadPool1_0() throws Exception {
        List<ModelNode> updates = createSubSystem(
                "<thread-factory name=\"test-factory\"/>" +
                        "<queueless-thread-pool name=\"other\"><max-threads count=\"1\"/></queueless-thread-pool>" +
                        "<queueless-thread-pool name=\"test-pool\">" +
                        "   <max-threads count=\"100\"/>" +
                        "   <keepalive-time time=\"1000\" unit=\"MILLISECONDS\"/>" +
                        "   <thread-factory name=\"test-factory\"/>" +
                        "   <handoff-executor name=\"other\"/>" +
                        "   <properties>" +
                        "      <property name=\"propA\" value=\"valueA\"/>" +
                        "      <property name=\"propB\" value=\"valueB\"/>" +
                        "   </properties>" +
                        "</queueless-thread-pool>", Namespace.THREADS_1_0);

        executeForResult(updates.get(0));
        executeForResult(updates.get(1));
        executeForResult(updates.get(2));
        executeForResult(updates.get(3));

        checkFullQueuelessThreadPool();

    }

    private void checkFullQueuelessThreadPool() throws Exception {
        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("queueless-thread-pool");
        assertEquals(2, threadPool.keys().size());
        assertEquals(100, threadPool.require("test-pool").require(MAX_THREADS).asInt());
        assertEquals(1000L, threadPool.require("test-pool").require(KEEPALIVE_TIME).require(TIME).asLong());
        assertEquals("MILLISECONDS", threadPool.require("test-pool").require(KEEPALIVE_TIME).require(UNIT).asString());
        assertEquals("other", threadPool.require("test-pool").require("handoff-executor").asString());
    }

    @Test
    public void testSeveralQueuelessThreadPools() throws Exception {
        List<ModelNode> updates = createSubSystem("<queueless-thread-pool name=\"test-poolA\"><max-threads count=\"1\"/></queueless-thread-pool>"
                + "<queueless-thread-pool name=\"test-poolB\"><max-threads count=\"1\"/></queueless-thread-pool>");
        assertEquals(3, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadFactory = subsystem.require("queueless-thread-pool");
        assertEquals(2, threadFactory.keys().size());
    }

    @Test
    public void testSimpleBlockingQueuelessThreadPool() throws Exception {
        List<ModelNode> updates = createSubSystem("<blocking-queueless-thread-pool name=\"test-pool\"><max-threads count=\"1\"/></blocking-queueless-thread-pool>");
        assertEquals(2, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("blocking-queueless-thread-pool");
        assertEquals(1, threadPool.keys().size());
    }

    @Test
    public void testSimpleBlockingQueuelessThreadPool1_0() throws Exception {
        List<ModelNode> updates = createSubSystem("<queueless-thread-pool name=\"test-pool\" blocking=\"true\">"
                + "   <max-threads count=\"1\" per-cpu=\"2\"/>" + "</queueless-thread-pool>",
                Namespace.THREADS_1_0);
        assertEquals(2, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("blocking-queueless-thread-pool");
        assertEquals(1, threadPool.keys().size());
    }

    @Test
    public void testFullBlockingQueuelessThreadPool() throws Exception {
        List<ModelNode> updates = createSubSystem(
                "<thread-factory name=\"test-factory\"/>" +
                        "<blocking-queueless-thread-pool name=\"test-pool\">" +
                        "   <max-threads count=\"100\"/>" +
                        "   <keepalive-time time=\"1000\" unit=\"MILLISECONDS\"/>" +
                        "   <thread-factory name=\"test-factory\"/>" +
                        "</blocking-queueless-thread-pool>");

        executeForResult(updates.get(0));
        executeForResult(updates.get(1));
        executeForResult(updates.get(2));

        checkFullBlockingQueuelessThreadPool();

    }

    @Test
    public void testFullBlockingQueuelessThreadPool1_0() throws Exception {
        List<ModelNode> updates = createSubSystem(
                "<thread-factory name=\"test-factory\"/>" +
                        "<queueless-thread-pool name=\"test-pool\" blocking=\"true\">" +
                        "   <max-threads count=\"100\"/>" +
                        "   <keepalive-time time=\"1000\" unit=\"MILLISECONDS\"/>" +
                        "   <thread-factory name=\"test-factory\"/>" +
                        "   <handoff-executor name=\"other\"/>" +
                        "   <properties>" +
                        "      <property name=\"propA\" value=\"valueA\"/>" +
                        "      <property name=\"propB\" value=\"valueB\"/>" +
                        "   </properties>" +
                        "</queueless-thread-pool>", Namespace.THREADS_1_0);

        executeForResult(updates.get(0));
        executeForResult(updates.get(1));
        executeForResult(updates.get(2));

        checkFullBlockingQueuelessThreadPool();

    }

    private void checkFullBlockingQueuelessThreadPool() throws Exception {
        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("blocking-queueless-thread-pool");
        assertEquals(1, threadPool.keys().size());
        assertEquals(100, threadPool.require("test-pool").require(MAX_THREADS).asInt());
        assertEquals(1000L, threadPool.require("test-pool").require(KEEPALIVE_TIME).require(TIME).asLong());
        assertEquals("MILLISECONDS", threadPool.require("test-pool").require(KEEPALIVE_TIME).require(UNIT).asString());
        assertFalse(threadPool.has("handoff-executor"));
    }

    @Test
    public void testSeveralBlockingQueuelessThreadPools() throws Exception {
        List<ModelNode> updates = createSubSystem("<blocking-queueless-thread-pool name=\"test-poolA\">" +
                "<max-threads count=\"1\"/></blocking-queueless-thread-pool>"
                + "<blocking-queueless-thread-pool name=\"test-poolB\"><max-threads count=\"1\"/></blocking-queueless-thread-pool>");
        assertEquals(3, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadFactory = subsystem.require("blocking-queueless-thread-pool");
        assertEquals(2, threadFactory.keys().size());
    }

    @Test
    public void testSeveralQueuelessThreadPools1_0() throws Exception {
        List<ModelNode> updates = createSubSystem("<queueless-thread-pool name=\"test-poolA\">"
                + "   <max-threads count=\"1\" per-cpu=\"2\"/>" + "</queueless-thread-pool>"
                + "<queueless-thread-pool name=\"test-poolB\">" + "   <max-threads count=\"1\" per-cpu=\"2\"/>"
                + "</queueless-thread-pool>", Namespace.THREADS_1_0);
        assertEquals(3, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadFactory = subsystem.require("queueless-thread-pool");
        assertEquals(2, threadFactory.keys().size());
    }

    @Test
    public void testSimpleBoundedQueueThreadPool() throws Exception {
        List<ModelNode> updates = createSubSystem("<bounded-queue-thread-pool name=\"test-pool\">" +
                "<max-threads count=\"1\"/><queue-length count=\"1\"/></bounded-queue-thread-pool>");
        assertEquals(2, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("bounded-queue-thread-pool");
        assertEquals(1, threadPool.keys().size());
    }

    @Test
    public void testSimpleBoundedQueueThreadPool1_0() throws Exception {
        List<ModelNode> updates = createSubSystem("<bounded-queue-thread-pool name=\"test-pool\">"
                + "   <max-threads count=\"1\" per-cpu=\"0\"/>" + "   <queue-length count=\"1\"/>"
                + "</bounded-queue-thread-pool>", Namespace.THREADS_1_0);
        assertEquals(2, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("bounded-queue-thread-pool");
        assertEquals(1, threadPool.keys().size());
    }

    @Test
    public void testFullBoundedQueueThreadPool() throws Exception {
        List<ModelNode> updates = createSubSystem(
                "<thread-factory name=\"test-factory\"/>" +
                        "<queueless-thread-pool name=\"other\"><max-threads count=\"1\"/></queueless-thread-pool>" +
                        "<bounded-queue-thread-pool name=\"test-pool\" allow-core-timeout=\"true\">" +
                        "   <core-threads count=\"200\"/>" +
                        "   <max-threads count=\"100\"/>" +
                        "   <queue-length count=\"300\"/>" +
                        "   <keepalive-time time=\"1000\" unit=\"MILLISECONDS\"/>" +
                        "   <thread-factory name=\"test-factory\"/>" +
                        "   <handoff-executor name=\"other\"/>" +
                        "</bounded-queue-thread-pool>");

        executeForResult(updates.get(0));
        executeForResult(updates.get(1));
        executeForResult(updates.get(2));
        executeForResult(updates.get(3));

        checkFullBoundedQueueThreadPool();
    }

    @Test
    public void testFullBoundedQueueThreadPool1_0() throws Exception {
        List<ModelNode> updates = createSubSystem(
                "<thread-factory name=\"test-factory\"/>" +
                        "<queueless-thread-pool name=\"other\"><max-threads count=\"1\"/></queueless-thread-pool>" +
                        "<bounded-queue-thread-pool name=\"test-pool\" allow-core-timeout=\"true\">" +
                        "   <core-threads count=\"200\"/>" +
                        "   <max-threads count=\"100\" per-cpu=\"0\"/>" +
                        "   <queue-length count=\"300\"/>" +
                        "   <keepalive-time time=\"1000\" unit=\"MILLISECONDS\"/>" +
                        "   <thread-factory name=\"test-factory\"/>" +
                        "   <handoff-executor name=\"other\"/>" +
                        "   <properties>" +
                        "      <property name=\"propA\" value=\"valueA\"/>" +
                        "      <property name=\"propB\" value=\"valueB\"/>" +
                        "   </properties>" +
                        "</bounded-queue-thread-pool>", Namespace.THREADS_1_0);

        executeForResult(updates.get(0));
        executeForResult(updates.get(1));
        executeForResult(updates.get(2));
        executeForResult(updates.get(3));

        checkFullBoundedQueueThreadPool();
    }

    private void checkFullBoundedQueueThreadPool() throws Exception {
        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("bounded-queue-thread-pool");
        assertEquals(1, threadPool.keys().size());
        assertTrue(threadPool.require("test-pool").require("allow-core-timeout").asBoolean());
        assertEquals(200, threadPool.require("test-pool").require(CORE_THREADS).asInt());
        assertEquals(300, threadPool.require("test-pool").require(QUEUE_LENGTH).asInt());
        assertEquals(100, threadPool.require("test-pool").require(MAX_THREADS).asInt());
        assertEquals(1000L, threadPool.require("test-pool").require(KEEPALIVE_TIME).require(TIME).asLong());
        assertEquals("MILLISECONDS", threadPool.require("test-pool").require(KEEPALIVE_TIME).require(UNIT).asString());
        assertEquals("other", threadPool.require("test-pool").require("handoff-executor").asString());
    }

    @Test
    public void testSeveralBoundedQueueThreadPools() throws Exception {
        List<ModelNode> updates = createSubSystem("<bounded-queue-thread-pool name=\"test-poolA\"><max-threads count=\"1\"/><queue-length count=\"1\"/></bounded-queue-thread-pool>"
                + "<bounded-queue-thread-pool name=\"test-poolB\"><max-threads count=\"1\"/><queue-length count=\"1\"/></bounded-queue-thread-pool>");
        assertEquals(3, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadFactory = subsystem.require("bounded-queue-thread-pool");
        assertEquals(2, threadFactory.keys().size());
    }

    @Test
    public void testSeveralBoundedQueueThreadPools1_0() throws Exception {
        List<ModelNode> updates = createSubSystem("<bounded-queue-thread-pool name=\"test-poolA\">"
                + "   <max-threads count=\"1\" per-cpu=\"2\"/>" + "   <queue-length count=\"1\" per-cpu=\"2\"/>"
                + "</bounded-queue-thread-pool>" + "<bounded-queue-thread-pool name=\"test-poolB\">"
                + "   <max-threads count=\"1\" per-cpu=\"2\"/>" + "   <queue-length count=\"1\" per-cpu=\"2\"/>"
                + "</bounded-queue-thread-pool>", Namespace.THREADS_1_0);
        assertEquals(3, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadFactory = subsystem.require("bounded-queue-thread-pool");
        assertEquals(2, threadFactory.keys().size());
    }

    @Test
    public void testSimpleBlockingBoundedQueueThreadPool() throws Exception {
        List<ModelNode> updates = createSubSystem("<blocking-bounded-queue-thread-pool name=\"test-pool\">" +
                "<max-threads count=\"1\"/><queue-length count=\"1\"/></blocking-bounded-queue-thread-pool>");
        assertEquals(2, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("blocking-bounded-queue-thread-pool");
        assertEquals(1, threadPool.keys().size());
    }

    @Test
    public void testSimpleBlockingBoundedQueueThreadPool1_0() throws Exception {
        List<ModelNode> updates = createSubSystem("<bounded-queue-thread-pool name=\"test-pool\" blocking=\"true\">"
                + "   <max-threads count=\"1\" per-cpu=\"0\"/>" + "   <queue-length count=\"1\"/>"
                + "</bounded-queue-thread-pool>", Namespace.THREADS_1_0);
        assertEquals(2, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("blocking-bounded-queue-thread-pool");
        assertEquals(1, threadPool.keys().size());
    }

    @Test
    public void testFullBlockingBoundedQueueThreadPool() throws Exception {
        List<ModelNode> updates = createSubSystem(
                "<thread-factory name=\"test-factory\"/>" +
                        "<blocking-bounded-queue-thread-pool name=\"test-pool\" allow-core-timeout=\"true\">" +
                        "   <core-threads count=\"200\"/>" +
                        "   <max-threads count=\"100\"/>" +
                        "   <queue-length count=\"300\"/>" +
                        "   <keepalive-time time=\"1000\" unit=\"MILLISECONDS\"/>" +
                        "   <thread-factory name=\"test-factory\"/>" +
                        "</blocking-bounded-queue-thread-pool>");

        executeForResult(updates.get(0));
        executeForResult(updates.get(1));
        executeForResult(updates.get(2));

        checkFullBlockingBoundedQueueThreadPool();
    }

    @Test
    public void testFullBlockingBoundedQueueThreadPool1_0() throws Exception {
        List<ModelNode> updates = createSubSystem(
                "<thread-factory name=\"test-factory\"/>" +
                        "<bounded-queue-thread-pool name=\"test-pool\" blocking=\"true\" allow-core-timeout=\"true\">" +
                        "   <core-threads count=\"200\"/>" +
                        "   <max-threads count=\"100\" per-cpu=\"0\"/>" +
                        "   <queue-length count=\"300\"/>" +
                        "   <keepalive-time time=\"1000\" unit=\"MILLISECONDS\"/>" +
                        "   <thread-factory name=\"test-factory\"/>" +
                        "   <handoff-executor name=\"other\"/>" +
                        "   <properties>" +
                        "      <property name=\"propA\" value=\"valueA\"/>" +
                        "      <property name=\"propB\" value=\"valueB\"/>" +
                        "   </properties>" +
                        "</bounded-queue-thread-pool>", Namespace.THREADS_1_0);

        executeForResult(updates.get(0));
        executeForResult(updates.get(1));
        executeForResult(updates.get(2));

        checkFullBlockingBoundedQueueThreadPool();
    }

    private void checkFullBlockingBoundedQueueThreadPool() throws Exception {
        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("blocking-bounded-queue-thread-pool");
        assertEquals(1, threadPool.keys().size());
        assertTrue(threadPool.require("test-pool").require("allow-core-timeout").asBoolean());
        assertEquals(200, threadPool.require("test-pool").require(CORE_THREADS).asInt());
        assertEquals(300, threadPool.require("test-pool").require(QUEUE_LENGTH).asInt());
        assertEquals(100, threadPool.require("test-pool").require(MAX_THREADS).asInt());
        assertEquals(1000L, threadPool.require("test-pool").require(KEEPALIVE_TIME).require(TIME).asLong());
        assertEquals("MILLISECONDS", threadPool.require("test-pool").require(KEEPALIVE_TIME).require(UNIT).asString());
        assertFalse(threadPool.has(HANDOFF_EXECUTOR));
    }

    @Test
    public void testSeveralBlockingBoundedQueueThreadPools() throws Exception {
        List<ModelNode> updates = createSubSystem("<blocking-bounded-queue-thread-pool name=\"test-poolA\">" +
                "<max-threads count=\"1\"/><queue-length count=\"1\"/></blocking-bounded-queue-thread-pool>"
                + "<blocking-bounded-queue-thread-pool name=\"test-poolB\">" +
                "<max-threads count=\"1\"/><queue-length count=\"1\"/></blocking-bounded-queue-thread-pool>");
        assertEquals(3, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadFactory = subsystem.require("blocking-bounded-queue-thread-pool");
        assertEquals(2, threadFactory.keys().size());
    }

    @Test
    public void testSeveralBlockingBoundedQueueThreadPools1_0() throws Exception {
        List<ModelNode> updates = createSubSystem("<bounded-queue-thread-pool name=\"test-poolA\" blocking=\"true\">"
                + "   <max-threads count=\"1\" per-cpu=\"2\"/>" + "   <queue-length count=\"1\" per-cpu=\"2\"/>"
                + "</bounded-queue-thread-pool>" + "<bounded-queue-thread-pool name=\"test-poolB\" blocking=\"true\">"
                + "   <max-threads count=\"1\" per-cpu=\"2\"/>" + "   <queue-length count=\"1\" per-cpu=\"2\"/>"
                + "</bounded-queue-thread-pool>", Namespace.THREADS_1_0);
        assertEquals(3, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("subsystem").require("threads");
        ModelNode threadFactory = subsystem.require("blocking-bounded-queue-thread-pool");
        assertEquals(2, threadFactory.keys().size());
    }

    private ModelNode createOperation(String operationName, String... address) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        if (address.length > 0) {
            for (String addr : address) {
                operation.get(OP_ADDR).add(addr);
            }
        } else {
            operation.get(OP_ADDR).setEmptyList();
        }

        return operation;
    }


    private List<ModelNode> createSubSystem(String subsystemContents) throws Exception {
        return createSubSystem(subsystemContents, Namespace.THREADS_1_1);
    }

    private List<ModelNode> createSubSystem(String subsystemContents, Namespace namespace) throws Exception {
        final String xmlContent = "<subsystem xmlns=\"" + namespace.getUriString() + "\">" + subsystemContents + "</subsystem>";
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());
        this.services = builder.build();
        this.model = services.readWholeModel();
        return parse(xmlContent);
    }

    /**
     * Override to get the actual result from the response.
     *
     * @param operation the operation to execute
     * @return the response's "result" child node
     * @throws OperationFailedException if the response outcome is "failed"
     */
    public ModelNode executeForResult(ModelNode operation) throws OperationFailedException {
        createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);
        ModelNode rsp = services.executeForResult(operation);
        if (FAILED.equals(rsp.get(OUTCOME).asString())) {
            ModelNode fd = rsp.get(FAILURE_DESCRIPTION);
            throw new OperationFailedException(fd.toString(), fd);
        }
        model = services.readWholeModel();
        return rsp.get(RESULT);
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization() {

            @Override
            protected ProcessType getProcessType() {
                return ProcessType.HOST_CONTROLLER;
            }

            @Override
            protected RunningMode getRunningMode() {
                return RunningMode.ADMIN_ONLY;
            }
        };
    }
}
