/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.security.common.other;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.dmr.ModelNode;
import org.wildfly.test.security.common.elytron.AbstractConfigurableElement;

/**
 * Elytron configurator for trusted-domains attribute in a security-domain.
 *
 * @author Josef Cacek
 */
public class TrustedDomainsConfigurator extends AbstractConfigurableElement {

    private final String[] trustedSecurityDomains;

    private ModelNode originalDomains;

    private TrustedDomainsConfigurator(Builder builder) {
        super(builder);
        this.trustedSecurityDomains = builder.trustedSecurityDomains;
    }

    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        final PathAddress domainAddress = PathAddress.pathAddress().append("subsystem", "elytron").append("security-domain",
                name);
        ModelNode op = Util.createEmptyOperation("read-attribute", domainAddress);
        op.get("name").set("trusted-security-domains");
        ModelNode result = client.execute(op);
        if (Operations.isSuccessfulOutcome(result)) {
            result = Operations.readResult(result);
            originalDomains = result.isDefined() ? result : null;
        } else {
            throw new RuntimeException("Reading existing value of trusted-security-domains attribute failed: "
                    + Operations.getFailureDescription(result));
        }

        op = Util.createEmptyOperation("write-attribute", domainAddress);
        op.get("name").set("trusted-security-domains");
        for (String domain : trustedSecurityDomains) {
            op.get("value").add(domain);
        }
        CoreUtils.applyUpdate(op, client);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        final PathAddress domainAddress = PathAddress.pathAddress().append("subsystem", "elytron").append("security-domain",
                name);
        ModelNode op = Util.createEmptyOperation("write-attribute", domainAddress);
        op.get("name").set("trusted-security-domains");
        if (originalDomains != null) {
            op.get("value").set(originalDomains);
        }
        CoreUtils.applyUpdate(op, client);
        originalDomains = null;
    }

    /**
     * Creates builder to build {@link TrustedDomainsConfigurator}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link TrustedDomainsConfigurator}.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private String[] trustedSecurityDomains;

        private Builder() {
        }

        public Builder withTrustedSecurityDomains(String... trustedSecurityDomains) {
            this.trustedSecurityDomains = trustedSecurityDomains;
            return this;
        }

        public TrustedDomainsConfigurator build() {
            return new TrustedDomainsConfigurator(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}