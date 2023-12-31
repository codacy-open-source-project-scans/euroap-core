/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jmx.model;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESOURCE_ADDED_NOTIFICATION;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.notification.Notification;
import org.jboss.as.controller.notification.NotificationFilter;
import org.jboss.as.controller.notification.NotificationHandler;
import org.jboss.as.controller.notification.NotificationHandlerRegistry;
import org.jboss.as.controller.operations.global.GlobalNotifications;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.NotificationHandlerRegistration;
import org.jboss.as.jmx.BaseMBeanServerPlugin;
import org.jboss.as.jmx.logging.JmxLogger;
import org.jboss.dmr.ModelNode;

/**
 * An MBeanServer wrapper that exposes the ModelController via JMX.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ModelControllerMBeanServerPlugin extends BaseMBeanServerPlugin {

    private final MBeanServer mbeanServer;
    private final ConfiguredDomains configuredDomains;
    private final ModelControllerMBeanHelper legacyHelper;
    private final ModelControllerMBeanHelper exprHelper;
    private final NotificationHandlerRegistry notificationRegistry;
    private final AtomicLong notificationSequenceNumber = new AtomicLong(0);

    public ModelControllerMBeanServerPlugin(final MBeanServer mbeanServer,
                                            final ConfiguredDomains configuredDomains, ModelController controller, NotificationHandlerRegistry notificationHandlerRegistry, final MBeanServerDelegate delegate,
                                            boolean legacyWithProperPropertyFormat, ProcessType processType,
                                            ManagementModelIntegration.ManagementModelProvider managementModelProvider, boolean isMasterHc) {
        assert configuredDomains != null;
        this.mbeanServer = mbeanServer;
        this.configuredDomains = configuredDomains;
        this.notificationRegistry = notificationHandlerRegistry;

        MutabilityChecker mutabilityChecker = MutabilityChecker.create(processType, isMasterHc);
        legacyHelper = configuredDomains.getLegacyDomain() != null ?
                new ModelControllerMBeanHelper(TypeConverters.createLegacyTypeConverters(legacyWithProperPropertyFormat),
                        configuredDomains, configuredDomains.getLegacyDomain(), controller, mutabilityChecker, managementModelProvider) : null;
        exprHelper = configuredDomains.getExprDomain() != null ?
                new ModelControllerMBeanHelper(TypeConverters.createExpressionTypeConverters(), configuredDomains,
                        configuredDomains.getExprDomain(), controller, mutabilityChecker, managementModelProvider) : null;

        // JMX notifications for MBean registration/unregistration are emitted by the MBeanServerDelegate and not by the
        // MBeans itself. If we have a reference on the delegate, we add a listener for any WildFly resource address
        // that converts the resource-added and resource-removed notifications to MBeanServerNotification and send them
        // through the delegate.
        if (delegate != null) {
            for (String domain : configuredDomains.getDomains()) {
                ResourceRegistrationNotificationHandler handler = new ResourceRegistrationNotificationHandler(delegate, domain);
                notificationRegistry.registerNotificationHandler(NotificationHandlerRegistration.ANY_ADDRESS, handler, handler);
            }
        }
    }

    @Override
    public boolean accepts(ObjectName objectName) {
        String domain = objectName.getDomain();
        if (!objectName.isDomainPattern()) {
            return domain.equals(configuredDomains.getLegacyDomain()) || domain.equals(configuredDomains.getExprDomain());
        }

        Pattern p = Pattern.compile(objectName.getDomain().replace("*", ".*"));
        String legacyDomain = configuredDomains.getLegacyDomain();
        if (legacyDomain != null && p.matcher(legacyDomain).matches()) {
            return true;
        }
        String exprDomain = configuredDomains.getExprDomain();
        if (exprDomain != null && p.matcher(exprDomain).matches()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldAuditLog() {
        return false;
    }

    @Override
    public boolean shouldAuthorize() {
        return false;
    }


    public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException,
            ReflectionException {
        return getHelper(name).getAttribute(name, attribute);
    }

    public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException {
        return getHelper(name).getAttributes(name, attributes);
    }

    public ClassLoader getClassLoader(ObjectName loaderName) throws InstanceNotFoundException {
        if (getHelper(loaderName).resolvePathAddress(loaderName) != null) {
            return SecurityActions.getClassLoader(this.getClass());
        }
        throw JmxLogger.ROOT_LOGGER.mbeanNotFound(loaderName);
    }

    public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
        if (getHelper(mbeanName).toPathAddress(mbeanName) != null) {
            return SecurityActions.getClassLoader(this.getClass());
        }
        throw JmxLogger.ROOT_LOGGER.mbeanNotFound(mbeanName);
    }

    public String[] getDomains() {
        return configuredDomains.getDomains();
    }

    public Integer getMBeanCount() {
        //long start = System.nanoTime();
        int count = 0;
        if (legacyHelper != null) {
            count += legacyHelper.getMBeanCount();
        }
        if (exprHelper != null) {
            // exprHelper has the same # of mbeans as legacyHelper, so only ask for a count if we didn't already
            count = count > 0 ? count * 2 : exprHelper.getMBeanCount();
        }
        //JmxLogger.ROOT_LOGGER.infof("Elapsed getMBeanCount time using the non-RBAC RootResourceIterator approach was %d, resulting in a count of %d", (System.nanoTime() - start), count);
        return count;
    }

    public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
        return getHelper(name).getMBeanInfo(name);
    }

    public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
        return getHelper(name).getObjectInstance(name);
    }

    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException,
            MBeanException, ReflectionException {
        return getHelper(name).invoke(name, operationName, params, signature);
    }

    public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException {
        // return true for NotificationBroadcaster so that client connected to the MBeanServer know
        // that it can broadcast notifications.
        if (NotificationBroadcaster.class.getName().equals(className)) {
            return true;
        }
        return false;
    }

    public boolean isRegistered(ObjectName name) {
        return getHelper(name).resolvePathAddress(name) != null;
    }

    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
        if (name != null && !name.isDomainPattern()) {
            return getHelper(name).queryMBeans(mbeanServer, name, query);
        } else {
            Set<ObjectInstance> instances = new HashSet<ObjectInstance>();
            if (legacyHelper != null) {
                instances.addAll(legacyHelper.queryMBeans(mbeanServer, name, query));
            }
            if (exprHelper != null) {
                instances.addAll(exprHelper.queryMBeans(mbeanServer, name, query));
            }
            return instances;
        }
    }

    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        if (name != null && !name.isDomainPattern()) {
            return getHelper(name).queryNames(mbeanServer, name, query);
        } else {
            Set<ObjectName> instances = new HashSet<ObjectName>();
            if (legacyHelper != null) {
                instances.addAll(legacyHelper.queryNames(mbeanServer, name, query));
            }
            if (exprHelper != null) {
                instances.addAll(exprHelper.queryNames(mbeanServer, name, query));
            }
            return instances;
        }
    }

    public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException,
            InvalidAttributeValueException, MBeanException, ReflectionException {
        getHelper(name).setAttribute(name, attribute);
    }

    public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
        return getHelper(name).setAttributes(name, attributes);
    }

    public void addNotificationListener(final ObjectName name, final NotificationListener listener, final javax.management.NotificationFilter filter, final Object handback)
            throws InstanceNotFoundException {
        // NOTE: We do not perform any check whether the ObjectName conforms to an existing resource,
        // throwing an InstanceNotFoundException if not. This is arguably a bug, as the javadoc for
        // MBeanServerConnection.addNotificationListener says it will do that. But, the kernel management
        // layer we are wrapping here does not require its notification handling registration be limited
        // to existing resources, so it doesn't need us to reject this kind of registration. And doing so may
        // break existing use cases, e.g. see the discussion in https://issues.jboss.org/browse/WFCORE-3387
        //
        // Perhaps we could check whether the address corresponds to a resource that *could* exist (i.e. there's
        // a registration for some resource type that matches) and throw INFE for those that do not. That
        // would help with catching things like (some) typos in the ObjectName.

        PathAddress pathAddress = getHelper(name).toPathAddress(name);
        JMXNotificationHandler handler = new JMXNotificationHandler(getHelper(name).getDomain(), name, listener, filter, handback);
        notificationRegistry.registerNotificationHandler(pathAddress, handler, handler);
    }

    public void addNotificationListener(ObjectName name, ObjectName listener, javax.management.NotificationFilter filter, Object handback)
            throws InstanceNotFoundException {
        throw new RuntimeOperationsException(JmxLogger.ROOT_LOGGER.addNotificationListerWithObjectNameNotSupported(listener));
    }

    public void removeNotificationListener(ObjectName name, NotificationListener listener, javax.management.NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException {
        PathAddress pathAddress = getHelper(name).toPathAddress(name);
        JMXNotificationHandler handler = new JMXNotificationHandler(getHelper(name).getDomain(), name, listener, filter, handback);
        notificationRegistry.unregisterNotificationHandler(pathAddress, handler, handler);
    }

    public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException,
            ListenerNotFoundException {
        removeNotificationListener(name, listener, null, null);
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener, javax.management.NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException {
        throw new RuntimeOperationsException(JmxLogger.ROOT_LOGGER.removeNotificationListerWithObjectNameNotSupported(listener));
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException {
        throw new RuntimeOperationsException(JmxLogger.ROOT_LOGGER.removeNotificationListerWithObjectNameNotSupported(listener));
    }

    private ModelControllerMBeanHelper getHelper(ObjectName name) {
        String domain = name.getDomain();
        if (domain.equals(configuredDomains.getLegacyDomain()) || name.isDomainPattern()) {
            return legacyHelper;
        }
        if (domain.equals(configuredDomains.getExprDomain())) {
            return exprHelper;
        }
        //This should not happen
        throw JmxLogger.ROOT_LOGGER.unknownDomain(domain);
    }

    /**
     * Handle WildFly notifications, convert them to JMX notifications and forward them to the JMX listener.
     */
    private class JMXNotificationHandler implements NotificationHandler, NotificationFilter {

        private final String domain;
        private final ObjectName name;
        private final NotificationListener listener;
        private final javax.management.NotificationFilter filter;
        private final Object handback;

        private JMXNotificationHandler(String domain, ObjectName name, NotificationListener listener, javax.management.NotificationFilter filter, Object handback) {
            this.domain = domain;
            this.name = name;
            this.listener = listener;
            this.filter = filter;
            this.handback = handback;
        }

        @Override
        public boolean isNotificationEnabled(Notification notification) {
            if (isResourceAddedOrRemovedNotification(notification)) {
                // filter outs resource-added and resource-removed notifications that are handled by the
                // MBeanServerDelegate
                return false;
            }
            ObjectName sourceName = ObjectNameAddressUtil.createObjectName(domain, notification.getSource());
            if (!name.equals(sourceName)) {
                return false;
            }
            if (filter == null) {
                return true;
            } else {
                javax.management.Notification jmxNotification = convert(notification);
                return filter.isNotificationEnabled(jmxNotification);
            }
        }

        @Override
        public void handleNotification(Notification notification) {
            javax.management.Notification jmxNotification = convert(notification);
            if (jmxNotification != null) {
            listener.handleNotification(jmxNotification, handback);
            }
        }

        private javax.management.Notification convert(Notification notification) {
            long sequenceNumber = notificationSequenceNumber.incrementAndGet();
            ObjectName source = ObjectNameAddressUtil.createObjectName(domain, notification.getSource());
            String message = notification.getMessage();
            long timestamp = notification.getTimestamp();
            String notificationType = notification.getType();
            javax.management.Notification jmxNotification;
            if (notificationType.equals(ModelDescriptionConstants.ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION)) {
                ModelNode data = notification.getData();
                String attributeName = data.get(ModelDescriptionConstants.NAME).asString();
                String jmxAttributeName = NameConverter.convertToCamelCase(attributeName);
                try {
                    ImmutableManagementResourceRegistration reg = getHelper(source).getMBeanRegistration(source);
                    ModelNode modelDescription = reg.getModelDescription(PathAddress.EMPTY_ADDRESS).getModelDescription(null);
                    ModelNode attributeDescription = modelDescription.get(ATTRIBUTES, attributeName);
                    AttributeDefinition attrDef = reg.getAttributeAccess(PathAddress.EMPTY_ADDRESS, attributeName).getAttributeDefinition();

                    TypeConverters converters = getHelper(source).getConverters();
                    Object oldValue = converters.fromModelNode(attrDef, attributeDescription, data.get(GlobalNotifications.OLD_VALUE));
                    Object newValue = converters.fromModelNode(attrDef, attributeDescription, data.get(GlobalNotifications.NEW_VALUE));
                    String attributeType = converters.convertToMBeanType(attrDef, attributeDescription).getTypeName();
                    jmxNotification = new AttributeChangeNotification(source, sequenceNumber, timestamp, message, jmxAttributeName, attributeType, oldValue, newValue);

                } catch (InstanceNotFoundException e) {
                    // fallback to a generic notification
                    jmxNotification = new javax.management.Notification(notification.getType(), source, sequenceNumber, timestamp, message);

                }
            } else if (notificationType.equals(ModelDescriptionConstants.RESOURCE_ADDED_NOTIFICATION) ||
                notificationType.equals(ModelDescriptionConstants.RESOURCE_REMOVED_NOTIFICATION)) {
                // do not convert resource-added and resource-removed notifications: for JMX, they are not emitted by the MBean itself
                // but by the MBeanServerDelegate (see ModelControllerMBeanServerPlugin constructor)
                jmxNotification = null;
            } else {
                jmxNotification = new javax.management.Notification(notificationType, source, sequenceNumber, timestamp, message);
                jmxNotification.setUserData(notification.getData());
            }
            return jmxNotification;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            JMXNotificationHandler that = (JMXNotificationHandler) o;

            if (filter != null ? !filter.equals(that.filter) : that.filter != null) return false;
            if (handback != null ? !handback.equals(that.handback) : that.handback != null) return false;
            if (!listener.equals(that.listener)) return false;
            if (!name.equals(that.name)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + listener.hashCode();
            result = 31 * result + (filter != null ? filter.hashCode() : 0);
            result = 31 * result + (handback != null ? handback.hashCode() : 0);
            return result;
        }
    }

    /**
     * Handle resource-added and resource-removed notifications that are converted to MBeanServerNotifcations
     * emitted by the MBeanServerDelegate
     */
    private static class ResourceRegistrationNotificationHandler implements NotificationHandler, NotificationFilter {

        private final MBeanServerDelegate delegate;
        private final String domain;
        private AtomicLong sequence = new AtomicLong(0);

        private ResourceRegistrationNotificationHandler(MBeanServerDelegate delegate, String domain) {
            this.delegate = delegate;
            this.domain = domain;
        }

        @Override
        public void handleNotification(Notification notification) {
            String jmxType = notification.getType().equals(RESOURCE_ADDED_NOTIFICATION) ? MBeanServerNotification.REGISTRATION_NOTIFICATION : MBeanServerNotification.UNREGISTRATION_NOTIFICATION;
            ObjectName mbeanName = ObjectNameAddressUtil.createObjectName(domain, notification.getSource());
            javax.management.Notification jmxNotification = new MBeanServerNotification(jmxType, MBeanServerDelegate.DELEGATE_NAME, sequence.incrementAndGet(), mbeanName);
            delegate.sendNotification(jmxNotification);
        }

        @Override
        public boolean isNotificationEnabled(Notification notification) {
            return isResourceAddedOrRemovedNotification(notification);
        }
    }

    private static boolean isResourceAddedOrRemovedNotification(Notification notification) {
        return notification.getType().equals(RESOURCE_ADDED_NOTIFICATION) ||
                notification.getType().equals(ModelDescriptionConstants.RESOURCE_REMOVED_NOTIFICATION);
    }
}
