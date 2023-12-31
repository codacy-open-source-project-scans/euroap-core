/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.cli.gui;

import java.awt.Window;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.gui.component.CLIOutput;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class CliGuiContext {

    private CommandContext cmdCtx;
    private CommandExecutor executor;
    private JPanel mainPanel;
    private CommandLine cmdLine;
    private boolean isStandalone;
    private CLIOutput output;
    private JTabbedPane tabs;

    CliGuiContext() {
    }

    void setOutput(CLIOutput output) {
        this.output = output;
    }

    void setCommandContext(CommandContext cmdCtx) {
        this.cmdCtx = cmdCtx;
    }

    void setTabs(JTabbedPane tabs) {
        this.tabs = tabs;
    }

    void setExecutor(CommandExecutor executor) {
        this.executor = executor;
        try {
            ModelNode result = executor.doCommand("/:read-attribute(name=process-type)");
            this.isStandalone = result.get("result").asString().equals("Server");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void setMainPanel(JPanel mainPanel) {
        this.mainPanel = mainPanel;
    }

    void setCommandLine(CommandLine cmdLine) {
        this.cmdLine = cmdLine;
    }

    /**
     * Find if we are connected to a standalone AS instances
     * or a domain controller.
     * @return true if standalone, false otherwise.
     */
    public boolean isStandalone() {
        return this.isStandalone;
    }

    /**
     * Get the output component.
     *
     * @return The Output component.
     */
    public CLIOutput getOutput() {
        return this.output;
    }

    /**
     * Get the command context.
     * @return The command context.
     */
    public CommandContext getCommmandContext() {
        return this.cmdCtx;
    }

    /**
     * Get the tabbed pane containing the Command Builder and Output tabs
     * @return
     */
    public JTabbedPane getTabs() {
        return this.tabs;
    }

    /**
     * Get the main panel for CLI GUI.
     * @return The main panel
     */
    public JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * Get the singleton JFrame instance for the GUI
     * @return The JFrame
     */
    public Window getMainWindow() {
        return SwingUtilities.getWindowAncestor(mainPanel);
    }

    /**
     * Get the main command line.
     * @return The main command text field.
     */
    public CommandLine getCommandLine() {
        return cmdLine;
    }

    /**
     * Get the command executor.
     * @return The command executor.
     */
    public CommandExecutor getExecutor() {
        return executor;
    }
}
