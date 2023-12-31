/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.controller.parsing;

import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration of all the recognized core configuration XML element local names, by name.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public enum Element {
    // must be first
// must be first
    UNKNOWN(null),

    // Domain elements in alpha order
    ACCESS_CONTROL("access-control"),
    ADVANCED_FILTER("advanced-filter"),
    AGENT_LIB("agent-lib"),
    AGENT_PATH("agent-path"),
    ANY("any"),
    ANY_ADDRESS("any-address"),
    ANY_IPV4_ADDRESS("any-ipv4-address"),
    ANY_IPV6_ADDRESS("any-ipv6-address"),
    APPLICATION_CLASSIFICATION("application-classification"),
    APPLICATION_CLASSIFICATIONS("application-classifications"),
    AUDIT_LOG("audit-log"),
    AUTHENTICATION("authentication"),
    AUTHORIZATION("authorization"),

    CACHE("cache"),
    CLIENT_CERT_STORE("client-certificate-store"),
    CLIENT_MAPPING("client-mapping"),
    CONFIGURATION_CHANGES("configuration-changes"),
    CONSTANT_HEADERS("constant-headers"),
    CONSTRAINTS("constraints"),
    CONTENT("content"),
    CREDENTIAL_REFERENCE("credential-reference"),

    DISCOVERY_OPTION("discovery-option"),
    DISCOVERY_OPTIONS("discovery-options"),
    DOMAIN("domain"),
    DOMAIN_CONTROLLER("domain-controller"),
    DEPLOYMENT("deployment"),
    DEPLOYMENTS("deployments"),
    DEPLOYMENT_OVERLAY("deployment-overlay"),
    DEPLOYMENT_OVERLAYS("deployment-overlays"),

    ENGINE("engine"),
    ENVIRONMENT_VARIABLES("environment-variables"),
    EXCLUDE("exclude"),
    EXCLUDED_EXTENSIONS("excluded-extensions"),
    EXTENSION("extension"),
    EXTENSIONS("extensions"),

    FILE_HANDLER("file-handler"),
    FORMATTER("formatter"),
    FORMATTERS("formatters"),
    FS_ARCHIVE("fs-archive"),
    FS_EXPLODED("fs-exploded"),

    GROUP("group"),
    GROUP_SEARCH("group-search"),
    GROUP_TO_PRINCIPAL("group-to-principal"),
    GROUPS_FILTER("groups-filter"),


    HANDLER("handler"),
    HANDLERS("handlers"),
    HEADER("header"),
    HEADER_MAPPING("header-mapping"),
    HEAP("heap"),
    HOST("host"),
    HOSTS("hosts"),
    HOST_API_VERSION("host-api-version"),
    HOST_EXCLUDE("host-exclude"),
    HOST_EXCLUDES("host-excludes"),
    HOST_RELEASE("host-release"),
    HOST_SCOPED_ROLES("host-scoped-roles"),
    HTTP_INTERFACE("http-interface"),
    HTTP_UPGRADE("http-upgrade"),

    IDENTITY("identity"),
    IGNORED_RESOURCE("ignored-resources"),
    IN_MEMORY_HANDLER("in-memory-handler"),
    INCLUDE("include"),
    INSTANCE("instance"),
    INET_ADDRESS("inet-address"),
    INTERFACE("interface"),
    INTERFACE_SPECS("interface-specs"),
    INTERFACES("interfaces"),

    JAAS("jaas"),
    JAVA_AGENT("java-agent"),
    JSON_FORMATTER("json-formatter"),
    JVM("jvm"),
    JVMS("jvms"),
    JVM_OPTIONS("jvm-options"),

    KERBEROS("kerberos"),
    KEY_PASSWORD_CREDENTIAL_REFERENCE("key-password-credential-reference"),
    KEYSTORE("keystore"),
    KEYSTORE_PASSWORD_CREDENTIAL_REFERENCE("keystore-password-credential-reference"),
    KEYTAB("keytab"),

    LAUNCH_COMMAND("launch-command"),
    LDAP("ldap"),
    LINK_LOCAL_ADDRESS("link-local-address"),
    LOCAL("local"),
    LOCAL_DESTINATION("local-destination"),
    LOGGER ("logger"),
    LOOPBACK("loopback"),
    LOOPBACK_ADDRESS("loopback-address"),

    MANAGEMENT("management"),
    MANAGEMENT_CLIENT_CONTENT("management-client-content"),
    MANAGEMENT_INTERFACES("management-interfaces"),
    MEMBERSHIP_FILTER("membership-filter"),
    MODULE_OPTIONS("module-options"),
    MULTICAST("multicast"),

    NAME("name"),
    NATIVE_INTERFACE("native-interface"),
    NATIVE_REMOTING_INTERFACE("native-remoting-interface"),
    NIC("nic"),
    NIC_MATCH("nic-match"),
    NOT("not"),

    OPTION("option"),
    OUTBOUND_CONNECTIONS("outbound-connections"),
    OUTBOUND_SOCKET_BINDING("outbound-socket-binding"),

    PASSWORD("password"),
    PATH("path"),
    PATHS("paths"),
    PERIODIC_ROTATING_FILE_HANDLER("periodic-rotating-file-handler"),
    PERMGEN("permgen"),
    PLUG_IN("plug-in"),
    PLUG_INS("plug-ins"),
    POINT_TO_POINT("point-to-point"),
    PRINCIPAL_TO_GROUP("principal-to-group"),
    PROFILE("profile"),
    PROFILES("profiles"),
    PROPERTY("property"),
    PROPERTIES("properties"),
    PUBLIC_ADDRESS("public-address"),

    REMOTE("remote"),
    REMOTE_DESTINATION("remote-destination"),
    ROLE("role"),
    ROLE_MAPPING("role-mapping"),
    ROLLOUT_PLANS("rollout-plans"),

    SEARCH_CREDENTIAL_REFERENCE("search-credential-reference"),
    SECRET("secret"),
    SECURITY_REALM("security-realm"),
    SECURITY_REALMS("security-realms"),
    SENSITIVE_CLASSIFICATION("sensitive-classification"),
    SENSITIVE_CLASSIFICATIONS("sensitive-classifications"),
    SERVER("server"),
    SERVER_GROUP_SCOPED_ROLES("server-group-scoped-roles"),
    SERVER_LOGGER("server-logger"),
    SERVER_IDENTITIES("server-identities"),
    SERVERS("servers"),
    SERVER_GROUP("server-group"),
    SERVER_GROUPS("server-groups"),
    SITE_LOCAL_ADDRESS("site-local-address"),
    SIZE_ROTATING_FILE_HANDLER("size-rotating-file-handler"),
    SOCKET("socket"),
    SOCKET_BINDING("socket-binding"),
    SOCKET_BINDINGS("socket-bindings"),
    SOCKET_BINDING_GROUP("socket-binding-group"),
    SOCKET_BINDING_GROUPS("socket-binding-groups"),
    SSL("ssl"),
    STACK("stack"),
    STATIC_DISCOVERY("static-discovery"),
    SUBNET_MATCH("subnet-match"),
    SUBSYSTEM("subsystem"),
    SYSLOG_HANDLER("syslog-handler"),
    SYSTEM_PROPERTIES("system-properties"),
    TCP("tcp"),
    TLS("tls"),
    TRUSTSTORE("truststore"),
    TYPE("type"),

    UDP("udp"),
    UP("up"),
    USER("user"),
    USERNAME_FILTER("username-filter"),
    USERNAME_IS_DN("username-is-dn"),
    USERNAME_TO_DN("username-to-dn"),
    USERS("users"),

    VARIABLE("variable"),
    VIRTUAL("virtual"),
    VAULT("vault"),
    VAULT_EXPRESSION_SENSITIVITY("vault-expression-sensitivity"),
    VAULT_OPTION("vault-option");

    private final String name;

    Element(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    private static final Map<String, Element> MAP;

    static {
        final Map<String, Element> map = new HashMap<String, Element>();
        for (Element element : values()) {
            final String name = element.getLocalName();
            if (name != null) map.put(name, element);
        }
        MAP = map;
    }

    public static Element forName(String localName) {
        final Element element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }
}
