/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.cli.gui;

import java.awt.event.MouseEvent;
import javax.swing.JTree;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.jboss.as.cli.gui.ManagementModelNode.UserObject;
import org.jboss.dmr.ModelNode;

/**
 * JTree that knows how to find context-sensitive help and display as ToolTip for
 * each node.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class CommandBuilderTree extends JTree {
    private CliGuiContext cliGuiCtx;
    private ManagementModelNode currentNode = null;
    private String currentDescription = null;

    public CommandBuilderTree(CliGuiContext cliGuiCtx, TreeModel model) {
        super(model);
        this.cliGuiCtx = cliGuiCtx;
        setToolTipText(""); // enables toolTip system for this tree
    }

    @Override
    public synchronized String getToolTipText(MouseEvent me) {
        if (getRowForLocation(me.getX(), me.getY()) == -1) {
            currentNode = null;
            currentDescription = null;
            return null;
        }

        TreePath treePath = getPathForLocation(me.getX(), me.getY());
        ManagementModelNode node = (ManagementModelNode)treePath.getLastPathComponent();

        // don't read description again when mouse is moved within the same node
        if (node == currentNode) return currentDescription;

        currentNode = node;
        currentDescription = null;

        try {
            ModelNode readResource = cliGuiCtx.getExecutor().doCommand(node.addressPath() + ":read-resource-description");
            UserObject usrObj = (UserObject)node.getUserObject();
            if (node.isGeneric()) {
                currentDescription = "Used for generic operations on " + usrObj.getName() + ", such as 'add'";
            } else if (!node.isLeaf()) {
                currentDescription = readResource.get("result", "description").asString();
            } else {
                ModelNode description = readResource.get("result", "attributes", usrObj.getName(), "description");
                if (description.isDefined()) {
                    currentDescription = description.asString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return currentDescription;
    }

}
