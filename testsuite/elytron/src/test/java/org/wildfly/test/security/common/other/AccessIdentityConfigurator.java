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
import org.wildfly.test.security.common.elytron.ConfigurableElement;

/**
 * Configuration helper for '/core-service=management/access=identity'. It can set or remove the security-domain name.
 *
 * @author Josef Cacek
 */
public class AccessIdentityConfigurator implements ConfigurableElement {

    private static final PathAddress IDENTITY_ADDR = PathAddress.pathAddress().append("core-service", "management")
            .append("access", "identity");
    private final String securityDomain;
    private String originalDomain;

    private AccessIdentityConfigurator(Builder builder) {
        this.securityDomain = builder.securityDomain;
    }

    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        originalDomain = setAccessIdentity(client, securityDomain);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        setAccessIdentity(client, originalDomain);
        originalDomain = null;
    }

    @Override
    public String getName() {
        return "/core-service=management/access=identity";
    }

    private String setAccessIdentity(ModelControllerClient client, String domainToSet) throws Exception {
        String origDomainValue = null;
        ModelNode op = Util.createEmptyOperation("read-attribute", IDENTITY_ADDR);
        op.get("name").set("security-domain");
        ModelNode result = client.execute(op);
        boolean identityExists = Operations.isSuccessfulOutcome(result);
        op = null;
        if (identityExists) {
            result = Operations.readResult(result);
            origDomainValue = result.isDefined() ? result.asString() : null;

            if (domainToSet == null) {
                op = Util.createRemoveOperation(IDENTITY_ADDR);
            } else if (!domainToSet.equals(origDomainValue)) {
                op = Util.createEmptyOperation("write-attribute", IDENTITY_ADDR);
                op.get("name").set("security-domain");
                op.get("value").set(domainToSet);
            }
        } else if (domainToSet != null) {
            op = Util.createAddOperation(IDENTITY_ADDR);
            op.get("security-domain").set(domainToSet);
        }

        if (op!=null) {
            CoreUtils.applyUpdate(op, client);
        }
        return origDomainValue;
    }
    /**
     * Creates builder to build {@link AccessIdentityConfigurator}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link AccessIdentityConfigurator}.
     */
    public static final class Builder {
        private String securityDomain;

        private Builder() {
        }

        public Builder withSecurityDomain(String securityDomain) {
            this.securityDomain = securityDomain;
            return this;
        }

        public AccessIdentityConfigurator build() {
            return new AccessIdentityConfigurator(this);
        }
    }
}
