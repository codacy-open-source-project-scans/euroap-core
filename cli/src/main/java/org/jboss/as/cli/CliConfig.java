/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.cli;


/**
 * This interface represents the JBoss CLI configuration.
 *
 * @author Alexey Loubyansky
 */
public interface CliConfig {

    /**
     * The default server controller host to connect to.
     *
     * @deprecated Use {@link CliConfig#getDefaultControllerAddress()} instead.
     *
     * @return default server controller host to connect to
     */
    @Deprecated
    String getDefaultControllerHost();

    /**
     * The default server controller port to connect to.
     *
     * @deprecated Use {@link CliConfig#getDefaultControllerAddress()} instead.
     *
     * @return  default server controller port to connect to
     */
    @Deprecated
    int getDefaultControllerPort();

    /**
     * The default server controller protocol for addresses where no protocol is specified.
     *
     * @return default server controller protocol
     */
    String getDefaultControllerProtocol();

    /**
     * If {@code true} then for addresses specified without a protocol but with a port number of 9999 the
     * protocol should be assumed to be 'remoting://'
     *
     * @return use legacy override option.
     */
    boolean isUseLegacyOverride();

    /**
     * The default address of the controller from the configuration.
     *
     * @return The default address.
     */
    ControllerAddress getDefaultControllerAddress();

    /**
     * Obtain the {@link ControllerAddress} for a given alias.
     *
     * @param alias - The alias if the address mapping.
     * @return The {@link ControllerAddress} if defined, otherwise {@code null}
     */
    ControllerAddress getAliasedControllerAddress(String alias);

    /**
     * Whether the record the history of executed commands and operations.
     *
     * @return  true if the history is enabled, false - otherwise.
     */
    boolean isHistoryEnabled();

    /**
     * The name of the command and operation history file.
     *
     * @return  name of the command and operation history file
     */
    String getHistoryFileName();

    /**
     * The directory which contains the command and operation history file.
     *
     * @return  directory which contains the command and operation history file.
     */
    String getHistoryFileDir();

    /**
     * Maximum size of the history log.
     *
     * @return maximum size of the history log
     */
    int getHistoryMaxSize();

    /**
     * Connection timeout period in milliseconds.
     *
     * @return connection timeout in milliseconds
     */
    int getConnectionTimeout();

    /**
     * The global SSL configuration if it has been defined.
     *
     * @return The SSLConfig
     */
    SSLConfig getSslConfig();

    /**
     * Whether the operation requests should be validated in terms of
     * addresses, operation names and parameters before they are
     * sent to the controller for execution.
     *
     * @return  true is the operation requests should be validated, false - otherwise.
     */
    boolean isValidateOperationRequests();

    /**
     * Whether to resolve system properties specified as command argument
     * (or operation parameter) values before sending the operation request
     * to the controller or let the resolution happen on the server side.
     * If the method returns true, the resolution should be performed by the CLI,
     * otherwise - on the server side.
     *
     * @return  true if the system properties specified as operation parameter
     * values should be resolved by the CLI, false if the resolution
     * of the parameter values should happen on the server side.
     */
    boolean isResolveParameterValues();

    /**
     * Whether the info or error messages should be written to the terminal output.
     *
     * The output of the info and error messages is done in the following way:
     * 1) the message is always logged using a logger
     *    (which is disabled in the config by default);
     * 2) if the output target was specified on the command line using '>'
     *    it would be used;
     * 3) if the output target was not specified, whether the message is
     *    written or not to the terminal output will depend on
     *    whether it's a silent mode or not.
     *
     * @return
     */
    boolean isSilent();

    /**
     * Non-Interactive only, using --file or --commands
     *
     * If true, disables all user interaction in non-interactive mode.
     * CLI will error if any user interaction is expected from the commands
     *
     * @return
     */
    boolean isErrorOnInteract();

    /**
     * Whether the role based access control should be used
     * to check the availability of the commands (for tab-completion).
     *
     * @return  true if command availability checks should include RBAC,
     *          otherwise - false
     */
    boolean isAccessControl();

    /**
     * When enabled, in non interactive mode, the command name and arguments are displayed prior to any
     * command output.
     *
     * @return true, the commands are echoed, false, commands are not echoed.
     */
    boolean isEchoCommand();

    /**
     * The command timeout.
     *
     * @return The command timeout;
     */
    Integer getCommandTimeout();

    /**
     * Output JSON for DMR content.
     * @return true if JSON output is expected, false if DMR string output is expected.
     */
    boolean isOutputJSON();

    /**
     * Format DMR content in color.
     * @return true if the output should be in color, false if color shouldn't be used.
     */
    boolean isColorOutput();

    /**
     * Color configurations to be used when enabled
     * @return The color configurations
     */
    ColorConfig getColorConfig();

    /**
     * Output paging is enabled by default and it enables users to browse and search long output of commands.
     *
     * @return true, the output of commands is paged, false, whole output of commands is written at once.
     */
    boolean isOutputPaging();
}
