/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.subsystem.test.validation;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.math.BigDecimal;
import java.util.Collections;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.operations.validation.MinMaxValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.validation.subsystem.ValidateSubsystemExtension;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
/*TODO tc: not sure how this test is even relevent in current context of writing subsystems as add description providers
are no longer written by subsystem developers but are generated by generic code.
  */
public class ValidateOperationsTestCase extends AbstractSubsystemTest {

    public ValidateOperationsTestCase() {
        super(ValidateSubsystemExtension.SUBSYSTEM_NAME, new ValidateSubsystemExtension());
    }

    /**
     * Tests that a valid operation passes validation
     */
    @Test
    public void testValidNoArgs() throws Exception {
        ModelNode operation = createAddOperation();
        KernelServices services = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setBootOperations(operation)
                .build();

        services.validateOperation(operation);
    }

    /**
     * Tests that a valid operation passes validation
     */
    @Test
    public void testValidArgs() throws Exception {
        getMainExtension().setAddAttributes(
                SimpleAttributeDefinitionBuilder.create("test", ModelType.LONG)
                        .setRequired(true)
                        .setValidator(new LongRangeValidator(0L, 2L))
                        .build()
        );
        ModelNode operation = createAddOperation();
        operation.get("test").set(1);
        KernelServices services = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setBootOperations(Collections.singletonList(operation))
                .build();

        services.validateOperation(operation);
    }

    @Test
    public void testNonExistentParameter() throws Exception {
        ModelNode operation = createAddOperation();
        operation.get("test").set(1);
        try {
            createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                    .setBootOperations(Collections.singletonList(operation))
                    .build();
            Assert.fail("Not valid");
        } catch (Exception expected) {
            // ok
        }
    }

    @Test
    public void testMissingRequiredParam() throws Exception {
        getMainExtension().setAddAttributes(
                SimpleAttributeDefinitionBuilder.create("test", ModelType.LONG)
                        .setRequired(true)
                        .build()
        );
        ModelNode operation = createAddOperation();
        try {
            createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                    .setBootOperations(Collections.singletonList(operation))
                    .build();
            Assert.fail("Not valid");
        } catch (Exception expected) {
            // ok
        }
    }

    @Test
    public void testMissingRequiredParam2() throws Exception {
        getMainExtension().setAddAttributes(
                SimpleAttributeDefinitionBuilder.create("test", ModelType.LONG)
                        .build()
        );
        ModelNode operation = createAddOperation();
        try {
            createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                    .setBootOperations(Collections.singletonList(operation))
                    .build();
            Assert.fail("Not valid");
        } catch (Exception expected) {
            // ok
        }
    }

    @Test
    public void testMissingNonRequiredParam() throws Exception {
        getMainExtension().setAddAttributes(
                SimpleAttributeDefinitionBuilder.create("test", ModelType.LONG)
                        .setRequired(false)
                        .build()
        );
        ModelNode operation = createAddOperation();
        createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setBootOperations(Collections.singletonList(operation))
                .build();
    }

    @Test
    public void testWrongParamType() throws Exception {
        getMainExtension().setAddAttributes(
                SimpleAttributeDefinitionBuilder.create("test", ModelType.LONG)
                        .build()
        );
        ModelNode operation = createAddOperation();
        operation.get("test").set("Hello");
        try {
            createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                    .setBootOperations(Collections.singletonList(operation))
                    .build();
            Assert.fail("Not valid");
        } catch (Exception expected) {
            // ok
        }
    }

    @Test
    public void testBigDecimalRangeTooSmall() throws Exception {
        getMainExtension().setAddAttributes(
                SimpleAttributeDefinitionBuilder.create("test", ModelType.BIG_DECIMAL)
                        .setValidator(new BigDecimalRangeValidator(10, Integer.MAX_VALUE))
                        .build()
        );
        ModelNode operation = createAddOperation();
        operation.get("test").set(new BigDecimal("5"));
        try {
            createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                    .setBootOperations(Collections.singletonList(operation))
                    .build();
            Assert.fail("Not valid");
        } catch (Exception expected) {
            // ok
        }
    }

