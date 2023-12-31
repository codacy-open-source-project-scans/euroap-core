/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.cli.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.function.Supplier;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.gui.component.CLIOutput;
import org.jboss.as.cli.gui.component.ScriptMenu;
import org.jboss.as.cli.gui.component.TabsMenu;
import org.jboss.as.cli.gui.metacommand.DeployAction;
import org.jboss.as.cli.gui.metacommand.OnlineHelpAction;
import org.jboss.as.cli.gui.metacommand.UndeployAction;
import org.jboss.as.controller.client.ModelControllerClient;

/**
 * Static main class for the GUI.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class GuiMain {
    private static Image jbossIcon;
    static {
        URL iconURL = GuiMain.class.getResource("/icon/wildfly.png");
        jbossIcon = Toolkit.getDefaultToolkit().getImage(iconURL);
        ToolTipManager.sharedInstance().setDismissDelay(15000);
    }

    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(GuiMain.class);
    private static String LOOK_AND_FEEL_KEY = "cli-gui-laf";

    private GuiMain() {} // don't allow an instance

    public static void start(CommandContext cmdCtx) {
        initJFrame(makeGuiContext(cmdCtx, null));
    }

    public static CliGuiContext startEmbedded(CommandContext cmdCtx, Supplier<ModelControllerClient> client) {
        return makeGuiContext(cmdCtx, client);
    }

    private static CliGuiContext makeGuiContext(CommandContext cmdCtx, Supplier<ModelControllerClient> client) {
        CliGuiContext cliGuiCtx = new CliGuiContext();

        cliGuiCtx.setCommandContext(cmdCtx);

        CommandExecutor executor = new CommandExecutor(cliGuiCtx, client);
        cliGuiCtx.setExecutor(executor);

        CLIOutput output = new CLIOutput();
        cliGuiCtx.setOutput(output);

        JPanel outputDisplay = makeOutputDisplay(output);
        JTabbedPane tabs = makeTabbedPane(cliGuiCtx, outputDisplay);
        cliGuiCtx.setTabs(tabs);

        DoOperationActionListener opListener = new DoOperationActionListener(cliGuiCtx);
        CommandLine cmdLine = new CommandLine(opListener);
        cliGuiCtx.setCommandLine(cmdLine);

        output.addMouseListener(new SelectPreviousOpMouseAdapter(cliGuiCtx, opListener));

        JPanel mainPanel = makeMainPanel(tabs, cmdLine);
        cliGuiCtx.setMainPanel(mainPanel);

        return cliGuiCtx;
    }

    private static JPanel makeMainPanel(JTabbedPane tabs, CommandLine cmdLine) {
        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEtchedBorder());
        mainPanel.setLayout(new BorderLayout(5,5));
        mainPanel.add(cmdLine, BorderLayout.NORTH);
        mainPanel.add(tabs, BorderLayout.CENTER);
        return mainPanel;
    }

    public static Image getJBossIcon() {
        return jbossIcon;
    }

    private static synchronized void initJFrame(CliGuiContext cliGuiCtx) {
        JFrame frame = new JFrame("CLI GUI");

        frame.setIconImage(getJBossIcon());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.setJMenuBar(makeMenuBar(cliGuiCtx));
        frame.setSize(800, 600);

        Container contentPane = frame.getContentPane();

        contentPane.add(cliGuiCtx.getMainPanel(), BorderLayout.CENTER);

        setUpLookAndFeel(cliGuiCtx.getMainWindow());
        frame.setVisible(true);
    }

    public static JMenuBar makeMenuBar(CliGuiContext cliGuiCtx) {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(makeDeploymentsMenu(cliGuiCtx));
        menuBar.add(new TabsMenu(cliGuiCtx));
        menuBar.add(new ScriptMenu(cliGuiCtx));
        JMenu lfMenu = makeLookAndFeelMenu(cliGuiCtx);
        if (lfMenu != null) menuBar.add(lfMenu);
        JMenu helpMenu = makeHelpMenu();
        if (helpMenu != null) menuBar.add(helpMenu);
        return menuBar;
    }

    private static JMenu makeLookAndFeelMenu(CliGuiContext cliGuiCtx) {
        final LookAndFeelInfo[] all = UIManager.getInstalledLookAndFeels();
        if (all == null) return null;

        final JMenu lfMenu = new JMenu("Look & Feel");
        lfMenu.setMnemonic(KeyEvent.VK_L);

        for (final LookAndFeelInfo lookAndFeelInfo : all) {
            JMenuItem item = new JMenuItem(new ChangeLookAndFeelAction(cliGuiCtx, lookAndFeelInfo));
            lfMenu.add(item);
        }

        return lfMenu;
    }

    private static class ChangeLookAndFeelAction extends AbstractAction {
        private static final String errorTitle = "Look & Feel Not Set";
        private CliGuiContext cliGuiCtx;
        private LookAndFeelInfo lookAndFeelInfo;

        ChangeLookAndFeelAction(CliGuiContext cliGuiCtx, LookAndFeelInfo lookAndFeelInfo) {
            super(lookAndFeelInfo.getName());
            this.cliGuiCtx = cliGuiCtx;
            this.lookAndFeelInfo = lookAndFeelInfo;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            Window mainWindow = cliGuiCtx.getMainWindow();
            try {
                UIManager.setLookAndFeel(lookAndFeelInfo.getClassName());
                SwingUtilities.updateComponentTreeUI(mainWindow);
                PREFERENCES.put(LOOK_AND_FEEL_KEY, lookAndFeelInfo.getClassName());
            } catch (Exception ex) {
                showErrorDialog(mainWindow, errorTitle, ex);
            }
        }
    }

    private static JMenu makeDeploymentsMenu(CliGuiContext cliGuiCtx) {
        JMenu metaCmdMenu = new JMenu("Deployments");
        metaCmdMenu.setMnemonic(KeyEvent.VK_D);

        JMenuItem deploy = new JMenuItem(new DeployAction(cliGuiCtx));
        deploy.setMnemonic(KeyEvent.VK_D);
        metaCmdMenu.add(deploy);

        JMenuItem unDeploy = new JMenuItem(new UndeployAction(cliGuiCtx));
        unDeploy.setMnemonic(KeyEvent.VK_U);
        metaCmdMenu.add(unDeploy);

        return metaCmdMenu;
    }

    private static JMenu makeHelpMenu() {
        if (!Desktop.isDesktopSupported()) return null;
        final Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) return null;

        JMenu help = new JMenu("Help");
        help.setMnemonic(KeyEvent.VK_H);
        JMenuItem onlineHelp = new JMenuItem(new OnlineHelpAction());
        help.add(onlineHelp);

        return help;
    }

    private static JTabbedPane makeTabbedPane(CliGuiContext cliGuiCtx, JPanel output) {
        JTabbedPane tabs = new JTabbedPane();
        ManagementModel mgtModel = new ManagementModel(cliGuiCtx);
        tabs.addTab("Command Builder", mgtModel);

        ManagementModelNode loggingSubsys = mgtModel.findNode("/subsystem=logging/");
        if (loggingSubsys != null && cliGuiCtx.isStandalone() && ServerLogsPanel.isLogDownloadAvailable(cliGuiCtx)) {
            tabs.addTab("Server Logs", new ServerLogsPanel(cliGuiCtx));
        }
        tabs.addTab("Output", output);
        return tabs;
    }

    private static JPanel makeOutputDisplay(CLIOutput output) {
        JPanel outputDisplay = new JPanel();
        outputDisplay.setSize(400, 5000);
        outputDisplay.setLayout(new BorderLayout(5,5));
        outputDisplay.add(new JScrollPane(output), BorderLayout.CENTER);
        return outputDisplay;
    }

    public static void setUpLookAndFeel(Window mainWindow) {
        try {
            final String laf = PREFERENCES.get(LOOK_AND_FEEL_KEY, UIManager.getSystemLookAndFeelClassName());
            UIManager.setLookAndFeel(laf);
            SwingUtilities.updateComponentTreeUI(mainWindow);
        } catch (Throwable e) {
            // Just ignore if the L&F has any errors
        }
    }

    private static void showErrorDialog(Window window, final String title, final Throwable t) {
        JOptionPane.showMessageDialog(window, t.getLocalizedMessage(), title, JOptionPane.ERROR_MESSAGE);
    }
}
