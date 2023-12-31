/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.process.protocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

import javax.net.ServerSocketFactory;

import org.jboss.as.process.logging.ProcessLogger;
import org.jboss.as.process.protocol.Connection.ClosedCallback;
import org.wildfly.common.Assert;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ProtocolServer {

    private final ThreadFactory threadFactory;
    private final ServerSocketFactory socketFactory;
    private final ConnectionHandler connectionHandler;
    private final InetSocketAddress bindAddress;
    private final int backlog;
    private final int readTimeout;
    private final Executor readExecutor;
    private volatile boolean stop;
    private volatile Thread thread;
    private volatile ServerSocket serverSocket;
    private volatile InetSocketAddress boundAddress;
    private final ClosedCallback callback;

    public ProtocolServer(final Configuration configuration) throws IOException {
        threadFactory = configuration.getThreadFactory();
        socketFactory = configuration.getSocketFactory();
        connectionHandler = configuration.getConnectionHandler();
        bindAddress = configuration.getBindAddress();
        backlog = configuration.getBacklog();
        readTimeout = configuration.getReadTimeout();
        readExecutor = configuration.getReadExecutor();
        callback = configuration.getClosedCallback();
        Assert.checkNotNullParam("bindAddress", bindAddress);
        Assert.checkNotNullParam("connectionHandler", connectionHandler);
    }

    public void start() throws IOException {
        stop = false;

        final ServerSocket serverSocket = socketFactory.createServerSocket();
        this.serverSocket = serverSocket;
        thread = threadFactory.newThread(new Runnable() {
            public void run() {
                try {
                    while (! serverSocket.isClosed() && ! stop) {
                        try {
                            final Socket socket = serverSocket.accept();
                            boolean ok = false;
                            try {
                                socket.setSoTimeout(readTimeout);
                                ok = true;
                            } finally {
                                if (! ok) {
                                    try {
                                        socket.close();
                                    } catch (IOException e) {
                                        ProcessLogger.PROTOCOL_CLIENT_LOGGER.failedToCloseSocket(e);
                                    }
                                }
                            }
                            safeHandleConnection(socket);
                        } catch (SocketException e) {
                            if (!stop) {
                                // we do not log if service is stopped, we assume the exception was caused by closing the
                                // ServerSocket
                                ProcessLogger.PROTOCOL_CLIENT_LOGGER.failedToAcceptConnection(e);
                            }
                        } catch (IOException e) {
                            ProcessLogger.PROTOCOL_CLIENT_LOGGER.failedToAcceptConnection(e);
                        }

                    }
                } finally {
                    StreamUtils.safeClose(serverSocket);
                }
            }
        });
        if (thread == null) {
            throw ProcessLogger.ROOT_LOGGER.failedToCreateServerThread();
        }
        thread.setName("Accept thread");
        serverSocket.setReuseAddress(true);
        serverSocket.bind(bindAddress, backlog);
        boundAddress = (InetSocketAddress) serverSocket.getLocalSocketAddress();
        thread.start();
    }

    public void stop() {
        stop = true;
        final Thread thread = this.thread;
        boundAddress = null;
        if (thread != null) {
            // thread.interrupt may not actually interrupt socket.accept()
            thread.interrupt();
        }
        StreamUtils.safeClose(serverSocket);
    }

    private void safeHandleConnection(final Socket socket) {
        boolean ok = false;
        try {
            final ConnectionImpl connection = new ConnectionImpl(socket, MessageHandler.NULL, readExecutor, callback);
            connection.setMessageHandler(connectionHandler.handleConnected(connection));
            final Thread thread = threadFactory.newThread(connection.getReadTask());
            if (thread == null) {
                throw ProcessLogger.ROOT_LOGGER.threadCreationRefused();
            }
            thread.setName("Read thread for " + socket.getRemoteSocketAddress());
            thread.start();
            ok = true;
        } catch (IOException e) {
            ProcessLogger.PROTOCOL_CLIENT_LOGGER.failedToHandleIncomingConnection(e);
        } finally {
            if (! ok) {
                try {
                    socket.close();
                } catch (IOException e) {
                    ProcessLogger.PROTOCOL_CLIENT_LOGGER.failedToCloseSocket(e);
                }
            }
        }
    }

    public InetSocketAddress getBoundAddress() {
        return boundAddress;
    }

    public static final class Configuration {
        private ThreadFactory threadFactory;
        private ServerSocketFactory socketFactory;
        private ConnectionHandler connectionHandler;
        private MessageHandler messageHandler;
        private InetSocketAddress bindAddress;
        private int backlog;
        private int readTimeout;
        private Executor readExecutor;
        private ClosedCallback closedCallback;

        public ThreadFactory getThreadFactory() {
            return threadFactory;
        }

        public void setThreadFactory(final ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
        }

        public ServerSocketFactory getSocketFactory() {
            return socketFactory;
        }

        public void setSocketFactory(final ServerSocketFactory socketFactory) {
            this.socketFactory = socketFactory;
        }

        public ConnectionHandler getConnectionHandler() {
            return connectionHandler;
        }

        public void setConnectionHandler(final ConnectionHandler connectionHandler) {
            this.connectionHandler = connectionHandler;
        }

        public MessageHandler getMessageHandler() {
            return messageHandler;
        }

        public void setMessageHandler(final MessageHandler messageHandler) {
            this.messageHandler = messageHandler;
        }

        public InetSocketAddress getBindAddress() {
            return bindAddress;
        }

        public void setBindAddress(final InetSocketAddress bindAddress) {
            this.bindAddress = bindAddress;
        }

        public int getBacklog() {
            return backlog;
        }

        public void setBacklog(final int backlog) {
            this.backlog = backlog;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(final int readTimeout) {
            this.readTimeout = readTimeout;
        }

        public Executor getReadExecutor() {
            return readExecutor;
        }

        public void setReadExecutor(final Executor readExecutor) {
            this.readExecutor = readExecutor;
        }


        public ClosedCallback getClosedCallback() {
            return closedCallback;
        }

        public void setCallback(ClosedCallback closedCallback) {
            this.closedCallback = closedCallback;
        }
    }
}