    @Test
    public void testBigDecimalRangeTooLarge() throws Exception {
        getMainExtension().setAddAttributes(
                SimpleAttributeDefinitionBuilder.create("test", ModelType.BIG_DECIMAL)
                        .setValidator(new BigDecimalRangeValidator(Integer.MIN_VALUE, 10))
                        .build()
        );
        ModelNode operation = createAddOperation();
        operation.get("test").set(new BigDecimal("15"));
        try {
            createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                    .setBootOperations(Collections.singletonList(operation))
                    .build();
            Assert.fail("Not valid");
        } catch (Exception expected) {
            // ok
        }
    }

    @Test
    public void testIntRangeTooSmall() throws Exception {
        getMainExtension().setAddAttributes(
                SimpleAttributeDefinitionBuilder.create("test", ModelType.INT)
                        .setValidator(new IntRangeValidator(10))
                        .build()
        );
        ModelNode operation = createAddOperation();
        operation.get("test").set(5);
        try {
            createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                    .setBootOperations(operation)
                    .build();
            Assert.fail("Not valid");
        } catch (Exception expected) {
            // ok
        }
    }

    @Test
    public void testIntRangeTooLarge() throws Exception {
        getMainExtension().setAddAttributes(
                SimpleAttributeDefinitionBuilder.create("test", ModelType.INT)
                        .setValidator(new IntRangeValidator(Integer.MIN_VALUE, 10))
                        .build()
        );
        ModelNode operation = createAddOperation();
        operation.get("test").set(15);
        try {
            createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                    .setBootOperations(operation)
                    .build();
            Assert.fail("Not valid");
        } catch (Exception expected) {
            // ok
        }
    }

    @Test
    public void testStringTooShort() throws Exception {
        getMainExtension().setAddAttributes(
                SimpleAttributeDefinitionBuilder.create("test", ModelType.STRING)
                        .setValidator(new StringLengthValidator(3))
                        .build()
        );
        ModelNode operation = createAddOperation();
        operation.get("test").set("Yo");
        try {
            createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                    .setBootOperations(operation)
                    .build();
            Assert.fail("Not valid");
        } catch (Exception expected) {
            // ok
        }
    }

    @Test
    public void testStringTooLong() throws Exception {
        getMainExtension().setAddAttributes(
                SimpleAttributeDefinitionBuilder.create("test", ModelType.STRING)
                        .setValidator(new StringLengthValidator(0, 1))
                        .build()
        );
        ModelNode operation = createAddOperation();
        operation.get("test").set("Yo");
        try {
            createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                    .setBootOperations(operation)
                    .build();
            Assert.fail("Not valid");
        } catch (Exception expected) {
            // ok
        }
    }

    @Test
    public void testStringJustRight() throws Exception {
        getMainExtension().setAddAttributes(
                SimpleAttributeDefinitionBuilder.create("test", ModelType.STRING)
                        .setValidator(new StringLengthValidator(2, 2))
                        .build()
        );
        ModelNode operation = createAddOperation();
        operation.get("test").set("Yo");
        createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setBootOperations(operation)
                .build();
    }

    @Test
    public void testBytesTooShort() throws Exception {
        getMainExtension().setAddAttributes(
                SimpleAttributeDefinitionBuilder.create("test", ModelType.BYTES)
                        .setMinSize(3)
                        .build()
        );
        ModelNode operation = createAddOperation();
        operation.get("test").set(new byte[] {1, 2});
        try {
            createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                    .setBootOperations(operation)
                    .build();
            Assert.fail("Not valid");
        } catch (Exception expected) {
            // ok
        }
    }

    @Test
    public void testBytesTooLong() throws Exception {
        getMainExtension().setAddAttributes(
                SimpleAttributeDefinitionBuilder.create("test", ModelType.BYTES)
                        .setMaxSize(1)
                        .build()
        );
        ModelNode operation = createAddOperation();
        operation.get("test").set(new byte[] {1, 2});
        try {
            createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                    .setBootOperations(operation)
                    .build();
            Assert.fail("Not valid");
        } catch (Exception expected) {
            //
        }
    }

