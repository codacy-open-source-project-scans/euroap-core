/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.controller.audit;


import java.util.Collections;
import java.util.List;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.audit.SyslogAuditLogHandler.Facility;
import org.jboss.dmr.ModelNode;

/**
 * Abstract base class for {@link AuditLogger} implementations.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public interface ManagedAuditLogger extends AuditLogger {

    /**
     * Get whether this audit logger logs read-only operations
     *
     * @return whether this audit logger logs read-only operations
     */
    boolean isLogReadOnly();

    /**
     * Set whether to log read-only operations
     *
     * @param logReadOnly wheter to log read-only operations
     */
    void setLogReadOnly(boolean logReadOnly);

    /**
     * Get whether this audit logger logs operations on boot
     *
     * @return whether this audit logger logs operations on boot
     */
    boolean isLogBoot();

    /**
     * Set whether to log operations on boot
     *
     * @param logBoot whether to log operations on boot
     */
    void setLogBoot(boolean logBoot);

    /**
     * Gets the status of the audit logger
     *
     * @return the status
     */
    Status getLoggerStatus();

    /**
     * Sets the status of the audit logger
     *
     * @param  newStatus the status
     */
    void setLoggerStatus(Status newStatus);

    /**
     * Gets the handler updater used to schedule updates to the handlers
     *
     * @return the handler updater
     */
    AuditLogHandlerUpdater getUpdater();

    /**
     * Recycles a handler. This stops it, and resets the failure count so that the next time it is used
     * it will reinitialize.
     *
     * @param name the name of the handler
     */
    void recycleHandler(String name);

    /**
     * Create another audit logger configuration, e.g. for JMX which has its own global config of the audit logger
     *
     * @param manualCommit if {@code true} the caller is responsible for applying the changes themselves, if {@code false} the changes will be committed after the next log records has been written
     * @return the new configuration
     */
    ManagedAuditLogger createNewConfiguration(boolean manualCommit);

    /**
     * Add a formatter
     *
     * @param formatter the formatter
     */
    void addFormatter(AuditLogItemFormatter formatter);

    // Immediate updates - TODO find some better way to do these if we end up adding more handler types!
    /**
     * Update the handler formatter. This will take effect immediately.
     *
     * @param name the name of the handler
     * @param formatterName the name of the formatter
     */
    void updateHandlerFormatter(String name, String formatterName);

    /**
     * Update the handler max failure count. This will take effect immediately
     *
     * @param name the name of the handler
     * @param count the max failure count
     */
    void updateHandlerMaxFailureCount(String name, int count);

    /**
     * Update the syslog handler facility. This will take effect immediately.
     *
     * @param name the name of the syslog handler
     * @param facility the facility
     */
    void updateSyslogHandlerFacility(String name, Facility facility);

    /**
     * Update the handler app-name. This will take effect immediately
     *
     * @param name the name of the handler
     * @param appName the app name
     */
    void updateSyslogHandlerAppName(String name, String appName);

    /**
     * Update the handler reconnect timeout. This will take effect immediately
     *
     * @param name the name of the handler
     * @param reconnectTimeout the app name
     */
    void updateSyslogHandlerReconnectTimeout(String name, int reconnectTimeout);
    //ImmediateUpdates - end

    /**
     * Update the handler history size.
     *
     * @param name the name of the handler
     * @param maxHistory the history size of the handler
     */
    void updateInMemoryHandlerMaxHistory(String name, int maxHistory);
    /**
     * Remove a formatter
     *
     * @param name the formatter name
     */
    void removeFormatter(String name);

    /**
     * Get the current failure count of a handler
     *
     * @param name the name of the handler
     * @return the failure count
     */
    int getHandlerFailureCount(String name);

    /**
     * Get whether a handler was disabled due to failures
     *
     * @param name the name of the handler
     * @return whether it is disabled
     */
    boolean getHandlerDisabledDueToFailure(String name);

    /**
     * Gets a formatter by its name
     *
     * @param name the name of the formatter
     * @return the formatter
     */
    JsonAuditLogItemFormatter getJsonFormatter(String name);

    /**
     * Gets the last log entries
     *
     * @param name the name of the handler
     * @return the last log entries of the handler
     */
    default List<ModelNode> listLastEntries(String name){
        return Collections.emptyList();
    }

    /**
     * Callback for the controller to call before the controller is booted
     */
    void startBoot();

    /**
     * Callback for the controller to call when the controller has been booted
     */
    void bootDone();

    /**
     * <p>The audit log handler updater. Additive changes will be used for the audit log record as a result of
     * management operations causing updates here. Removals and updates will not take effect until the current audit log
     * record has been written.
     * </p>
     * <p>
     * This means that if a new handler is added and a reference is added, the new handler will be used to log the
     * operations causing that to happen.
     * </p>
     * <p>
     * If a handler is removed, the operations causing the removal will be logged to the handler, before removing it.
     * </p>
     * <p>
     * If a handler is changed, for example to change the location of a file handler or the protocol of a syslog
     * handler, the operations causing the change will be logged to the current handler location. The next incoming
     * log message will be written to the new handler location.
     * </p>
     */
    interface AuditLogHandlerUpdater {
        /**
         * Adds a new handler, this handler will be used when logging the current operation
         *
         * @param handler the handler
         */
        void addHandler(AuditLogHandler handler);

        /**
         * Update a handler. The update will only take place if the handler has actually been changed. The
         * changes to the handler will only take effect after the current operation has been logged.
         *
         * @param handler the updated handler
         */
        void updateHandler(AuditLogHandler handler);

        /**
         * Remove a handler. The removal will only take effect after the current operation has been logged.
         *
         * @param name the name of the handler to be removed
         * @throws IllegalStateException if the handler still has references to it
         */
        void removeHandler(String name);

        /**
         * Add a handler reference. This reference will take effect when logging the current operation
         *
         * @param referenceAddress the address of the handler reference (the value of the last element is the name of
         *                         the referenced handler)
         */
        void addHandlerReference(PathAddress referenceAddress);

        /**
         * Add a handler reference. This reference removal will only take effect after the current operation has been
         * logged.
         *
         * @param referenceAddress the address of the handler reference (the value of the last element is the name of
         *                         the referenced handler)
         */
        void removeHandlerReference(PathAddress referenceAddress);

        /**
         * Roll back changes made as part of the current operation.
         */
        void rollbackChanges();

        /**
         * Apply the changes. This is only allowed for update tasks for new audit log configurations which specify that manual commit
         * should be used.
         *
         * @throws IllegalStateException if manual commit should not be used
         */
        void applyChanges();

    }
}
