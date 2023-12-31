/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.cli.operation;



/**
 * An instance of this interface represents a prefix for the operation request address part.
 *
 * @author Alexey Loubyansky
 */
public interface OperationRequestAddress extends Iterable<OperationRequestAddress.Node> {

    /**
     * Appends the node type to the current address.
     * Note, the current address must end on a node name before this method
     * is invoked.
     *
     * @param nodeType  the node type to append to the current address
     */
    void toNodeType(String nodeType);

    /**
     * Appends the node name to the current address.
     * Note, the current address must end on a node type before this method
     * is invoked.
     *
     * @param nodeName the node name to append to the current address
     */
    void toNode(String nodeName);

    /**
     * Appends the node to the current address.
     * Note, the current address must end on a node (i.e. node name) before
     * this method is invoked.
     *
     * @param nodeType  the node type of the node to append to the current address
     * @param nodeName  the node name of the node to append to the current address
     */
    void toNode(String nodeType, String nodeName);

    /**
     * Appends the path to the current address.
     * Note, the current address must end on a node (i.e. node name) before
     * this method is invoked.
     *
     * @param path  the path to append to the current address
     */
    void appendPath(OperationRequestAddress path);

    /**
     * Sets the current prefix to the node type of the current node,
     * i.e. the node name is removed from the end of the prefix.
     * @return the node name the prefix ended on
     */
    String toNodeType();

    /**
     * Removes the last node in the prefix, i.e. moves the value a node up.
     * @return the node the prefix ended on
     */
    Node toParentNode();

    /**
     * Resets the prefix, i.e. this will make the prefix empty.
     */
    void reset();

    /**
     * Checks whether the prefix ends on a node type or a node name.
     * @return  true if the prefix ends on a node type, otherwise false.
     */
    boolean endsOnType();

    /**
     * Checks whether the prefix is empty.
     * @return  true if the prefix is empty, otherwise false.
     */
    boolean isEmpty();

    /**
     * Returns the node type of the last node.
     * @return the node type of the last node or null if the prefix is empty.
     */
    String getNodeType();

    /**
     * Returns the node name of the last node.
     * @return  the node name of the last node or null if the prefix ends
     * on a type or is empty.
     */
    String getNodeName();

    /**
     * Returns the number of nodes (more specifically node types, if the address
     * ends on a type, it means the last node is not complete, but it will be
     * counted as a node by this method).
     *
     * @return  the number of nodes this address consists of
     */
    int length();

    interface Node {

        String getType();

        String getName();
    }
}
