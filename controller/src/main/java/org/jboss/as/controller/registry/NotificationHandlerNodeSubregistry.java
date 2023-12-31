/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.controller.registry;

import static org.jboss.as.controller.PathElement.WILDCARD_VALUE;

import java.util.Collection;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.notification.Notification;
import org.jboss.as.controller.notification.NotificationHandler;

/**
 * A subregistry of {@code NotificationHandlerNodeRegistry} corresponding to a {@link org.jboss.as.controller.PathElement#getKey()} node and its children.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class NotificationHandlerNodeSubregistry {

    /**
     * The key of the node's PathElement (can not be {@code null})
     */
    private final String keyName;
    /**
     * the subregistry's parent node (can not be {@code null})
     */
    private final NotificationHandlerNodeRegistry parent;

    /**
     * The children of this subregistry.
     *
     * One of the keys can be {@link org.jboss.as.controller.PathElement#WILDCARD_VALUE} if an address is an address pattern.
     */
    @SuppressWarnings( { "unused" })
    private volatile Map<String, NotificationHandlerNodeRegistry> childRegistries;

    private static final AtomicMapFieldUpdater<NotificationHandlerNodeSubregistry, String, NotificationHandlerNodeRegistry> childRegistriesUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(NotificationHandlerNodeSubregistry.class, Map.class, "childRegistries"));

    NotificationHandlerNodeSubregistry(String keyName, NotificationHandlerNodeRegistry parent) {
        this.keyName = keyName;
        this.parent = parent;
        childRegistriesUpdater.clear(this);
    }

    /**
     * Get or create a new registry child for the given {@code elementValue} and traverse it to register the entry.
     */
    void registerEntry(ListIterator<PathElement> iterator, String elementValue, ConcreteNotificationHandlerRegistration.NotificationHandlerEntry entry) {
        final NotificationHandlerNodeRegistry newRegistry = new NotificationHandlerNodeRegistry(elementValue, this);
        final NotificationHandlerNodeRegistry existingRegistry = childRegistriesUpdater.putIfAbsent(this, elementValue, newRegistry);

        final NotificationHandlerNodeRegistry registry = existingRegistry != null ? existingRegistry : newRegistry;
        registry.registerEntry(iterator, entry);
    }

    /**
     * Get the registry child for the given {@code elementValue} and traverse it to unregister the entry.
     */
    void unregisterEntry(ListIterator<PathElement> iterator, String value, ConcreteNotificationHandlerRegistration.NotificationHandlerEntry entry) {
        NotificationHandlerNodeRegistry registry = childRegistries.get(value);
        if (registry == null) {
            return;
        }
        registry.unregisterEntry(iterator, entry);
    }

    /**
     * Get the registry child for the given {@code elementValue} and traverse it to collect the handlers that match the notifications.
     * If the subregistry has a children for the {@link org.jboss.as.controller.PathElement#WILDCARD_VALUE}, it is also traversed.
     */
    void findHandlers(ListIterator<PathElement> iterator, String value, Notification notification, Collection<NotificationHandler> handlers) {
        NotificationHandlerNodeRegistry registry = childRegistries.get(value);
        if (registry != null) {
            registry.findEntries(iterator, handlers, notification);
        }
        // if a child registry exists for the wildcard, we traverse it too
        NotificationHandlerNodeRegistry wildCardRegistry = childRegistries.get(WILDCARD_VALUE);
        if (wildCardRegistry != null) {
            wildCardRegistry.findEntries(iterator, handlers, notification);
        }
    }

    String getLocationString() {
        return parent.getLocationString() + "(" + keyName + " => ";
    }
}
