/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.controller.operations.global;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESOURCE_ADDED_NOTIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESOURCE_REMOVED_NOTIFICATION;

import java.util.ResourceBundle;

import org.jboss.as.controller.NotificationDefinition;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Global notifications emitted by all resources:
 *  * resource-added
 *  * resource-removed
 *  * attribute-value-written that contains in its data the old-value and new-value of the attribute that has been written
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class GlobalNotifications {

    private static final NotificationDefinition RESOURCE_ADDED = NotificationDefinition.Builder.create(RESOURCE_ADDED_NOTIFICATION, ControllerResolver.getResolver("global"))
            .build();
    private static final NotificationDefinition RESOURCE_REMOVED = NotificationDefinition.Builder.create(RESOURCE_REMOVED_NOTIFICATION, ControllerResolver.getResolver("global"))
            .build();

    public static final String OLD_VALUE = "old-value";
    public static final String NEW_VALUE = "new-value";

    private static final NotificationDefinition ATTRIBUTE_VALUE_WRITTEN = NotificationDefinition.Builder.create(ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION, ControllerResolver.getResolver("global"))
            .setDataValueDescriptor(new NotificationDefinition.DataValueDescriptor() {
                @Override
                public ModelNode describe(ResourceBundle bundle) {
                    String prefix = "global." + ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION + ".";
                    final ModelNode desc = new ModelNode();
                    desc.get(NAME, DESCRIPTION).set(bundle.getString(prefix + NAME));
                    desc.get(OLD_VALUE, DESCRIPTION).set(bundle.getString(prefix + OLD_VALUE));
                    desc.get(NEW_VALUE, DESCRIPTION).set(bundle.getString(prefix + NEW_VALUE));
                    return desc;
                }
            })
            .build();

    public static void registerGlobalNotifications(ManagementResourceRegistration root, ProcessType processType) {
        root.registerNotification(RESOURCE_ADDED, true);
        root.registerNotification(RESOURCE_REMOVED, true);

        if (processType != ProcessType.DOMAIN_SERVER) {
            root.registerNotification(ATTRIBUTE_VALUE_WRITTEN, true);
        }
    }
}