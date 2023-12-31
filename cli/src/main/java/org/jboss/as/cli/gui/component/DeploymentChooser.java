/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.cli.gui.component;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.jboss.as.cli.gui.CliGuiContext;

/**
 * This component produces a JPanel containing a sortable table that allows choosing
 * a deployment that exists on the server.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class DeploymentChooser extends JPanel {

    private StandaloneDeploymentTableModel model;

    public DeploymentChooser(CliGuiContext cliGuiCtx, boolean isStandalone) {
        if (isStandalone) {
            model = new StandaloneDeploymentTableModel(cliGuiCtx);
        } else {
            model = new DomainDeploymentTableModel(cliGuiCtx);
        }

        DeploymentTable table = new DeploymentTable(model, isStandalone);
        JScrollPane scroller = new JScrollPane(table);
        add(scroller);
    }

    /**
     * Get the name of the selected deployment.
     * @return The name or null if there are no deployments.
     */
    public String getSelectedDeployment() {
        return model.getSelectedDeployment();
    }

    public boolean hasDeployments() {
        return model.hasDeployments();
    }

}
