/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.suites;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LEVEL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WARNING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WARNINGS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.remoting.logging.RemotingLogger;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class OperationWarningTestsCase {

    private static DomainTestSupport testSupport;
    private static DomainLifecycleUtil domainPrimaryLifecycleUtil;

    protected static final String NAME_WORKER = "puppet-primary";
    protected static final String WORKER = "worker";
    protected static final String NAME_PROFILE = "default";
    protected static final PathAddress ADDRESS_WORKER = PathAddress.pathAddress(PathElement.pathElement(PROFILE, NAME_PROFILE),
            PathElement.pathElement(SUBSYSTEM, "io"), PathElement.pathElement(WORKER, NAME_WORKER));
    protected static final PathAddress ADDRESS_REMOTING = PathAddress.pathAddress(
            PathElement.pathElement(PROFILE, NAME_PROFILE), PathElement.pathElement(SUBSYSTEM, "remoting"));
    protected static final String BAD_LEVEL = "X_X";

    @BeforeClass
    public static void beforeClass() throws Exception {
        testSupport = DomainTestSuite.createSupport(OperationWarningTestsCase.class.getSimpleName());
        domainPrimaryLifecycleUtil = testSupport.getDomainPrimaryLifecycleUtil();
        addWorker();
    }

    @AfterClass
    public static void afterClass() {
        try {
            setRemotingWorkerTo("default", "OFF");
        } catch (Exception e) {
        }

        try {
            removeWorker();
        } catch (Exception e) {
        }
        testSupport = null;
        domainPrimaryLifecycleUtil = null;
        DomainTestSuite.stopSupport();
    }

    @Test
    public void testMe() throws Exception {
         ModelNode result = setRemotingWorkerTo(NAME_WORKER,BAD_LEVEL);
         Assert.assertTrue(result.toString(), Operations.isSuccessfulOutcome(result));
         Assert.assertTrue(result.toString(),result.hasDefined(RESPONSE_HEADERS));
         ModelNode responseHeaders = result.get(RESPONSE_HEADERS);
         Assert.assertTrue(responseHeaders.hasDefined(WARNINGS));
         List<ModelNode> warnings = responseHeaders.get(WARNINGS).asList();
         Assert.assertTrue(warnings.size() == 2);
         ModelNode warningLoggerLevel = warnings.get(0);
         String message = warningLoggerLevel.get(WARNING).asString();
         Assert.assertEquals(ControllerLogger.ROOT_LOGGER.couldntConvertWarningLevel(BAD_LEVEL), message);
         Level level = Level.parse(warningLoggerLevel.get(LEVEL).asString());
         Assert.assertEquals(Level.ALL,level);
         ModelNode warningWorker = warnings.get(1);
         message = warningWorker.get(WARNING).asString();
         Assert.assertEquals(RemotingLogger.ROOT_LOGGER.warningOnWorkerChange(NAME_WORKER), message);
         level = Level.parse(warningWorker.get(LEVEL).asString());
         Assert.assertEquals(Level.WARNING,level);
         //default level is "WARNING, set to severe and check if there are warnings
         result = setRemotingWorkerTo("default","SEVERE");
         responseHeaders = result.get(RESPONSE_HEADERS);
         Assert.assertFalse(responseHeaders.hasDefined(WARNINGS));
         result = setRemotingWorkerTo("default","OFF");
         responseHeaders = result.get(RESPONSE_HEADERS);
         Assert.assertFalse(responseHeaders.hasDefined(WARNINGS));
    }

    protected static void addWorker() throws Exception {
        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP_ADDR).set(ADDRESS_WORKER.toModelNode());
        op.get(ModelDescriptionConstants.OP).set(ADD);
        ModelNode result = domainPrimaryLifecycleUtil.getDomainClient().execute(op);
        Assert.assertTrue(result.toString(), Operations.isSuccessfulOutcome(result));
    }

    protected static void removeWorker() throws Exception {
        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP_ADDR).set(ADDRESS_WORKER.toModelNode());
        op.get(ModelDescriptionConstants.OP).set(REMOVE);
        ModelNode result = domainPrimaryLifecycleUtil.getDomainClient().execute(op);
        Assert.assertTrue(result.toString(), Operations.isSuccessfulOutcome(result));
    }

    protected static ModelNode setRemotingWorkerTo(final String name, final String level) throws IOException {
        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP_ADDR).set(ADDRESS_REMOTING.toModelNode());
        op.get(ModelDescriptionConstants.OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(NAME).set(WORKER);
        op.get(VALUE).set(name);
        op.get(OPERATION_HEADERS).get(ModelDescriptionConstants.WARNING_LEVEL).set(level);
        ModelNode result = domainPrimaryLifecycleUtil.getDomainClient().execute(op);
        return result;
    }
}
