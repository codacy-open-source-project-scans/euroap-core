/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.cli.handlers.jca;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.accesscontrol.AccessRequirement;
import org.jboss.as.cli.accesscontrol.AccessRequirementBuilder;
import org.jboss.as.cli.accesscontrol.HostServerOperationAccess;
import org.jboss.as.cli.handlers.BaseOperationCommand;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.DefaultCompleter;
import org.jboss.as.cli.impl.DefaultCompleter.CandidatesProvider;
import org.jboss.as.cli.util.SimpleTable;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class JDBCDriverInfoHandler extends BaseOperationCommand {

    private final ArgumentWithValue host;
    private final ArgumentWithValue server;
    private final ArgumentWithValue name;

    private HostServerOperationAccess hostServerPermission;

    public JDBCDriverInfoHandler(CommandContext ctx) {
        super(ctx, "jdbc-driver-info", true);
        addRequiredPath("/subsystem=datasources");

        host = new ArgumentWithValue(this, new DefaultCompleter(new CandidatesProvider(){
            @Override
            public Collection<String> getAllCandidates(CommandContext ctx) {
                final ModelControllerClient client = ctx.getModelControllerClient();
                if(client == null) {
                    return Collections.emptyList();
                }
                return hostServerPermission.getAllowedHosts(ctx);
            }}), "--host") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                return ctx.isDomainMode() && super.canAppearNext(ctx);
            }
        };

        final HostServerOperationAccess hostServerPermission = this.hostServerPermission;
        server = new ArgumentWithValue(this, new DefaultCompleter(new CandidatesProvider(){
            @Override
            public Collection<String> getAllCandidates(CommandContext ctx) {
                final ModelControllerClient client = ctx.getModelControllerClient();
                if(client == null) {
                    return Collections.emptyList();
                }
                return hostServerPermission.getAllowedServers(ctx, host.getValue(ctx.getParsedCommandLine()));
            }}), "--server") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                return host.isPresent(ctx.getParsedCommandLine()) && super.canAppearNext(ctx);
            }
        };

        name = new ArgumentWithValue(this, new DefaultCompleter(new CandidatesProvider(){
            @Override
            public Collection<String> getAllCandidates(CommandContext ctx) {
                try {
                    final ModelNode req = buildRequestWithoutHeaders(ctx);
                    final ModelNode response = ctx.getModelControllerClient().execute(req);
                    if(response.hasDefined(Util.RESULT)) {
                        final List<ModelNode> list = response.get(Util.RESULT).asList();
                        final List<String> names = new ArrayList<String>(list.size());
                        for(ModelNode node : list) {
                            if(node.hasDefined(Util.DRIVER_NAME)) {
                                names.add(node.get(Util.DRIVER_NAME).asString());
                            }
                        }
                        return names;
                    } else {
                        return Collections.emptyList();
                    }
                } catch (Exception e) {
                    return Collections.emptyList();
                }
            }}), 0, "--name") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(ctx.isDomainMode()) {
                    return host.isPresent(ctx.getParsedCommandLine()) &&
                            server.isPresent(ctx.getParsedCommandLine()) &&
                            super.canAppearNext(ctx);
                }
                return super.canAppearNext(ctx);
            }
        };
    }

    protected AccessRequirement setupAccessRequirement(CommandContext ctx) {
        hostServerPermission = new HostServerOperationAccess(ctx, Util.SUBSYSTEM + '=' + Util.DATASOURCES, Util.INSTALLED_DRIVERS_LIST);
        return AccessRequirementBuilder.Factory.create(ctx)
                .any()
                .operation(Util.SUBSYSTEM + '=' + Util.DATASOURCES, Util.INSTALLED_DRIVERS_LIST)
                .requirement(hostServerPermission)
                .build();
    }

    @Override
    protected ModelNode buildRequestWithoutHeaders(CommandContext ctx) throws CommandFormatException {
        final ModelNode req = new ModelNode();
        final ModelNode address = req.get(Util.ADDRESS);
        if(ctx.isDomainMode()) {
            address.add(Util.HOST, host.getValue(ctx.getParsedCommandLine(), true));
            address.add(Util.SERVER, server.getValue(ctx.getParsedCommandLine(), true));
        }
        address.add(Util.SUBSYSTEM, Util.DATASOURCES);
        req.get(Util.OPERATION).set(Util.INSTALLED_DRIVERS_LIST);
        return req;
    }

    @Override
    protected void handleResponse(CommandContext ctx, ModelNode response, boolean composite) throws CommandLineException {
        final ModelNode result = response.get(Util.RESULT);
        if(!result.isDefined()) {
            throw new CommandLineException("The operation result is not defined: " + result);
        }
        final List<ModelNode> list = result.asList();
        if (!name.isPresent(ctx.getParsedCommandLine())) {
            final SimpleTable table = new SimpleTable(new String[] { "NAME", "SOURCE" }, ctx.getTerminalWidth());
            for (ModelNode node : list) {
                final ModelNode driverName = node.get(Util.DRIVER_NAME);
                if (!driverName.isDefined()) {
                    throw new CommandLineException(Util.DRIVER_NAME + " is not available: " + node);
                }
                final String source;
                if (node.hasDefined(Util.DEPLOYMENT_NAME)) {
                    source = node.get(Util.DEPLOYMENT_NAME).asString();
                } else if (node.hasDefined(Util.DRIVER_MODULE_NAME)) {
                    final StringBuilder buf = new StringBuilder();
                    buf.append(node.get(Util.DRIVER_MODULE_NAME).asString());
                    if(node.hasDefined(Util.MODULE_SLOT)) {
                        buf.append('/').append(node.get(Util.MODULE_SLOT).asString());
                    }
                    source = buf.toString();
                } else {
                    source = "n/a";
                }
                table.addLine(new String[] { driverName.asString(), source });
            }
            ctx.printLine(table.toString(true));
        } else {
            final String name = this.name.getValue(ctx.getParsedCommandLine());
            final SimpleTable table = new SimpleTable(2, ctx.getTerminalWidth());
            for (ModelNode node : list) {
                final ModelNode driverName = node.get(Util.DRIVER_NAME);
                if (!driverName.isDefined()) {
                    throw new CommandLineException(Util.DRIVER_NAME + " is not available: " + node);
                }
                if(name.equals(driverName.asString())) {
                    for(String propName : node.keys()) {
                        final ModelNode value = node.get(propName);
                        table.addLine(new String[] { propName, value.isDefined() ? value.asString() : "n/a" });
                    }
                }
            }
            ctx.printLine(table.toString(false));
        }
    }
}
