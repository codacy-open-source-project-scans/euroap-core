/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.security.common.config;

/**
 * Simple property holder for datasource configuration.
 *
 * @see org.jboss.as.test.integration.security.common.AbstractDataSourceServerSetupTask
 * @author Josef Cacek
 */
public class DataSource {

    private final String name;
    private final String connectionUrl;
    private final String driver;
    private final String username;
    private final String password;

    // Constructors ----------------------------------------------------------

    /**
     * Create a new DataSource.
     *
     * @param builder
     */
    private DataSource(Builder builder) {
        this.name = builder.name;
        this.connectionUrl = builder.connectionUrl;
        this.driver = builder.driver;
        this.username = builder.username;
        this.password = builder.password;
    }

    // Public methods --------------------------------------------------------

    /**
     * Get the name.
     *
     * @return the name.
     */
    public String getName() {
        return name;
    }

    public String getJndiName() {
        return "java:jboss/datasources/" + name;
    }

    /**
     * Get the connectionUrl.
     *
     * @return the connectionUrl.
     */
    public String getConnectionUrl() {
        return connectionUrl;
    }

    /**
     * Get the driver.
     *
     * @return the driver.
     */
    public String getDriver() {
        return driver;
    }

    /**
     * Get the username.
     *
     * @return the username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get the password.
     *
     * @return the password.
     */
    public String getPassword() {
        return password;
    }

    // Embedded classes ------------------------------------------------------

    public static class Builder {
        private String name;
        private String connectionUrl;
        private String driver;
        private String username;
        private String password;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder connectionUrl(String connectionUrl) {
            this.connectionUrl = connectionUrl;
            return this;
        }

        public Builder driver(String driver) {
            this.driver = driver;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public DataSource build() {
            return new DataSource(this);
        }
    }

}
