/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jmx.model;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.ObjectMapAttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.jmx.logging.JmxLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.dmr.ValueExpression;

/**
 * Converts between Open MBean types/data and ModelController types/data
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class TypeConverters {

    static final Pattern VAULT_PATTERN = Pattern.compile("\\$\\{VAULT::.*::.*::.*\\}");

    private static final SimpleTypeConverter BIG_DECIMAL_NO_EXPR = new SimpleTypeConverter(BigDecimalValueAccessor.INSTANCE, false);
    private static final SimpleTypeConverter BIG_DECIMAL_EXPR = new SimpleTypeConverter(BigDecimalValueAccessor.INSTANCE, true);
    private static final SimpleTypeConverter BIG_INTEGER_NO_EXPR = new SimpleTypeConverter(BigIntegerValueAccessor.INSTANCE, false);
    private static final SimpleTypeConverter BIG_INTEGER_EXPR = new SimpleTypeConverter(BigIntegerValueAccessor.INSTANCE, true);
    private static final SimpleTypeConverter BOOLEAN_NO_EXPR = new SimpleTypeConverter(BooleanValueAccessor.INSTANCE, false);
    private static final SimpleTypeConverter BOOLEAN_EXPR = new SimpleTypeConverter(BooleanValueAccessor.INSTANCE, true);
    private static final SimpleTypeConverter BYTES_NO_EXPR = new SimpleTypeConverter(BytesValueAccessor.INSTANCE, false);
    private static final SimpleTypeConverter DOUBLE_NO_EXPR = new SimpleTypeConverter(DoubleValueAccessor.INSTANCE, false);
    private static final SimpleTypeConverter DOUBLE_EXPR = new SimpleTypeConverter(DoubleValueAccessor.INSTANCE, true);
    private static final SimpleTypeConverter STRING_NO_EXPR = new SimpleTypeConverter(StringValueAccessor.INSTANCE, false);
    private static final SimpleTypeConverter STRING_EXPR = new SimpleTypeConverter(StringValueAccessor.INSTANCE, true);
    private static final SimpleTypeConverter PROPERTY_NO_EXPR = new SimpleTypeConverter(StringValueAccessor.INSTANCE, false);
    private static final SimpleTypeConverter INT_NO_EXPR = new SimpleTypeConverter(IntegerValueAccessor.INSTANCE, false);
    private static final SimpleTypeConverter INT_EXPR = new SimpleTypeConverter(IntegerValueAccessor.INSTANCE, true);
    private static final SimpleTypeConverter LONG_NO_EXPR = new SimpleTypeConverter(LongValueAccessor.INSTANCE, false);
    private static final SimpleTypeConverter LONG_EXPR = new SimpleTypeConverter(LongValueAccessor.INSTANCE, true);
    private static final SimpleTypeConverter TYPE_NO_EXPR = new SimpleTypeConverter(ModelTypeValueAccessor.INSTANCE, false);
    private static final SimpleTypeConverter TYPE_EXPR = new SimpleTypeConverter(ModelTypeValueAccessor.INSTANCE, true);
    private static final SimpleTypeConverter UNDEFINED_NO_EXPR = new SimpleTypeConverter(UndefinedValueAccessor.INSTANCE, false);
    private static final SimpleTypeConverter UNDEFINED_EXPR = new SimpleTypeConverter(UndefinedValueAccessor.INSTANCE, true);

    private final boolean expressions;
    //Older versions simply used a DMR string for PROPERTY types
    private final boolean legacyWithProperPropertyFormat;

    private TypeConverters(boolean expressions, boolean legacyWithProperPropertyFormat) {
        this.expressions = expressions;
        this.legacyWithProperPropertyFormat = legacyWithProperPropertyFormat;
    }

    public static TypeConverters createLegacyTypeConverters(boolean properPropertyFormat) {
        return new TypeConverters(false, properPropertyFormat);
    }

    public static TypeConverters createExpressionTypeConverters() {
        return new TypeConverters(true, true);
    }

    OpenType<?> convertToMBeanType(AttributeDefinition attributeDefinition, final ModelNode description) {
        return getConverter(attributeDefinition, description).getOpenType();
    }

    ModelNode toModelNode(AttributeDefinition attributeDefinition, final ModelNode description, final Object value) {
        ModelNode node = new ModelNode();
        if (value == null) {
            return node;
        }
        return getConverter(attributeDefinition, description).toModelNode(value);
    }

    Object fromModelNode(AttributeDefinition attributeDefinition, final ModelNode description, final ModelNode value) {
        if (value == null || !value.isDefined()) {
            return null;
        }
        return getConverter(attributeDefinition, description).fromModelNode(value);
    }

    private ModelType getType(ModelNode typeNode) {
        if (typeNode == null) {
            return ModelType.UNDEFINED;
        }
        try {
            return ModelType.valueOf(typeNode.toString());
        } catch (RuntimeException e) {
            return null;
        }
    }

    // TODO WFCORE-3551 stop using the full attribute description for this, particularly
    // for non-OBJECT/LIST/PROPERTY where all we need is the ModelType
    TypeConverter getConverter(AttributeDefinition attributeDefinition, ModelNode description) {
        return getConverter(
                attributeDefinition,
                description.hasDefined(TYPE) ? description.get(TYPE) : null,
                description.hasDefined(VALUE_TYPE) ? description.get(VALUE_TYPE) : null);
    }

    TypeConverter getConverter(AttributeDefinition attributeDefinition, ModelType modelType, ModelNode valueTypeNode) {
        switch (modelType) {
            case BIG_DECIMAL:
                return expressions ? BIG_DECIMAL_EXPR : BIG_DECIMAL_NO_EXPR;
            case BIG_INTEGER:
                return expressions ? BIG_INTEGER_EXPR : BIG_INTEGER_NO_EXPR;
            case BOOLEAN:
                return expressions ? BOOLEAN_EXPR : BOOLEAN_NO_EXPR;
            case BYTES:
                //Allowing expressions for byte[] seems pointless
                return BYTES_NO_EXPR;
            case DOUBLE:
                return expressions ? DOUBLE_EXPR : DOUBLE_NO_EXPR;
            case STRING:
                return expressions ? STRING_EXPR : STRING_NO_EXPR;
            case PROPERTY:
                //For the legacy setup properties are converted to a dmr string
                //For the expr setup or legacy with legacyWithProperPropertyFormat=true we use a composite type
                return expressions || legacyWithProperPropertyFormat ? new PropertyTypeConverter(attributeDefinition, valueTypeNode) : PROPERTY_NO_EXPR;
            case INT:
                return expressions ? INT_EXPR : INT_NO_EXPR;
            case LONG:
                return expressions ? LONG_EXPR : LONG_NO_EXPR;
            case TYPE:
                return expressions ? TYPE_EXPR : TYPE_NO_EXPR;
            case UNDEFINED:
                return expressions ? UNDEFINED_EXPR : UNDEFINED_NO_EXPR;
            case OBJECT:
                return new ObjectTypeConverter(attributeDefinition, valueTypeNode);
            case LIST:
                return new ListTypeConverter(attributeDefinition, valueTypeNode);
            default:
                throw JmxLogger.ROOT_LOGGER.unknownType(modelType);
        }
    }

    private TypeConverter getConverter(AttributeDefinition attributeDefinition, ModelNode typeNode, ModelNode valueTypeNode) {
        ModelType modelType = getType(typeNode);
        if (modelType == null) {
            return new ComplexTypeConverter(attributeDefinition, typeNode);
        }
        return getConverter(attributeDefinition, modelType, valueTypeNode);
    }

    private static ModelNode nullNodeAsUndefined(ModelNode node) {
        if (node == null) {
            return new ModelNode();
        }
        return node;
    }

    interface TypeConverter {
        OpenType<?> getOpenType();
        Object fromModelNode(final ModelNode node);
        ModelNode toModelNode(final Object o);
        Object[] toArray(final List<Object> list);
    }


    private static class SimpleTypeConverter implements TypeConverter {
        private final SimpleValueAccessor valueAccessor;
        private final boolean expressions;


        public SimpleTypeConverter(SimpleValueAccessor valueAccessor, boolean expressions) {
            this.valueAccessor = valueAccessor;
            this.expressions = expressions;
        }

        @Override
        public OpenType<?> getOpenType() {
            if (!expressions) {
                return valueAccessor.getOpenType();
            } else {
                return SimpleType.STRING;
            }

        }

        @Override
        public Object fromModelNode(final ModelNode node) {
            if (node == null || !node.isDefined() || node.asString().isEmpty()) {
                return null;
            }
            if (!expressions || valueAccessor == UndefinedValueAccessor.INSTANCE) {
                if (!expressions && node.getType() == ModelType.EXPRESSION) {
                    if (!VAULT_PATTERN.matcher(node.asString()).matches()) {
                        ModelNode resolved;
                        try {
                            resolved = ExpressionResolver.SIMPLE.resolveExpressions(node);
                        } catch (OperationFailedException e) {
                            throw new IllegalArgumentException(e);
                        }
                        return valueAccessor.fromModelNode(resolved);
                    }
                }
                return valueAccessor.fromModelNode(node);

            } else {
                return node.asString();
            }
        }

        @Override
        public ModelNode toModelNode(final Object o) {
            if (o == null) {
                return new ModelNode();
            }
            boolean possibleExpression = false;
            if (o instanceof String) {
                possibleExpression = isPossibleExpression((String)o);
            }
            if (expressions) {
                if (possibleExpression) {
                    return new ModelNode().set(new ValueExpression((String)o));
                }
                if (o instanceof String) {
                    return valueAccessor.toModelNodeFromString((String) o);
                }
                return valueAccessor.toModelNode(o);
            } else {
                if (possibleExpression) {
                    if (valueAccessor != StringValueAccessor.INSTANCE) {
                        throw JmxLogger.ROOT_LOGGER.expressionCannotBeConvertedIntoTargeteType(valueAccessor.getOpenType());
                    }
                    return new ModelNode().set(new ValueExpression((String)o));
                }
                return valueAccessor.toModelNode(o);
            }
        }

        private boolean isPossibleExpression(String s) {
            int start = s.indexOf("${");
            return start != -1 && s.indexOf('}', start) != -1;
        }

        public Object[] toArray(final List<Object> list) {
            if (expressions) {
                return list.toArray(new String[list.size()]);
            } else {
                return valueAccessor.toArray(list);
            }
        }
    }

    private class ObjectTypeConverter implements TypeConverter {

        private final AttributeDefinition attributeDefinition;
        final ModelNode valueTypeNode;
        final ModelType valueType;
        final boolean mapOfMaps;
        OpenType<?> openType;

        ObjectTypeConverter(AttributeDefinition attributeDefinition, ModelNode valueTypeNode) {
            this.attributeDefinition = attributeDefinition;
            this.valueTypeNode = nullNodeAsUndefined(valueTypeNode);
            ModelType valueType = getType(valueTypeNode);
            this.valueType = valueType == ModelType.UNDEFINED ? null : valueType;
            mapOfMaps = attributeDefinition instanceof ObjectMapAttributeDefinition;
        }

        @Override
        public OpenType<?>  getOpenType() {
            if (openType != null) {
                return openType;
            }

            openType = getConverter(attributeDefinition, valueTypeNode, null).getOpenType();
            if ((valueType == null && !mapOfMaps) && (openType instanceof CompositeType || !valueTypeNode.isDefined())) {
                //For complex value types that are not maps of maps just return the composite type
                return openType;
            }
            try {

                if (!(openType instanceof CompositeType)) {
                    CompositeType rowType = new CompositeType(
                            JmxLogger.ROOT_LOGGER.compositeEntryTypeName(),
                            JmxLogger.ROOT_LOGGER.compositeEntryTypeDescription(),
                            new String[]{"key", "value"},
                            new String[]{JmxLogger.ROOT_LOGGER.compositeEntryKeyDescription(), JmxLogger.ROOT_LOGGER.compositeEntryValueDescription()},
                            new OpenType[]{SimpleType.STRING, openType});
                    openType = new TabularType(JmxLogger.ROOT_LOGGER.compositeMapName(), JmxLogger.ROOT_LOGGER.compositeMapDescription(), rowType, new String[]{"key"});
                } else if (mapOfMaps) {
                    CompositeType rowType = (CompositeType) openType;
                    CompositeType wrapperType = new CompositeType(
                            JmxLogger.ROOT_LOGGER.compositeEntryTypeName(),
                            JmxLogger.ROOT_LOGGER.compositeEntryTypeDescription(),
                            new String[]{"key", "value"},
                            new String[]{JmxLogger.ROOT_LOGGER.compositeEntryKeyDescription(), JmxLogger.ROOT_LOGGER.compositeEntryValueDescription()},
                            new OpenType[]{SimpleType.STRING, rowType});
                    openType = new TabularType(JmxLogger.ROOT_LOGGER.compositeMapName(), JmxLogger.ROOT_LOGGER.compositeMapDescription(), wrapperType, new String[]{"key"});
                } else {
                    JmxLogger.ROOT_LOGGER.debugf("Attribute is not a composite type, or a mapOfMaps: %s", openType);
                }
                return openType;
            } catch (OpenDataException e1) {
                throw new RuntimeException(e1);
            }
        }

        @Override
        public Object fromModelNode(final ModelNode node) {
            if (node == null || !node.isDefined()) {
                return null;
            }
            if (valueType != null) {
                return fromSimpleModelNode(node);
            } else {
                if (mapOfMaps) {
                    return fromMapOfMapsModelNode(node);
                } else {
                    TypeConverter converter = getConverter(attributeDefinition, valueTypeNode, null);
                    return converter.fromModelNode(node);
                }
            }
        }

        Object fromSimpleModelNode(final ModelNode node) {
            final TabularType tabularType = (TabularType)getOpenType();
            final TabularDataSupport tabularData = new TabularDataSupport(tabularType);
            final Map<String, ModelNode> values = new HashMap<String, ModelNode>();
            final List<Property> properties = node.isDefined() ? node.asPropertyList() : null;
            if (properties != null) {
                for (Property prop : properties) {
                    values.put(prop.getName(), prop.getValue());
                }
            }

            final TypeConverter converter = getConverter(attributeDefinition, valueTypeNode, null);
            for (Map.Entry<String, ModelNode> prop : values.entrySet()) {
                Map<String, Object> rowData = new HashMap<String, Object>();
                rowData.put("key", prop.getKey());
                rowData.put("value", converter.fromModelNode(prop.getValue()));
                try {
                    tabularData.put(new CompositeDataSupport(tabularType.getRowType(), rowData));
                } catch (OpenDataException e) {
                    throw new RuntimeException(e);
                }
            }
            return tabularData;
        }

        Object fromMapOfMapsModelNode(ModelNode node) {
            final TabularType tabularType = (TabularType)getOpenType();
            final TabularDataSupport tabularData = new TabularDataSupport(tabularType);
            final Map<String, ModelNode> values = new HashMap<String, ModelNode>();
            final List<Property> properties = node.isDefined() ? node.asPropertyList() : null;
            if (properties != null) {
                for (Property prop : properties) {
                    values.put(prop.getName(), prop.getValue());
                }
            }

            final TypeConverter converter = getConverter(attributeDefinition, valueTypeNode, null);
            for (Map.Entry<String, ModelNode> prop : values.entrySet()) {
                String key = prop.getKey();
                ModelNode value = prop.getValue();

                CompositeData valueData = (CompositeData)converter.fromModelNode(value);

                Map<String, Object> rowData = new HashMap<>();
                rowData.put("key", key);
                rowData.put("value", valueData);

                try {
                    tabularData.put(new CompositeDataSupport(tabularType.getRowType(), rowData));
                } catch (OpenDataException e) {
                    throw new RuntimeException(e);
                }
            }
            return tabularData;
        }

        @Override
        public ModelNode toModelNode(Object o) {
            if (o == null) {
                return new ModelNode();
            }
            final TypeConverter converter = getConverter(attributeDefinition, valueTypeNode, null);
            if (valueType == null) {
                //complex
                if (mapOfMaps) {
                    final ModelNode node = new ModelNode();
                    for (Map.Entry<String, Object> entry : ((Map<String, Object>)o).entrySet()) {
                        entry = convertTabularTypeEntryToMapEntry(entry);
                        String mapKey = entry.getKey();
                        CompositeDataSupport data = (CompositeDataSupport) entry.getValue();
                        ModelNode valueNode = converter.toModelNode(entry.getValue());
                        node.get(mapKey).set(valueNode);
                    }
                    return node;

                } else {
                    return converter.toModelNode(o);
                }
            } else {
                //map
                final ModelNode node = new ModelNode();
                for (Map.Entry<String, Object> entry : ((Map<String, Object>)o).entrySet()) {
                    entry = convertTabularTypeEntryToMapEntry(entry);
                    node.get(entry.getKey()).set(converter.toModelNode(entry.getValue()));
                }
                return node;
            }
        }

        @Override
        public Object[] toArray(List<Object> list) {
            if (getOpenType() == SimpleType.STRING) {
                return list.toArray(new String[list.size()]);
            }
            return list.toArray(new Object[list.size()]);
        }

        //TODO this may go depending on if we want to force tabular types only
        private Map.Entry<String, Object> convertTabularTypeEntryToMapEntry(final Map.Entry<?, Object> entry) {
            if (entry.getKey() instanceof List) {
                //It comes from a TabularType
                return new Map.Entry<String, Object>() {

                    @Override
                    public String getKey() {
                        List<?> keyList = (List<?>)entry.getKey();
                        if (keyList.size() != 1) {
                            throw JmxLogger.ROOT_LOGGER.invalidKey(keyList, entry);
                        }
                        return (String)keyList.get(0);
                    }

                    @Override
                    public Object getValue() {
                        return ((CompositeDataSupport)entry.getValue()).get("value");
                    }

                    @Override
                    public Object setValue(Object value) {
                        throw new UnsupportedOperationException();
                    }

                };
            } else {
                return new Map.Entry<String, Object>() {

                    @Override
                    public String getKey() {
                        return (String)entry.getKey();
                    }

                    @Override
                    public Object getValue() {
                        return entry.getValue();
                    }

                    @Override
                    public Object setValue(Object value) {
                        throw new UnsupportedOperationException();
                    }
                };
            }

        }
    }

    private class ListTypeConverter implements TypeConverter {
        private final AttributeDefinition attributeDefinition;
        final ModelNode valueTypeNode;

        ListTypeConverter(AttributeDefinition attributeDefinition, ModelNode valueTypeNode) {
            this.attributeDefinition = attributeDefinition;
            this.valueTypeNode = nullNodeAsUndefined(valueTypeNode);
        }

        @Override
        public OpenType<?> getOpenType() {
            try {
                return ArrayType.getArrayType(getConverter(attributeDefinition, valueTypeNode, null).getOpenType());
            } catch (OpenDataException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Object fromModelNode(final ModelNode node) {
            if (node == null || !node.isDefined()) {
                return null;
            }
            final List<Object> list = new ArrayList<Object>();
            final TypeConverter converter = getConverter(attributeDefinition, valueTypeNode, null);
            for (ModelNode element : node.asList()) {
                list.add(converter.fromModelNode(element));
            }
            return converter.toArray(list);
        }

        @Override
        public ModelNode toModelNode(Object o) {
            if (o == null) {
                return new ModelNode();
            }
            ModelNode node = new ModelNode();
            final TypeConverter converter = getConverter(attributeDefinition, valueTypeNode, null);
            for (Object value : (Object[])o) {
                node.add(converter.toModelNode(value));
            }
            return node;
        }

        @Override
        public Object[] toArray(List<Object> list) {
            return null;
        }
    }

    private class ComplexTypeConverter implements TypeConverter {
        private final AttributeDefinition attributeDefinition;
        final ModelNode typeNode;

        ComplexTypeConverter(AttributeDefinition attributeDefinition, final ModelNode typeNode) {
            this.attributeDefinition = attributeDefinition;
            this.typeNode = nullNodeAsUndefined(typeNode);
        }

        @Override
        public OpenType<?> getOpenType() {
            List<String> itemNames = new ArrayList<String>();
            List<String> itemDescriptions = new ArrayList<String>();
            List<OpenType<?>> itemTypes = new ArrayList<OpenType<?>>();

            //Some of the common operation descriptions use value-types like "The type will be that of the attribute found".
            if (!typeNode.isDefined() || typeNode.getType() == ModelType.STRING) {
                return SimpleType.STRING;
            }

            for (String name : typeNode.keys()) {
                ModelNode current = typeNode.get(name);
                itemNames.add(name);
                String description = null;
                if (!current.hasDefined(DESCRIPTION)) {
                    description = "-";
                }
                else {
                    description = current.get(DESCRIPTION).asString().trim();
                    if (description.length() == 0) {
                        description = "-";
                    }
                }

                itemDescriptions.add(getDescription(current));
                itemTypes.add(getConverter(attributeDefinition, current).getOpenType());
            }
            try {
                return new CompositeType(JmxLogger.ROOT_LOGGER.complexCompositeEntryTypeName(),
                        JmxLogger.ROOT_LOGGER.complexCompositeEntryTypeDescription(),
                        itemNames.toArray(new String[itemNames.size()]),
                        itemDescriptions.toArray(new String[itemDescriptions.size()]),
                        itemTypes.toArray(new OpenType[itemTypes.size()]));
            } catch (OpenDataException e) {
                throw new RuntimeException(e);
            }
        }

        String getDescription(ModelNode node) {
            if (!node.hasDefined(DESCRIPTION)) {
                return "-";
            }
            String description = node.get(DESCRIPTION).asString();
            if (description.trim().length() == 0) {
                return "-";
            }
            return description;
        }

        @Override
        public Object fromModelNode(ModelNode node) {
            if (node == null || !node.isDefined()) {
                return null;
            }

            final OpenType<?> openType = getOpenType();
            if (openType instanceof CompositeType) {
                final CompositeType compositeType = (CompositeType)openType;
                //Create a composite
                final Map<String, Object> items = new HashMap<String, Object>();
                for (String attrName : compositeType.keySet()) {
                    TypeConverter converter = getConverter(attributeDefinition, typeNode.get(attrName, TYPE), typeNode.get(attrName, VALUE_TYPE));
                    items.put(attrName, converter.fromModelNode(node.get(attrName)));
                }

                try {
                    return new CompositeDataSupport(compositeType, items);
                } catch (OpenDataException e) {
                    throw new RuntimeException(e);
                }
            } else {
                return node.toJSONString(false);
            }
        }

        @Override
        public ModelNode toModelNode(Object o) {
            if (o == null) {
                return new ModelNode();
            }
            if (o instanceof CompositeData) {
                final ModelNode node = new ModelNode();
                final CompositeData composite = (CompositeData)o;
                for (String key : composite.getCompositeType().keySet()) {
                    if (!typeNode.hasDefined(key)){
                        throw JmxLogger.ROOT_LOGGER.unknownValue(key);
                    }
                    TypeConverter converter = getConverter(attributeDefinition, typeNode.get(key, TYPE), typeNode.get(key, VALUE_TYPE));
                    node.get(key).set(converter.toModelNode(composite.get(key)));
                }
                return node;
            } else {
                return ModelNode.fromJSONString((String)o);
            }
        }

        @Override
        public Object[] toArray(List<Object> list) {
            return list.toArray(new CompositeData[list.size()]);
        }
    }

    private class PropertyTypeConverter implements TypeConverter {
        private final AttributeDefinition attributeDefinition;
        final ModelNode typeNode;

        public PropertyTypeConverter(AttributeDefinition attributeDefinition, ModelNode typeNode) {
            this.attributeDefinition = attributeDefinition;
            this.typeNode = typeNode;
        }

        @Override
        public CompositeType getOpenType() {
            try {
                return new CompositeType(
                        "property",
                        JmxLogger.ROOT_LOGGER.propertyCompositeType(),
                        new String[] {"name", "value"},
                        new String[] { JmxLogger.ROOT_LOGGER.propertyName(), JmxLogger.ROOT_LOGGER.propertyValue()},
                        new OpenType[] {SimpleType.STRING, getConverter().getOpenType()});
            } catch (OpenDataException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Object fromModelNode(ModelNode node) {
            Property prop = node.asProperty();
            Map<String, Object> items = new HashMap<String, Object>();
            items.put("name", prop.getName());
            items.put("value", getConverter().fromModelNode(prop.getValue()));
            try {
                return new CompositeDataSupport(getOpenType(), items);
            } catch (OpenDataException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public ModelNode toModelNode(Object o) {
            CompositeData data = (CompositeData)o;
            String name = (String)data.get("name");
            ModelNode value = getConverter().toModelNode(data.get("value"));
            ModelNode node = new ModelNode();
            node.set(name, value);
            return node;
        }

        @Override
        public Object[] toArray(List<Object> list) {
            return list.toArray(new Object[list.size()]);
        }

        private TypeConverter getConverter() {
            if (typeNode == null) {
                return expressions ? STRING_EXPR : STRING_NO_EXPR;
            }
            return TypeConverters.this.getConverter(attributeDefinition, typeNode, null);
        }
    }

    private abstract static class SimpleValueAccessor {
        abstract OpenType<?> getOpenType();
        abstract Object fromModelNode(ModelNode node);
        abstract ModelNode toModelNode(Object o);
        abstract ModelNode toModelNodeFromString(String s);
        abstract Object[] toArray(final List<Object> list);
    }

    private static class BigDecimalValueAccessor extends SimpleValueAccessor {
        static final BigDecimalValueAccessor INSTANCE = new BigDecimalValueAccessor();

        OpenType<?> getOpenType() {
            return SimpleType.BIGDECIMAL;
        }

        Object fromModelNode(final ModelNode node) {
            return node.asBigDecimal();
        }

        ModelNode toModelNode(final Object o) {
            return new ModelNode().set((BigDecimal)o);
        }

        Object[] toArray(final List<Object> list) {
            return list.toArray(new BigDecimal[list.size()]);
        }

        @Override
        ModelNode toModelNodeFromString(String s) {
            return new ModelNode(new BigDecimal(s));
        }
    }

    private static class BigIntegerValueAccessor extends SimpleValueAccessor {
        static final BigIntegerValueAccessor INSTANCE = new BigIntegerValueAccessor();

        OpenType<?> getOpenType() {
            return SimpleType.BIGINTEGER;
        }

        Object fromModelNode(final ModelNode node) {
            return node.asBigInteger();
        }

        ModelNode toModelNode(final Object o) {
            return new ModelNode().set((BigInteger)o);
        }

        Object[] toArray(final List<Object> list) {
            return list.toArray(new BigInteger[list.size()]);
        }

        @Override
        ModelNode toModelNodeFromString(String s) {
            return new ModelNode(new BigInteger(s));
        }
    }

    private static class BooleanValueAccessor extends SimpleValueAccessor {
        static final BooleanValueAccessor INSTANCE = new BooleanValueAccessor();

        OpenType<?> getOpenType() {
            return SimpleType.BOOLEAN;
        }

        Object fromModelNode(final ModelNode node) {
            return node.asBoolean();
        }

        ModelNode toModelNode(final Object o) {
            return new ModelNode().set((Boolean)o);
        }

        Object[] toArray(final List<Object> list) {
            return list.toArray(new Boolean[list.size()]);
        }

        @Override
        ModelNode toModelNodeFromString(String s) {
            return new ModelNode(Boolean.valueOf(s));
        }
    }

    private static class BytesValueAccessor extends SimpleValueAccessor {
        static final BytesValueAccessor INSTANCE = new BytesValueAccessor();
        static final ArrayType<byte[]> ARRAY_TYPE = ArrayType.getPrimitiveArrayType(byte[].class);

        OpenType<?> getOpenType() {
            return ARRAY_TYPE;
        }

        Object fromModelNode(final ModelNode node) {
            // The only use of this is BYTES_NO_EXPR so skip resolution.
            // If that changes, use ExpressionResolver.SIMPLER I guess
            //return node.resolve().asBytes();
            return node.asBytes();
        }

        ModelNode toModelNode(final Object o) {
            return new ModelNode().set((byte[])o);
        }

        Object[] toArray(final List<Object> list) {
            return list.toArray(new byte[list.size()][]);
        }

        @Override
        ModelNode toModelNodeFromString(String s) {
            return new ModelNode(s);
        }
    }

    private static class DoubleValueAccessor extends SimpleValueAccessor {
        static final DoubleValueAccessor INSTANCE = new DoubleValueAccessor();

        OpenType<?> getOpenType() {
            return SimpleType.DOUBLE;
        }

        Object fromModelNode(final ModelNode node) {
            return node.asDouble();
        }

        ModelNode toModelNode(final Object o) {
            return new ModelNode().set((Double)o);
        }

        Object[] toArray(final List<Object> list) {
            return list.toArray(new Double[list.size()]);
        }

        @Override
        ModelNode toModelNodeFromString(String s) {
            return new ModelNode(Double.valueOf(s));
        }
    }


    private static class IntegerValueAccessor extends SimpleValueAccessor {
        static final IntegerValueAccessor INSTANCE = new IntegerValueAccessor();

        OpenType<?> getOpenType() {
            return SimpleType.INTEGER;
        }

        Object fromModelNode(final ModelNode node) {
            return node.asInt();
        }

        ModelNode toModelNode(final Object o) {
            return new ModelNode().set((Integer)o);
        }

        Object[] toArray(final List<Object> list) {
            return list.toArray(new Integer[list.size()]);
        }

        @Override
        ModelNode toModelNodeFromString(String s) {
            return new ModelNode(Integer.valueOf(s));
        }
    }

    private static class StringValueAccessor extends SimpleValueAccessor {
        static final StringValueAccessor INSTANCE = new StringValueAccessor();

        OpenType<?> getOpenType() {
            return SimpleType.STRING;
        }

        Object fromModelNode(final ModelNode node) {
            return node.asString();
        }

        ModelNode toModelNode(final Object o) {
            if (o == null) {
                return new ModelNode();
            }
            return new ModelNode().set((String)o);
        }

        Object[] toArray(final List<Object> list) {
            return list.toArray(new String[list.size()]);
        }

        @Override
        ModelNode toModelNodeFromString(String s) {
            return new ModelNode(s);
        }
    }

    private static class UndefinedValueAccessor extends SimpleValueAccessor {
        static final UndefinedValueAccessor INSTANCE = new UndefinedValueAccessor();

        OpenType<?> getOpenType() {
            return SimpleType.STRING;
        }

        Object fromModelNode(final ModelNode node) {
            return node.toJSONString(false);
        }

        ModelNode toModelNode(final Object o) {
            return toModelNodeFromString((String)o);
        }

        Object[] toArray(final List<Object> list) {
            return list.toArray(new String[list.size()]);
        }

        @Override
        ModelNode toModelNodeFromString(String s) {
            return ModelNode.fromJSONString(s);
        }
    }

    private static class LongValueAccessor extends SimpleValueAccessor {
        static final LongValueAccessor INSTANCE = new LongValueAccessor();

        OpenType<?> getOpenType() {
            return SimpleType.LONG;
        }

        Object fromModelNode(final ModelNode node) {
            return node.asLong();
        }

        ModelNode toModelNode(final Object o) {
            return new ModelNode().set((Long)o);
        }

        Object[] toArray(final List<Object> list) {
            return list.toArray(new Long[list.size()]);
        }

        @Override
        ModelNode toModelNodeFromString(String s) {
            return new ModelNode(Long.valueOf(s));
        }
    }

    private static class ModelTypeValueAccessor extends SimpleValueAccessor {
        static final ModelTypeValueAccessor INSTANCE = new ModelTypeValueAccessor();

        OpenType<?> getOpenType() {
            return SimpleType.STRING;
        }

        Object fromModelNode(final ModelNode node) {
            return node.asString();
        }

        ModelNode toModelNode(final Object o) {
            return toModelNodeFromString((String)o);
        }

        Object[] toArray(final List<Object> list) {
            return list.toArray(new String[list.size()]);
        }

        @Override
        ModelNode toModelNodeFromString(String s) {
            return new ModelNode().set(ModelType.valueOf(s));
        }
    }
}
