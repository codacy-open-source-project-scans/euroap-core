/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.host.controller.mgmt;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.host.controller.logging.HostControllerLogger;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.protocol.mgmt.ManagementPingRequest;
import org.jboss.remoting3.Channel;
import org.jboss.threads.AsyncFuture;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Coordinates periodic pinging of a slave Host Controller to validate its connection
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SlaveHostPinger {

    public static final long STD_INTERVAL;
    public static final long STD_TIMEOUT;
    public static final long SHORT_TIMEOUT = 10000;

    static {
        long interval = -1;
        try {
            interval = Long.parseLong(WildFlySecurityManager.getPropertyPrivileged("jboss.as.domain.ping.interval", "15000"));
        } catch (Exception e) {
            // TODO log
        } finally {
            // add 500 ms to provided interval to help allow the slave pings to prevent the need for master pings
            STD_INTERVAL = interval > 0 ? interval + 500 : 15500;
        }
        long timeout = -1;
        try {
            timeout = Long.parseLong(WildFlySecurityManager.getPropertyPrivileged("jboss.as.domain.ping.timeout", "30000"));
        } catch (Exception e) {
            // TODO log
        } finally {
            STD_TIMEOUT = timeout > 0 ? timeout : 30000;
        }
    }

    private final String hostName;
    private final ManagementChannelHandler channelHandler;
    private final ScheduledExecutorService scheduler;

    private volatile Long remoteConnectionID;
    private volatile boolean cancelled;

    public SlaveHostPinger(String hostName, ManagementChannelHandler channelHandler, ScheduledExecutorService scheduler, long remoteConnectionID) {
        this.hostName = hostName;
        this.channelHandler = channelHandler;
        this.scheduler = scheduler;
        this.remoteConnectionID = remoteConnectionID;
    }

    public Long getRemoteConnectionID() {
        return remoteConnectionID;
    }

    public void schedulePing(long timeout, long delay) {
        PingTask task = new PingTask(timeout, delay);
        scheduler.schedule(task, delay, TimeUnit.MILLISECONDS);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        this.cancelled = true;
    }

    private class PingTask implements Runnable {

        private final long timeout;
        private final long interval;

        private PingTask(long timeout, long interval) {
            this.timeout = timeout;
            this.interval = interval;
        }

        @Override
        public void run() {
            if (!cancelled) {
                boolean fail = false;
                AsyncFuture<Long> future = null;
                try {
                    if (interval < 1 || System.currentTimeMillis() - channelHandler.getLastMessageReceivedTime() > interval) {
                        future = channelHandler.executeRequest(ManagementPingRequest.INSTANCE, null).getResult();
                        Long id = future.get(timeout, TimeUnit.MILLISECONDS);
                        if (!cancelled && remoteConnectionID != null && !remoteConnectionID.equals(id)) {
                            HostControllerLogger.DOMAIN_LOGGER.slaveHostControllerChanged(hostName);
                            fail = true;
                        } else {
                            remoteConnectionID = id;
                        }
                    }
                } catch (IOException e) {
                    HostControllerLogger.DOMAIN_LOGGER.debug("Caught exception sending ping request", e);
                } catch (InterruptedException e) {
                    safeCancel(future);
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    HostControllerLogger.DOMAIN_LOGGER.debug("Caught exception sending ping request", e);
                } catch (TimeoutException e) {
                    if (!cancelled) {
                        fail = true;
                        HostControllerLogger.DOMAIN_LOGGER.slaveHostControllerUnreachable(hostName, timeout);
                    }
                    safeCancel(future);
                } finally {
                    if (fail) {
                        Channel channel = null;
                        try {
                            channel = channelHandler.getChannel();
                        } catch (IOException e) {
                            // ignore; shouldn't happen as the channel is already established if this task is running
                        }
                        StreamUtils.safeClose(channel);
                    } else if (!cancelled && interval > 0) {
                        scheduler.schedule(this, interval, TimeUnit.MILLISECONDS);
                    }
                }
            }
        }

        void safeCancel(Future<?> future) {
            if (future != null) {
                future.cancel(true);
            }
        }
    }
}
