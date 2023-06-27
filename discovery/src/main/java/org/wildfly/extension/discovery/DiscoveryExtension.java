/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.discovery;

import java.util.EnumSet;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PersistentResourceXMLDescriptionWriter;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.discovery.spi.DiscoveryProvider;

/**
 * The extension class for the WildFly Discovery extension.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class DiscoveryExtension implements Extension {

    static final String SUBSYSTEM_NAME = "discovery";

    static final ParentResourceDescriptionResolver SUBSYSTEM_RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME, DiscoveryExtension.class);

    static final RuntimeCapability<?> DISCOVERY_PROVIDER_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.discovery.provider", true)
            .setServiceType(DiscoveryProvider.class)
            .setAllowMultipleRegistrations(true)
            .build();

    @Override
    public void initialize(final ExtensionContext context) {
        final SubsystemRegistration subsystemRegistration = context.registerSubsystem(SUBSYSTEM_NAME, DiscoverySubsystemModel.CURRENT.getVersion());
        subsystemRegistration.setHostCapable();
        subsystemRegistration.registerXMLElementWriter(new PersistentResourceXMLDescriptionWriter(DiscoverySubsystemSchema.CURRENT));

        final ManagementResourceRegistration resourceRegistration = subsystemRegistration.registerSubsystemModel(new DiscoverySubsystemDefinition());
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        for (DiscoverySubsystemSchema schema : EnumSet.allOf(DiscoverySubsystemSchema.class)) {
            context.setSubsystemXmlMapping(SUBSYSTEM_NAME, schema.getNamespace().getUri(), schema);
        }
    }
}
