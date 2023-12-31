/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.core.model.test.access;

import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.APPLICATION_CLASSIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHORIZATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CLASSIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONSTRAINT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SENSITIVITY_CLASSIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT_EXPRESSION;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.domain.management.access.ApplicationClassificationConfigResourceDefinition;
import org.jboss.as.domain.management.access.SensitivityResourceDefinition;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * Simple test case to test the parsing and marshalling of the <access-control /> element within the standalone.xml
 * configuration.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class StandaloneAccessControlTestCase extends AbstractCoreModelTest {
    private static final String SOCKET_CONFIG = SensitivityClassification.SOCKET_CONFIG.getName();

    @Test
    public void testConfiguration() throws Exception {
        //Initialize some additional constraints
        new SensitiveTargetAccessConstraintDefinition(new SensitivityClassification("play", "security-realm", true, true, true));
        new SensitiveTargetAccessConstraintDefinition(new SensitivityClassification("system-property", "system-property", true, true, true));
        new ApplicationTypeAccessConstraintDefinition(new ApplicationTypeConfig("play", "deployment", false));


        KernelServices kernelServices = createKernelServicesBuilder(TestModelType.STANDALONE)
                .setXmlResource("standalone.xml")
                .validateDescription()
                .build();
        Assert.assertTrue(kernelServices.isSuccessfulBoot());

        String marshalled = kernelServices.getPersistedSubsystemXml();
        ModelTestUtils.compareXml(ModelTestUtils.readResource(this.getClass(), "standalone.xml"), marshalled);

        //////////////////////////////////////////////////////////////////////////////////
        //Check that both set and undefined configured constraint settings get returned

        /*
         * <sensitive-classification type="play" name="security-realm" requires-addressable="false" requires-read="false" requires-write="false" />
         * <sensitive-classification type="system-property" name="system-property" requires-addressable="true" requires-read="true" requires-write="true" />
         * system-property sensitive classification default values are false, false, true
         */

        System.out.println(kernelServices.readWholeModel());
        //Sensitivity classification
        //This one is undefined
        ModelNode result = ModelTestUtils.checkOutcome(
                kernelServices.executeOperation(
                    Util.getReadAttributeOperation(PathAddress.pathAddress(
                        pathElement(CORE_SERVICE, MANAGEMENT),
                        pathElement(ACCESS, AUTHORIZATION),
                        pathElement(CONSTRAINT, SENSITIVITY_CLASSIFICATION),
                        pathElement(TYPE, CORE),
                        pathElement(CLASSIFICATION, SOCKET_CONFIG)), SensitivityResourceDefinition.CONFIGURED_REQUIRES_ADDRESSABLE.getName())));
        checkResultExists(result, new ModelNode());
        //This one is undefined
        result = ModelTestUtils.checkOutcome(
                kernelServices.executeOperation(
                    Util.getReadAttributeOperation(PathAddress.pathAddress(
                        pathElement(CORE_SERVICE, MANAGEMENT),
                        pathElement(ACCESS, AUTHORIZATION),
                        pathElement(CONSTRAINT, SENSITIVITY_CLASSIFICATION),
                        pathElement(TYPE, "play"),
                        pathElement(CLASSIFICATION, SECURITY_REALM)), SensitivityResourceDefinition.CONFIGURED_REQUIRES_ADDRESSABLE.getName())));
        checkResultExists(result, ModelNode.FALSE);

        // WFCORE-3995 Test write operations on configured-requires-addressable
        // This should fail as sensitivity constraint attribute configured-requires-read and configured-requires-write must not be false before writing configured-requires-addressable to true
        result = ModelTestUtils.checkFailed(
              kernelServices.executeOperation(
                  Util.getWriteAttributeOperation(PathAddress.pathAddress(
                      pathElement(CORE_SERVICE, MANAGEMENT),
                      pathElement(ACCESS, AUTHORIZATION),
                      pathElement(CONSTRAINT, SENSITIVITY_CLASSIFICATION),
                      pathElement(TYPE, "play"),
                      pathElement(CLASSIFICATION, SECURITY_REALM)), SensitivityResourceDefinition.CONFIGURED_REQUIRES_ADDRESSABLE.getName(), true)));
        checkResultNotExists(result);

        // This should fail as sensitivity constraint attribute configured-requires-read and configured-requires-write must not be false before undefine configured-requires-addressable to its default value true
        result = ModelTestUtils.checkFailed(
              kernelServices.executeOperation(
                  Util.getUndefineAttributeOperation(PathAddress.pathAddress(
                      pathElement(CORE_SERVICE, MANAGEMENT),
                      pathElement(ACCESS, AUTHORIZATION),
                      pathElement(CONSTRAINT, SENSITIVITY_CLASSIFICATION),
                      pathElement(TYPE, "play"),
                      pathElement(CLASSIFICATION, SECURITY_REALM)), SensitivityResourceDefinition.CONFIGURED_REQUIRES_ADDRESSABLE.getName())));
        checkResultNotExists(result);

        // WFCORE-3995 Test write operations on configured-requires-read
        // This should fail as sensitivity constraint attribute configured-requires-addressable must not be true before writing configured-requires-read to false
        result = ModelTestUtils.checkFailed(
                kernelServices.executeOperation(
                    Util.getWriteAttributeOperation(PathAddress.pathAddress(
                        pathElement(CORE_SERVICE, MANAGEMENT),
                        pathElement(ACCESS, AUTHORIZATION),
                        pathElement(CONSTRAINT, SENSITIVITY_CLASSIFICATION),
                        pathElement(TYPE, "system-propery"),
                        pathElement(CLASSIFICATION, SECURITY_REALM)), SensitivityResourceDefinition.CONFIGURED_REQUIRES_READ.getName(), false)));
        checkResultNotExists(result);

        // This should fail as sensitivity constraint attribute configured-requires-addressable must not be true before undefine configured-requires-read its default value false
        result = ModelTestUtils.checkFailed(
                kernelServices.executeOperation(
                    Util.getUndefineAttributeOperation(PathAddress.pathAddress(
                        pathElement(CORE_SERVICE, MANAGEMENT),
                        pathElement(ACCESS, AUTHORIZATION),
                        pathElement(CONSTRAINT, SENSITIVITY_CLASSIFICATION),
                        pathElement(TYPE, "system-propery"),
                        pathElement(CLASSIFICATION, SECURITY_REALM)), SensitivityResourceDefinition.CONFIGURED_REQUIRES_READ.getName())));
        checkResultNotExists(result);

        // This should fail as sensitivity constraint attribute configured-requires-write must not be false before writing configured-requires-read to true
        result = ModelTestUtils.checkFailed(
                kernelServices.executeOperation(
                    Util.getWriteAttributeOperation(PathAddress.pathAddress(
                        pathElement(CORE_SERVICE, MANAGEMENT),
                        pathElement(ACCESS, AUTHORIZATION),
                        pathElement(CONSTRAINT, SENSITIVITY_CLASSIFICATION),
                        pathElement(TYPE, "play"),
                        pathElement(CLASSIFICATION, SECURITY_REALM)), SensitivityResourceDefinition.CONFIGURED_REQUIRES_READ.getName(), true)));
        checkResultNotExists(result);

        // This should fail as sensitivity constraint attribute configured-requires-addressable must not be false before undefine configured-requires-read to its default value true
        result = ModelTestUtils.checkFailed(
                kernelServices.executeOperation(
                    Util.getUndefineAttributeOperation(PathAddress.pathAddress(
                        pathElement(CORE_SERVICE, MANAGEMENT),
                        pathElement(ACCESS, AUTHORIZATION),
                        pathElement(CONSTRAINT, SENSITIVITY_CLASSIFICATION),
                        pathElement(TYPE, "play"),
                        pathElement(CLASSIFICATION, SECURITY_REALM)), SensitivityResourceDefinition.CONFIGURED_REQUIRES_READ.getName())));
        checkResultNotExists(result);

        // WFCORE-3995 Test write operations on configured-requires-write
        // This should fail as sensitivity constraint attribute configured-requires-addressable and configured-requires-read must not be true before writing configured-requires-read to false
        result = ModelTestUtils.checkFailed(
                kernelServices.executeOperation(
                    Util.getWriteAttributeOperation(PathAddress.pathAddress(
                        pathElement(CORE_SERVICE, MANAGEMENT),
                        pathElement(ACCESS, AUTHORIZATION),
                        pathElement(CONSTRAINT, SENSITIVITY_CLASSIFICATION),
                        pathElement(TYPE, "system-propery"),
                        pathElement(CLASSIFICATION, SECURITY_REALM)), SensitivityResourceDefinition.CONFIGURED_REQUIRES_READ.getName(), false)));
        checkResultNotExists(result);

        //VaultExpression
        //It is defined
        PathAddress vaultAddress = PathAddress.pathAddress(
                pathElement(CORE_SERVICE, MANAGEMENT),
                pathElement(ACCESS, AUTHORIZATION),
                pathElement(CONSTRAINT, VAULT_EXPRESSION));
        result = ModelTestUtils.checkOutcome(
                kernelServices.executeOperation(
                        Util.getReadAttributeOperation(vaultAddress, SensitivityResourceDefinition.CONFIGURED_REQUIRES_READ.getName())));
        checkResultExists(result, ModelNode.FALSE);
        //Now undefine it and check again (need to undefine configured-requires-write first)
        ModelTestUtils.checkOutcome(
                kernelServices.executeOperation(
                        Util.getUndefineAttributeOperation(vaultAddress, SensitivityResourceDefinition.CONFIGURED_REQUIRES_WRITE.getName())));
        ModelTestUtils.checkOutcome(
                kernelServices.executeOperation(
                        Util.getUndefineAttributeOperation(vaultAddress, SensitivityResourceDefinition.CONFIGURED_REQUIRES_READ.getName())));
        result = ModelTestUtils.checkOutcome(
                kernelServices.executeOperation(
                        Util.getReadAttributeOperation(vaultAddress, SensitivityResourceDefinition.CONFIGURED_REQUIRES_READ.getName())));
        checkResultExists(result, new ModelNode());

        //Application classification
        //It is defined
        PathAddress applicationAddress = PathAddress.pathAddress(
                pathElement(CORE_SERVICE, MANAGEMENT),
                pathElement(ACCESS, AUTHORIZATION),
                pathElement(CONSTRAINT, APPLICATION_CLASSIFICATION),
                pathElement(TYPE, "play"),
                pathElement(CLASSIFICATION, "deployment"));
        result = ModelTestUtils.checkOutcome(
                kernelServices.executeOperation(
                        Util.getReadAttributeOperation(applicationAddress, ApplicationClassificationConfigResourceDefinition.CONFIGURED_APPLICATION.getName())));
        checkResultExists(result, ModelNode.FALSE);
        //Now undefine it and check again
        ModelTestUtils.checkOutcome(
                kernelServices.executeOperation(
                        Util.getUndefineAttributeOperation(applicationAddress, ApplicationClassificationConfigResourceDefinition.CONFIGURED_APPLICATION.getName())));
        result = ModelTestUtils.checkOutcome(
                kernelServices.executeOperation(
                        Util.getReadAttributeOperation(applicationAddress, ApplicationClassificationConfigResourceDefinition.CONFIGURED_APPLICATION.getName())));
        checkResultExists(result, new ModelNode());
        kernelServices.shutdown();

    }

    private void checkResultExists(ModelNode result, ModelNode expected) {
        Assert.assertTrue(result.has(RESULT));
        Assert.assertEquals(expected, result.get(RESULT));
    }

    private void checkResultNotExists(ModelNode result) {
        Assert.assertFalse(result.has(RESULT));
    }
}
