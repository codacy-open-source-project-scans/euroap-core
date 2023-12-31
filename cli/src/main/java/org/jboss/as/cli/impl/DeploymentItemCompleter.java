/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.cli.impl;


import java.io.IOException;
import java.util.List;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logging.Logger;

/**
 * A completer for exploded deployment.
 *
 * @author jfdenise
 */
public class DeploymentItemCompleter implements CommandLineCompleter {

    private final OperationRequestAddress address;
    private final Logger log = Logger.getLogger(DeploymentItemCompleter.class);

    public DeploymentItemCompleter(OperationRequestAddress address) {
        this.address = address;
    }

    @Override
    public int complete(CommandContext ctx, String buffer, int cursor,
            List<String> candidates) {
        getCandidates(ctx, buffer, candidates);
        return buffer.lastIndexOf("/") + 1;
    }

    private static String[] parsePath(String path) {
        // Normalise "./" + content
        if (path.isEmpty()) {
            path = "./";
        } else if (path.charAt(0) == '/') {
            path = "." + path;
        } else if (path.charAt(0) != '.') {
            path = "./" + path;
        }
        String directory = path;
        String subpath = "";
        int directoryIndex = path.lastIndexOf("/");
        if (directoryIndex >= 0) {
            directory = path.substring(0, directoryIndex + 1);
            subpath = path.substring(directoryIndex + 1);
        }

        String[] ret = {directory, subpath};
        return ret;
    }

    private void getCandidates(CommandContext ctx, String buffer,
            List<String> candidates) {
        try {
            // Corner case "."
            if (buffer.equals(".")) {
                candidates.add("./");
                return;
            }

            // Root dir of file system, meaningless.
            if (buffer.equals("/")) {
                return;
            }

            String[] parsed = parsePath(buffer);
            String directory = parsed[0];
            String subpath = parsed[1];

            DefaultOperationRequestBuilder builder
                    = new DefaultOperationRequestBuilder(address);
            builder.setOperationName(Util.BROWSE_CONTENT);
            builder.addProperty(Util.PATH, directory);
            builder.addProperty(Util.DEPTH, "1");
            ModelNode mn = builder.buildRequest();
            ModelNode response = ctx.getModelControllerClient().execute(mn);
            if (response.hasDefined(Util.OUTCOME) && response.get(Util.OUTCOME).
                    asString().equals(Util.SUCCESS)) {
                ModelNode result = response.get(Util.RESULT);
                if (result.getType() == ModelType.LIST) {
                    for (int i = 0; i < result.asInt(); i++) {
                        String path = result.get(i).get(Util.PATH).asString();
                        if (path.startsWith(subpath) && !path.equals(subpath)) {
                            candidates.add(path);
                        }
                    }
                }
            } else {
                log.debug("Invalid response getting candidates");
            }
        } catch (OperationFormatException | IOException ex) {
            log.debug("Exception getting candidates", ex);
        }
    }

}
