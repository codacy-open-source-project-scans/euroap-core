/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.controller.operations.validation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Validates that the given parameter is of the correct type.
 * <p>
 * Note on strict type matching:
 * </p>
 * <p>
 * The constructor takes a parameter {@code strictType}. If {@code strictType} is {@code false}, nodes being validated do not
 * need to precisely match the type(s) passed to the constructor; rather a limited set of value conversions
 * will be attempted, and if the node value can be converted, the node is considered to match the required type.
 * The conversions are:
 * <ul>
 * <li>For BIG_DECIMAL, BIG_INTEGER, DOUBLE, INT, LONG and PROPERTY, the related ModelNode.asXXX() method is invoked; if
 * no exception is thrown the type is considered to match.</li>
 * <li>For BOOLEAN, if the node is of type BOOLEAN it is considered to match. If it is of type STRING with a value
 * ignoring case of "true" or "false" it is considered to match.</li>
 * <li>For OBJECT, if the node is of type OBJECT or PROPERTY it is considered to match. If it is of type LIST and each element
 * in the list is of type PROPERTY it is considered to match.</li>
 * <li>For STRING, if the node is of type STRING, BIG_DECIMAL, BIG_INTEGER, DOUBLE, INT or LONG it is considered to match.</li>
 * </ul>
 * For all other types, an exact match is required.
 * </p>
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ModelTypeValidator implements ParameterValidator {
    protected static final BigDecimal BIGDECIMAL_MAX = BigDecimal.valueOf(Integer.MAX_VALUE);
    protected static final BigDecimal BIGDECIMAL_MIN = BigDecimal.valueOf(Integer.MIN_VALUE);
    protected static final BigInteger BIGINTEGER_MAX = BigInteger.valueOf(Integer.MAX_VALUE);
    protected static final BigInteger BIGINTEGER_MIN = BigInteger.valueOf(Integer.MIN_VALUE);

    private static final Map<EnumSet<ModelType>, Set<ModelType>> flagSets = new ConcurrentHashMap<>(16);
    private static Set<ModelType> sharedSetOf(boolean allowExpressions, ModelType firstValidType, ModelType... otherValidTypes) {
        EnumSet<ModelType> baseSet = EnumSet.of(firstValidType, otherValidTypes);
        if (allowExpressions) {
            baseSet.add(ModelType.EXPRESSION);
        }
        Set<ModelType> result = flagSets.get(baseSet);
        if (result == null) {
            Set<ModelType> immutable = Collections.unmodifiableSet(baseSet);
            Set<ModelType> existing = flagSets.putIfAbsent(baseSet, immutable);
            result = existing == null ? immutable : existing;
        }
        return result;
    }

    protected final Set<ModelType> validTypes;
    protected final boolean nullable;
    protected final boolean strictType;

    /**
     * Same as {@code ModelTypeValidator(type, false, false, false)}.
     *
     * @param type the valid type. Cannot be {@code null}
     */
    public ModelTypeValidator(final ModelType type) {
        this(false, false, false, type);
    }

    /**
     * Same as {@code ModelTypeValidator(type, nullable, false, false)}.
     *
     * @param type the valid type. Cannot be {@code null}
     * @param nullable whether {@link ModelType#UNDEFINED} is allowed
     */
    public ModelTypeValidator(final ModelType type, final boolean nullable) {
        this(nullable, false, false, type);
    }

    /**
     * Same as {@code ModelTypeValidator(type, nullable, allowExpressions, false)}.
     *
     * @param type the valid type. Cannot be {@code null}
     * @param nullable whether {@link ModelType#UNDEFINED} is allowed
     * @param allowExpressions whether {@link ModelType#EXPRESSION} is allowed
     */
    public ModelTypeValidator(final ModelType type, final boolean nullable, final boolean allowExpressions) {
        this(nullable, allowExpressions, false, type);
    }

    /**
     * Creates a ModelTypeValidator that allows the given type.
     *
     * @param type the valid type. Cannot be {@code null}
     * @param nullable whether {@link ModelType#UNDEFINED} is allowed
     * @param allowExpressions whether {@link ModelType#EXPRESSION} is allowed
     * @param strictType {@code true} if the type of a node must precisely match {@code type}; {@code false} if the value
     *              conversions described in the class javadoc can be performed to check for compatible types
     */
    public ModelTypeValidator(final ModelType type, final boolean nullable, final boolean allowExpressions, final boolean strictType) {
        this(nullable, allowExpressions, strictType, type);
    }

    /**
     * Creates a ModelTypeValidator that allows potentially more than one type.
     *
     * @param nullable whether {@link ModelType#UNDEFINED} is allowed
     * @param allowExpressions whether {@link ModelType#EXPRESSION} is allowed
     * @param strictType {@code true} if the type of a node must precisely match {@code type}; {@code false} if the value
     *              conversions described in the class javadoc can be performed to check for compatible types
     * @param firstValidType a valid type. Cannot be {@code null}
     * @param otherValidTypes additional valid types. May be {@code null}
     */
    public ModelTypeValidator(final boolean nullable, final boolean allowExpressions, final boolean strictType, ModelType firstValidType, ModelType... otherValidTypes) {
        this.validTypes = sharedSetOf(allowExpressions, firstValidType, otherValidTypes);
        this.nullable = nullable;
        this.strictType = strictType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        RuntimeException cause = null;
        if (!value.isDefined()) {
            if (!nullable) {
                throw ControllerLogger.ROOT_LOGGER.nullNotAllowed(parameterName);
            }
        } else  {
            boolean matched = false;
            if (strictType) {
                matched = validTypes.contains(value.getType());
            } else {
                for (ModelType validType : validTypes) {
                    try {
                        if (matches(value, validType)) {
                            matched = true;
                            break;
                        }
                    } catch (RuntimeException e) {
                        cause = e;
                    }
                }
            }
            if (!matched) {
                if (cause == null) {
                    throw ControllerLogger.ROOT_LOGGER.incorrectType(parameterName, validTypes, value.getType());
                }
                String message = String.format("%s. %s",
                        ControllerLogger.ROOT_LOGGER.incorrectType(parameterName, validTypes, value.getType()).getLocalizedMessage(),
                        ControllerLogger.ROOT_LOGGER.typeConversionError(value, validTypes));
                throw new OperationFailedException(message, cause);
            }
        }
    }

    private boolean matches(ModelNode value, ModelType validType) {
        if (validType == value.getType()) {
            return true;
        }

        switch (validType) {
            case BIG_DECIMAL: {
                value.asBigDecimal();
                return true;
            }
            case BIG_INTEGER: {
                value.asBigInteger();
                return true;
            }
            case DOUBLE: {
                value.asDouble();
                return true;
            }
            case INT: {
                switch (value.getType()) {
                    case BIG_DECIMAL:
                        BigDecimal valueBigDecimal = value.asBigDecimal();
                        return (valueBigDecimal.compareTo(BIGDECIMAL_MAX) <= 0) && (valueBigDecimal.compareTo(BIGDECIMAL_MIN) >= 0);
                    case BIG_INTEGER:
                        BigInteger valueBigInteger = value.asBigInteger();
                        return (valueBigInteger.compareTo(BIGINTEGER_MAX) <= 0) && (valueBigInteger.compareTo(BIGINTEGER_MIN) >= 0);
                    case LONG:
                        Long valueLong = value.asLong();
                        return valueLong <= Integer.MAX_VALUE && valueLong >= Integer.MIN_VALUE;
                    case DOUBLE:
                        Double valueDouble = value.asDouble();
                        return valueDouble <= Integer.MAX_VALUE && valueDouble >= Integer.MIN_VALUE;
                    case STRING:
                        value.asInt();
                        return true;
                    default:
                        return false;
                }
            }
            case LONG: {
                switch (value.getType()) {
                    case BIG_DECIMAL:
                        BigDecimal valueBigDecimal = value.asBigDecimal();
                        return (valueBigDecimal.compareTo(BIGDECIMAL_MAX) <= 0) && (valueBigDecimal.compareTo(BIGDECIMAL_MIN) >= 0);
                    case BIG_INTEGER:
                        BigInteger valueBigInteger = value.asBigInteger();
                        return (valueBigInteger.compareTo(BIGINTEGER_MAX) <= 0) && (valueBigInteger.compareTo(BIGINTEGER_MIN) >= 0);
                    case DOUBLE:
                        Double valueDouble = value.asDouble();
                        return valueDouble <= Long.MAX_VALUE && valueDouble >= Long.MIN_VALUE;
                    case INT:
                        value.asLong();
                        return true;
                    case STRING:
                        value.asLong();
                        return true;
                    default:
                        return false;
                }
            }
            case PROPERTY: {
                value.asProperty();
                return true;
            }
            case BOOLEAN: {
                // Allow some type conversions, not others.
                switch (value.getType()) {
                    case STRING: {
                        String s = value.asString();
                        if ("false".equalsIgnoreCase(s) || "true".equalsIgnoreCase(s)) {
                            return true;
                        }
                        // throw a RuntimeException to trigger the catch block in the caller
                        // that results in the added typeConversionError message
                        throw new RuntimeException();
                    }
                    case BOOLEAN:
                        //case INT:
                        return true;
                }
                return false;
            }
            case OBJECT: {
                // We accept OBJECT, PROPERTY or LIST where all elements are PROPERTY
                switch (value.getType()) {
                    case PROPERTY:
                    case OBJECT:
                        return true;
                    case LIST: {
                        for (ModelNode node : value.asList()) {
                            if (node.getType() != ModelType.PROPERTY) {
                                return false;
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
            case STRING: {
                // Allow some type conversions, not others.
                switch (value.getType()) {
                    case BIG_DECIMAL:
                    case BIG_INTEGER:
                    case BOOLEAN:
                    case DOUBLE:
                    case INT:
                    case LONG:
                    case STRING:
                        return true;
                }
                return false;
            }
            case BYTES:
            // we could handle STRING but IMO if people want to allow STRING to byte[] conversion
            // they should use a different validator class
            case LIST:
            // we could handle OBJECT but IMO if people want to allow OBJECT to LIST conversion
            // they should use a different validator class
            case EXPRESSION:
            case TYPE:
            case UNDEFINED:
            default:
                return false;
        }
    }

}
