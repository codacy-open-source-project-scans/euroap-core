/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.security.common.elytron;

import static org.wildfly.test.security.common.ModelNodeUtil.setIfNotNull;

import java.util.Map;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.dmr.ModelNode;

/**
 * Elytron 'kerberos-security-factory' configuration helper.
 *
 * @author Josef Cacek
 */
public class KerberosSecurityFactory extends AbstractConfigurableElement {

    private final Boolean debug;
    private final String[] mechanismNames;
    private final String[] mechanismOids;
    private final Integer minimumRemainingLifetime;
    private final Boolean obtainKerberosTicket;
    private final Map<String, String> options;
    private final String principal;
    private final Integer requestLifetime;
    private final Boolean server;
    private final Boolean wrapGssCredential;
    private final CliPath cliPath;

    private KerberosSecurityFactory(Builder builder) {
        super(builder);
        this.debug = builder.debug;
        this.mechanismNames = builder.mechanismNames;
        this.mechanismOids = builder.mechanismOids;
        this.minimumRemainingLifetime = builder.minimumRemainingLifetime;
        this.obtainKerberosTicket = builder.obtainKerberosTicket;
        this.options = builder.options;
        this.principal = builder.principal;
        this.requestLifetime = builder.requestLifetime;
        this.server = builder.server;
        this.wrapGssCredential = builder.wrapGssCredential;
        this.cliPath = builder.cliPath;
    }

    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        ModelNode op = Util.createAddOperation(
                PathAddress.pathAddress().append("subsystem", "elytron").append("kerberos-security-factory", name));
        setIfNotNull(op, "debug", debug);
        setIfNotNull(op, "mechanism-names", mechanismNames);
        setIfNotNull(op, "mechanism-oids", mechanismOids);
        setIfNotNull(op, "minimum-remaining-lifetime", minimumRemainingLifetime);
        setIfNotNull(op, "obtain-kerberos-ticket", obtainKerberosTicket);
        setIfNotNull(op, "obtain-kerberos-ticket", obtainKerberosTicket);
        setIfNotNull(op, "options", options);
        setIfNotNull(op, "principal", principal);
        setIfNotNull(op, "request-lifetime", requestLifetime);
        setIfNotNull(op, "server", server);
        setIfNotNull(op, "wrap-gss-credential", wrapGssCredential);
        setIfNotNull(op, "principal", principal);
        setIfNotNull(op, "path", cliPath.getPath());
        setIfNotNull(op, "relative-to", cliPath.getRelativeTo());

        CoreUtils.applyUpdate(op, client);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        CoreUtils.applyUpdate(
                Util.createRemoveOperation(
                        PathAddress.pathAddress().append("subsystem", "elytron").append("kerberos-security-factory", name)),
                client);
    }

    /**
     * Creates builder to build {@link KerberosSecurityFactory}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link KerberosSecurityFactory}.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private Boolean debug;
        private String[] mechanismNames;
        private String[] mechanismOids;
        private Integer minimumRemainingLifetime;
        private Boolean obtainKerberosTicket;
        private Map<String, String> options;
        private String principal;
        private Integer requestLifetime;
        private Boolean server;
        private Boolean wrapGssCredential;
        private CliPath cliPath;

        private Builder() {
        }

        public Builder withDebug(Boolean debug) {
            this.debug = debug;
            return this;
        }

        public Builder withMechanismNames(String[] mechanismNames) {
            this.mechanismNames = mechanismNames;
            return this;
        }

        public Builder withMechanismOids(String[] mechanismOids) {
            this.mechanismOids = mechanismOids;
            return this;
        }

        public Builder withMinimumRemainingLifetime(Integer minimumRemainingLifetime) {
            this.minimumRemainingLifetime = minimumRemainingLifetime;
            return this;
        }

        public Builder withObtainKerberosTicket(Boolean obtainKerberosTicket) {
            this.obtainKerberosTicket = obtainKerberosTicket;
            return this;
        }

        public Builder withOptions(Map<String, String> options) {
            this.options = options;
            return this;
        }

        public Builder withPrincipal(String principal) {
            this.principal = principal;
            return this;
        }

        public Builder withRequestLifetime(Integer requestLifetime) {
            this.requestLifetime = requestLifetime;
            return this;
        }

        public Builder withServer(Boolean server) {
            this.server = server;
            return this;
        }

        public Builder withWrapGssCredential(Boolean wrapGssCredential) {
            this.wrapGssCredential = wrapGssCredential;
            return this;
        }

        public Builder withCliPath(CliPath cliPath) {
            this.cliPath = cliPath;
            return this;
        }

        public KerberosSecurityFactory build() {
            return new KerberosSecurityFactory(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

}
