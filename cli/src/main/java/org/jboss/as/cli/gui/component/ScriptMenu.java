/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.cli.gui.component;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.prefs.Preferences;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.jboss.as.cli.gui.CliGuiContext;
import org.jboss.as.cli.gui.metacommand.ChooseScriptAction;
import org.jboss.as.cli.gui.metacommand.OpenScriptAction;

/**
 * Extension of JMenu that dynamically manages the list of previously-run scripts.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class ScriptMenu extends JMenu {
    // Store the previously-run scripts using the preferences API
    private static final Preferences prefs = Preferences.userNodeForPackage(ScriptMenu.class);
    private static final int SCRIPT_LIST_SIZE = 15;

    private CliGuiContext cliGuiCtx;

    private LinkedList<File> previouslyRun = new LinkedList<File>();

    public ScriptMenu(CliGuiContext cliGuiCtx) {
        super("Scripts");
        this.cliGuiCtx = cliGuiCtx;
        setMnemonic(KeyEvent.VK_S);

        JMenuItem chooseScript = new JMenuItem(new ChooseScriptAction(this, cliGuiCtx));
        chooseScript.setMnemonic(KeyEvent.VK_C);
        add(chooseScript);

        addSeparator();
        readPreviouslyRun();
    }

    private void readPreviouslyRun() {
        for (int i = 0; i < SCRIPT_LIST_SIZE; i++) {
            String filePath = prefs.get("Script" + i, null);
            if (filePath != null) {
                File file = new File(filePath);
                if (file.exists()) {
                    previouslyRun.add(file);
                    add(new OpenScriptAction(this, cliGuiCtx, file));
                }
            }
        }
    }

    private void writePreviouslyRun() {
        int i = 0;
        for (File file : previouslyRun) {
            if (!file.exists()) continue;

            try {
                prefs.put("Script" + i, file.getCanonicalPath());
            } catch (IOException e) {
                e.printStackTrace();
            }

            i++;
        }

        // fill rest of list with empty Strings
        for (int j=i; j < SCRIPT_LIST_SIZE; j++) {
            prefs.put("Script" + j, "");
        }
    }

    public void addScript(File scriptFile) {
        previouslyRun.remove(scriptFile);
        previouslyRun.addFirst(scriptFile);

        // remove all priviously-run scripts from the menu
        for (int i=this.getItemCount() - 1; i > 1; i--) {
            this.remove(i);
        }

        // prune list if it is too long
        if (previouslyRun.size() > SCRIPT_LIST_SIZE) {
            previouslyRun.removeLast();
        }

        // refresh menu items using changed list
        for (File file : previouslyRun) {
            if (file.exists()) {
                add(new OpenScriptAction(this, cliGuiCtx, file));
            }
        }

        writePreviouslyRun();
    }
}
