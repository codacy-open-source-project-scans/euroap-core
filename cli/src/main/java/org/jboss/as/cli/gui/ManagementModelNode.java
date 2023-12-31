/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.cli.gui;

import java.util.ArrayList;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * A node in the management tree.  Non-leaves are addressable entities in a DMR command.  Leaves are attributes.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class ManagementModelNode extends DefaultMutableTreeNode {

    private CliGuiContext cliGuiCtx;
    private CommandExecutor executor;
    private boolean isLeaf = false;
    private boolean isGeneric = false;

    /**
     * Constructor for root node only.
     */
    public ManagementModelNode(CliGuiContext cliGuiCtx) {
        this.cliGuiCtx = cliGuiCtx;
        this.executor = cliGuiCtx.getExecutor();
        this.isLeaf = false;
        setUserObject(new UserObject());
    }

    private ManagementModelNode(CliGuiContext cliGuiCtx, UserObject userObject) {
        this.cliGuiCtx = cliGuiCtx;
        this.executor = cliGuiCtx.getExecutor();
        this.isLeaf = userObject.isLeaf;
        this.isGeneric = userObject.isGeneric;
        if (isGeneric) setAllowsChildren(false);
        setUserObject(userObject);
    }

    /**
     * Clone as a root node
     * @return The cloned node.
     */
    @Override
    public ManagementModelNode clone() {
        UserObject toBeCloned = (UserObject)getUserObject();
        UserObject clonedUsrObj = new UserObject(toBeCloned, addressPath(), true);
        return new ManagementModelNode(cliGuiCtx, clonedUsrObj);
    }

    /**
     * Refresh children using read-resource operation.
     */
    public void explore() {
        if (isLeaf) return;
        if (isGeneric) return;
        removeAllChildren();

        try {
            String addressPath = addressPath();
            ModelNode resourceDesc = executor.doCommand(addressPath + ":read-resource-description");
            resourceDesc = resourceDesc.get("result");
            ModelNode response = executor.doCommand(addressPath + ":read-resource(include-runtime=true,include-defaults=true)");
            ModelNode result = response.get("result");
            if (!result.isDefined()) return;

            List<String> childrenTypes = getChildrenTypes(addressPath);
            for (ModelNode node : result.asList()) {
                Property prop = node.asProperty();
                if (childrenTypes.contains(prop.getName())) { // resource node
                    if (hasGenericOperations(addressPath, prop.getName())) {
                        add(new ManagementModelNode(cliGuiCtx, new UserObject(node, prop.getName())));
                    }
                    if (prop.getValue().isDefined()) {
                        for (ModelNode innerNode : prop.getValue().asList()) {
                            UserObject usrObj = new UserObject(innerNode, prop.getName(), innerNode.asProperty().getName());
                            add(new ManagementModelNode(cliGuiCtx, usrObj));
                        }
                    }
                } else { // attribute node
                    UserObject usrObj = new UserObject(node, resourceDesc, prop.getName(), prop.getValue().asString());
                    add(new ManagementModelNode(cliGuiCtx, usrObj));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean hasGenericOperations(String addressPath, String resourceName) throws Exception {
        ModelNode response = executor.doCommand(addressPath + resourceName + "=*/:read-operation-names");
        if (response.get("outcome").asString().equals("failed")) return false;

        for (ModelNode node : response.get("result").asList()) {
            if (node.asString().equals("add")) return true;
        }

        return false;
    }

    private List<String> getChildrenTypes(String addressPath) throws Exception {
        List<String> childrenTypes = new ArrayList<String>();
        ModelNode readChildrenTypes = executor.doCommand(addressPath + ":read-children-types");
        for (ModelNode type : readChildrenTypes.get("result").asList()) {
            childrenTypes.add(type.asString());
        }
        return childrenTypes;
    }

    /**
     * Get the DMR path for this node.  For leaves, the DMR path is the path of its parent.
     * @return The DMR path for this node.
     */
    public String addressPath() {
        if (isLeaf) {
            ManagementModelNode parent = (ManagementModelNode)getParent();
            return parent.addressPath();
        }

        StringBuilder builder = new StringBuilder();
        for (Object pathElement : getUserObjectPath()) {
            UserObject userObj = (UserObject)pathElement;
            if (userObj.isRoot()) { // don't want to escape root
                builder.append(userObj.getName());
                continue;
            }

            builder.append(userObj.getName());
            builder.append("=");
            builder.append(userObj.getEscapedValue());
            builder.append("/");
        }

        return builder.toString();
    }

    @Override
    public boolean isLeaf() {
        return this.isLeaf;
    }

    public boolean isGeneric() {
        return this.isGeneric;
    }

    public static String escapeAddressElement(String element) {
        element = element.replace(":", "\\:");
        element = element.replace("/", "\\/");
        element = element.replace("=", "\\=");
        element = element.replace(" ", "\\ ");
        element = element.replace("$", "\\$");
        return element;
    }

    /**
     * Encapsulates name/value pair.  Also encapsulates escaping of the value.
     */
    public class UserObject {
        private ModelNode backingNode;
        private String name;
        private String value;
        private boolean isLeaf;
        private boolean isGeneric = false;
        private boolean isRoot = false;
        private String separator;
        private AttributeDescription attribDesc = null;

        /**
         * Constructor for the root node.
         */
        public UserObject() {
            this.backingNode = new ModelNode();
            this.name = "/";
            this.value = "";
            this.isLeaf = false;
            this.isRoot = true;
            this.separator = "";
        }

        /**
         * Constructor for cloning purposes.
         * @param usrObj The object to be cloned.
         */
        public UserObject(UserObject usrObj, String addressPath, boolean isRoot) {
            if (usrObj.backingNode != null) this.backingNode = usrObj.backingNode.clone();
            this.name = addressPath;
            this.value = usrObj.value;
            this.isLeaf = usrObj.isLeaf;
            this.isRoot = isRoot;
            this.isGeneric = usrObj.isGeneric;
            this.separator = usrObj.separator;
            if (usrObj.attribDesc != null) this.attribDesc = new AttributeDescription(usrObj.attribDesc);
        }

        /**
         * Constructor for generic folder where resource=*.
         *
         * @param name The name of the resource.
         */
        public UserObject(ModelNode backingNode, String name) {
            this.backingNode = backingNode;
            this.name = name;
            this.value = "*";
            this.isLeaf = false;
            this.isGeneric = true;
            this.separator = "=";
        }

        // resource node such as subsystem=weld
        public UserObject(ModelNode backingNode, String name, String value) {
            this.backingNode = backingNode;
            this.name = name;
            this.value = value;
            this.isLeaf = false;
            this.separator = "=";
        }

        // attribute
        public UserObject(ModelNode backingNode, ModelNode resourceDesc, String name, String value) {
            this.attribDesc = new AttributeDescription(resourceDesc.get("attributes", name));
            this.backingNode = backingNode;
            this.name = name;
            this.value = value;
            this.isLeaf = true;

            if (this.attribDesc.isGraphable()) {
                this.separator = " \u2245 ";
            } else {
                this.separator = " => ";
            }
        }

        public ModelNode getBackingNode() {
            return this.backingNode;
        }

        public AttributeDescription getAttributeDescription() {
            return this.attribDesc;
        }

        public String getName() {
            return this.name;
        }

        public String getValue() {
            return this.value;
        }

        public String getEscapedValue() {
            return ManagementModelNode.escapeAddressElement(this.value);
        }

        public boolean isRoot() {
            return this.isRoot;
        }

        public boolean isLeaf() {
            return this.isLeaf;
        }

        public boolean isGeneric() {
            return this.isGeneric;
        }

        @Override
        public String toString() {
            if (isRoot) {
                return this.name;
            } else {
                return this.name + this.separator + this.value;
            }
        }
    }

    class AttributeDescription {

        private ModelNode attributes;

        AttributeDescription(ModelNode attributes) {
            this.attributes = attributes;
        }

        // Used for cloning
        AttributeDescription(AttributeDescription attrDesc) {
            this.attributes = attrDesc.attributes.clone();
        }

        /**
         * Is this a runtime attribute?
         */
        public boolean isRuntime() {
            return attributes.get("storage").asString().equals("runtime");
        }

        public ModelType getType() {
            return attributes.get("type").asType();
        }

        public boolean isGraphable() {
            return isRuntime() && isNumeric();
        }

        public boolean isNumeric() {
            ModelType type = getType();
            return (type == ModelType.BIG_DECIMAL) ||
                   (type == ModelType.BIG_INTEGER) ||
                   (type == ModelType.DOUBLE) ||
                   (type == ModelType.INT) ||
                   (type == ModelType.LONG);
        }
    }

}