    @Test
    public void testBytesJustRight() throws Exception {
        getMainExtension().setAddAttributes(
                SimpleAttributeDefinitionBuilder.create("test", ModelType.BYTES)
                        .setMinSize(2).setMaxSize(2)
                        .build()
        );
        ModelNode operation = createAddOperation();
        operation.get("test").set(new byte[] {1, 2});
        createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setBootOperations(operation)
                .build();
    }

    @Test
    public void testListTooShort() throws Exception {
        getMainExtension().setAddAttributes(
                StringListAttributeDefinition.Builder.of("test")
                        .setMinSize(3)
                        .build()
        );
        ModelNode operation = createAddOperation();
        operation.get("test").add("1");
        operation.get("test").add("2");
        try {
            createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                    .setBootOperations(operation)
                    .build();
            Assert.fail("Not valid");
        } catch (Exception expected) {
            // ok
        }
    }

    @Test
    public void testListTooLong() throws Exception {
        getMainExtension().setAddAttributes(
                StringListAttributeDefinition.Builder.of("test")
                        .setMaxSize(1)
                        .build()
        );
        ModelNode operation = createAddOperation();
        operation.get("test").add("1");
        operation.get("test").add("2");
        try {
            createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                    .setBootOperations(operation)
                    .build();
            Assert.fail("Not valid");
        } catch (Exception expected) {
            // ok
        }
    }

    @Test
    public void testListJustRight() throws Exception {
        getMainExtension().setAddAttributes(
                StringListAttributeDefinition.Builder.of("test")
                        .setMinSize(2).setMaxSize(2)
                        .build()
        );
        ModelNode operation = createAddOperation();
        operation.get("test").add("1");
        operation.get("test").add("2");
        createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setBootOperations(operation)
                .build();
    }

    @Test
    public void testListCorrectType() throws Exception {
        getMainExtension().setAddAttributes(
                PrimitiveListAttributeDefinition.Builder.of("test", ModelType.INT)
                        .build()
        );
        ModelNode operation = createAddOperation();
        operation.get("test").add(1);
        operation.get("test").add(2);
        createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setBootOperations(operation)
                .build();
    }

    @Test
    public void testListWrongType() throws Exception {
        getMainExtension().setAddAttributes(
                PrimitiveListAttributeDefinition.Builder.of("test", ModelType.INT)
                        .build()
        );
        ModelNode operation = createAddOperation();
        operation.get("test").add(1);
        operation.get("test").add("Hello");
        try {
            createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                    .setBootOperations(operation)
                    .build();
            Assert.fail("Not valid");
        } catch (Exception expected) {
            // ok
        }
    }

    protected ValidateSubsystemExtension getMainExtension() {
        return (ValidateSubsystemExtension)super.getMainExtension();
    }

    private ModelNode createAddOperation() {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, ValidateSubsystemExtension.SUBSYSTEM_NAME)).toModelNode());
        return operation;
    }

    private static class BigDecimalRangeValidator extends ModelTypeValidator implements MinMaxValidator {

        private final BigDecimal min;
        private final BigDecimal max;
        private BigDecimalRangeValidator(int min, int max) {
            super(ModelType.BIG_DECIMAL);
            this.min = new BigDecimal(min);
            this.max = new BigDecimal(max);
        }

        @Override
        public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
            super.validateParameter(parameterName, value);
            if (value.getType() == ModelType.BIG_DECIMAL && value.asBigDecimal().compareTo(max) > 0) {
                throw new OperationFailedException(value.asString());
            }
            if (value.getType() == ModelType.BIG_DECIMAL && value.asBigDecimal().compareTo(min) < 0) {
                throw new OperationFailedException(value.asString());
            }
        }

        @Override
        public Long getMin() {
            return min.longValueExact();
        }

        @Override
        public Long getMax() {
            return max.longValueExact();
        }
    }
}
