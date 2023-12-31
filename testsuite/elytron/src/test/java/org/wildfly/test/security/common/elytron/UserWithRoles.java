/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.security.common.elytron;

import static org.wildfly.common.Assert.checkNotNullParamWithNullPointerException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Object which holds user configuration (password, roles).
 *
 * @author Josef Cacek
 */
public class UserWithRoles {

    private final String name;
    private final String password;
    private final Set<String> roles;

    private UserWithRoles(Builder builder) {
        this.name = checkNotNullParamWithNullPointerException("builder.name", builder.name);
        this.password = builder.password != null ? builder.password : builder.name;
        this.roles = new HashSet<>(checkNotNullParamWithNullPointerException("builder.roles", builder.roles));
    }

    /**
     * Returns username.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns password as plain text.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set of roles to be assigned to the user.
     */
    public Set<String> getRoles() {
        return roles;
    }

    /**
     * Creates builder to build {@link UserWithRoles}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link UserWithRoles}.
     */
    public static final class Builder {
        private String name;
        private String password;
        private final Set<String> roles = new HashSet<>();

        private Builder() {
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        /**
         * Add given roles to the builder. It doesn't replace existing roles, but it adds given roles to them.
         */
        public Builder withRoles(Set<String> roles) {
            if (roles != null) {
                this.roles.addAll(roles);
            }
            return this;
        }

        /**
         * Add given roles to the builder. It doesn't replace existing roles, but it adds given roles to them.
         */
        public Builder withRoles(String... roles) {
            if (roles != null) {
                this.roles.addAll(Arrays.asList(roles));
            }
            return this;
        }

        /**
         * Clears set of already added roles.
         */
        public Builder clearRoles() {
            this.roles.clear();
            return this;
        }

        /**
         * Builds UserWithRoles instance.
         *
         * @return
         */
        public UserWithRoles build() {
            return new UserWithRoles(this);
        }
    }

}
