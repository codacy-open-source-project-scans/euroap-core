/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.cli.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.jboss.as.cli.gui.ManagementModelNode.AttributeDescription;
import org.jboss.as.cli.gui.ManagementModelNode.UserObject;

/**
 * This class contains a JTree view of the management model that allows you to build commands by
 * clicking nodes and operations.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class ManagementModel extends JPanel {
    private static final String GENERAL_HELP_TEXT = "Right-click a node to choose an operation.  Close/Open a folder to refresh.  Hover for help.";
    private static final String FILTER_TOOLTIP_HELP = "Display the root nodes containing the given text.";

    private CliGuiContext cliGuiCtx;

    private JTree tree;

    // used for the true root '/'
    public ManagementModel(CliGuiContext cliGuiCtx) {
        this(new ManagementModelNode(cliGuiCtx), cliGuiCtx);
    }

    public ManagementModel(ManagementModelNode root, CliGuiContext cliGuiCtx) {
        this.cliGuiCtx = cliGuiCtx;
        setLayout(new BorderLayout(10,10));
        this.tree = makeTree(root);
        add(new JLabel(GENERAL_HELP_TEXT), BorderLayout.NORTH);
        add(new JScrollPane(tree), BorderLayout.CENTER);
        add(makeFilterPanel(tree), BorderLayout.SOUTH);
    }

    /**
     * Find a node in the tree.  The node must be "visible" to be found.
     * @param address The full address of the node matching ManagementModelNode.addressPath()
     * @return The node, or null if not found.
     */
    public ManagementModelNode findNode(String address) {
        ManagementModelNode root = (ManagementModelNode)tree.getModel().getRoot();
        Enumeration<javax.swing.tree.TreeNode> allNodes = root.depthFirstEnumeration();
        while (allNodes.hasMoreElements()) {
            ManagementModelNode node = (ManagementModelNode)allNodes.nextElement();
            if (node.addressPath().equals(address)) return node;
        }

        return null;
    }

    private JPanel makeFilterPanel(JTree tree) {
        Box filterBox = Box.createHorizontalBox();
        JLabel filterLabel = new JLabel("Filter:");
        filterLabel.setToolTipText(FILTER_TOOLTIP_HELP);
        filterBox.add(filterLabel);
        filterBox.add(Box.createHorizontalStrut(5));
        JTextField filterInput = new JTextField(30);

        filterBox.add(filterInput);
        JButton clearButton = new JButton("Clear");
        clearButton.setToolTipText("Clear the filter");
        clearButton.addActionListener(new ClearFilterListener(filterInput));
        clearButton.setEnabled(false);

        filterInput.getDocument().addDocumentListener(new FilterDocumentListener(tree, clearButton));

        filterBox.add(clearButton);

        // Make filterBox half of width of panel
        JPanel filterPanel = new JPanel(new GridLayout(1,2));
        filterPanel.add(filterBox);
        filterPanel.add(Box.createGlue());
        return filterPanel;
    }

    private JTree makeTree(ManagementModelNode root) {
        root.explore();
        JTree tree = new CommandBuilderTree(cliGuiCtx, new DefaultTreeModel(root));
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeExpansionListener(new ManagementTreeExpansionListener((DefaultTreeModel) tree.getModel()));
        tree.addTreeSelectionListener(new ManagementTreeSelectionListener());
        tree.addMouseListener(new ManagementTreeMouseListener(tree));
        return tree;
    }

    /**
     * Get the node that has been selected by the user, or null if
     * nothing is selected.
     * @return The node or <code>null</code>
     */
    public ManagementModelNode getSelectedNode() {
        if (tree.getSelectionPath() == null) return null;
        return (ManagementModelNode)tree.getSelectionPath().getLastPathComponent();
    }

    private class ClearFilterListener implements ActionListener {
        private JTextField filterInput;

        ClearFilterListener(JTextField filterInput) {
            this.filterInput = filterInput;
        }

        public void actionPerformed(ActionEvent e) {
            filterInput.setText("");
        }
    }

    private class FilterDocumentListener implements DocumentListener {
        private JTree tree;
        private JButton clearButton;

        FilterDocumentListener(JTree tree, JButton clearButton) {
            this.tree = tree;
            this.clearButton = clearButton;
        }

        public void insertUpdate(DocumentEvent e) {
            changedUpdate(e);
        }

        public void removeUpdate(DocumentEvent e) {
            changedUpdate(e);
        }

        public void changedUpdate(DocumentEvent e) {
            String fieldText = null;
            try {
                fieldText = e.getDocument().getText(0, e.getDocument().getLength());
            } catch (BadLocationException ble) {
                ble.printStackTrace(); // should never happen
                return;
            }

            clearButton.setEnabled(fieldText.length() > 0);

            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            ManagementModelNode root = (ManagementModelNode)model.getRoot();
            root.explore(); // refresh all children

            Enumeration nodes = root.children();
            List<ManagementModelNode> removeList = new ArrayList<ManagementModelNode>();
            while (nodes.hasMoreElements()) {
                ManagementModelNode node = (ManagementModelNode)nodes.nextElement();
                if (!node.getUserObject().toString().contains(fieldText)) {
                    removeList.add(node);
                }
            }

            for (ManagementModelNode node : removeList) {
                root.remove(node);
            }

            model.nodeStructureChanged(root);
        }
    }

    /**
     * Listener that populates (or refreshes) the children when a node is expanded.
     */
    private class ManagementTreeExpansionListener implements TreeExpansionListener {
        private DefaultTreeModel treeModel;

        public ManagementTreeExpansionListener(DefaultTreeModel treeModel) {
            this.treeModel = treeModel;
        }

        public void treeCollapsed(TreeExpansionEvent tee) {
            // do nothing
        }

        public void treeExpanded(TreeExpansionEvent tee) {
            ManagementModelNode node = (ManagementModelNode) tee.getPath().getLastPathComponent();
            node.explore();
            treeModel.nodeStructureChanged(node);
        }
    }

    /**
     * Listener that populates the command line with the address of the selected node.
     */
    private class ManagementTreeSelectionListener implements TreeSelectionListener {

        public void valueChanged(TreeSelectionEvent tse) {
            ManagementModelNode selected = (ManagementModelNode) tse.getPath().getLastPathComponent();
            cliGuiCtx.getCommandLine().getCmdText().setText(selected.addressPath());
        }
    }

    /**
     * Listener that triggers the operationMenu menu containing operations.
     */
    private class ManagementTreeMouseListener extends MouseAdapter {

        private JTree tree;
        private OperationMenu operationMenu;
        private GraphingMenu graphingMenu;

        public ManagementTreeMouseListener(JTree tree) {
            this.tree = tree;
            this.operationMenu = new OperationMenu(cliGuiCtx, tree);
            this.graphingMenu = new GraphingMenu(cliGuiCtx, tree);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (!e.isPopupTrigger()) return;
            showPopup(e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (!e.isPopupTrigger()) return;
            showPopup(e);
        }

        private void showPopup(MouseEvent e) {
            int selRow = tree.getRowForLocation(e.getX(), e.getY());
            if (selRow == -1) return;

            TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
            tree.setSelectionPath(selPath);

            ManagementModelNode node = (ManagementModelNode)selPath.getLastPathComponent();

            UserObject usrObj = (UserObject)node.getUserObject();
            AttributeDescription attrDesc = usrObj.getAttributeDescription();
            if ((attrDesc != null) && attrDesc.isGraphable()) {
        //        graphingMenu.show(node, e.getX(), e.getY());
            } else {
                operationMenu.show(node, e.getX(), e.getY());
            }
        }
    }
}
