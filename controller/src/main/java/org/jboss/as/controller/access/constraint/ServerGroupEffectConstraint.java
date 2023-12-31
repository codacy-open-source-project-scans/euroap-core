/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.controller.access.constraint;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.access.HostEffect;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.JmxAction;
import org.jboss.as.controller.access.JmxTarget;
import org.jboss.as.controller.access.ServerGroupEffect;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.TargetResource;
import org.jboss.as.controller.access.rbac.StandardRole;

/**
 * Constraint related to whether the target resource is associated with one or more managed domain server groups.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class ServerGroupEffectConstraint extends AbstractConstraint implements Constraint, ScopingConstraint {


    public static final ScopingConstraintFactory FACTORY = new Factory();

    private static final ServerGroupEffectConstraint GLOBAL_USER = new ServerGroupEffectConstraint(true, false);
    private static final ServerGroupEffectConstraint DOMAIN_GLOBAL_REQUIRED = new ServerGroupEffectConstraint(false, false);
    private static final ServerGroupEffectConstraint HOST_GLOBAL_REQUIRED = new ServerGroupEffectConstraint(false, true);
    private static final ServerGroupEffectConstraint UNASSIGNED = new ServerGroupEffectConstraint();

    private final boolean user;
    private final boolean global;
    // nonServerHost means it applies to a host resource
    // but not a server or server-config child
    private final boolean nonServerHost;
    private final boolean unassigned;
    private final GroupsHolder groupsHolder;
    private final boolean readOnly;
    private final boolean groupAdd;
    private final boolean groupRemove;
    private final ServerGroupEffectConstraint readOnlyConstraint;

    // For domain unassigned resources
    private ServerGroupEffectConstraint() {
        this.user = false;
        this.global = false;
        this.nonServerHost = false;
        this.unassigned = true;
        this.readOnly = false;
        this.readOnlyConstraint = null;
        this.groupAdd = false;
        this.groupRemove = false;
        this.groupsHolder = new GroupsHolder();
    }

    // For GLOBAL cases
    private ServerGroupEffectConstraint(final boolean user, boolean nonServerHost) {
        this.user = user;
        this.global = true;
        this.nonServerHost = nonServerHost;
        this.unassigned = false;
        this.readOnly = false;
        this.readOnlyConstraint = null;
        this.groupAdd = false;
        this.groupRemove = false;
        this.groupsHolder = new GroupsHolder();
    }

    private ServerGroupEffectConstraint(Set<String> allowed, boolean nonServerHost, boolean groupAdd, boolean groupRemove) {
        this.user = false;
        this.global = false;
        this.nonServerHost = nonServerHost;
        this.unassigned = false;
        this.groupsHolder = new GroupsHolder(allowed);
        this.readOnly = false;
        this.groupAdd = groupAdd;
        this.groupRemove = groupRemove;
        this.readOnlyConstraint = null;
    }

    // For server group scoped role creation
    public ServerGroupEffectConstraint(List<String> allowed) {
        this.user = true;
        this.global = false;
        this.nonServerHost = false;
        this.unassigned = false;
        this.groupsHolder = new GroupsHolder(allowed);
        this.readOnly = false;
        this.groupAdd = false;
        this.groupRemove = false;
        this.readOnlyConstraint = new ServerGroupEffectConstraint(groupsHolder);
    }

    /**
     * Creates the constraint the standard constraint will return from {@link #getOutofScopeReadConstraint()}
     * Only call from {@link ServerGroupEffectConstraint#ServerGroupEffectConstraint(java.util.List)}
     */
    private ServerGroupEffectConstraint(GroupsHolder groupsHolder) {
        this.user = true;
        this.global = false;
        this.nonServerHost = false;
        this.unassigned = false;
        this.groupsHolder = groupsHolder;
        this.readOnly = true;
        this.groupAdd = false;
        this.groupRemove = false;
        this.readOnlyConstraint = null;
    }

    public void setAllowedGroups(List<String> allowed) {
        assert !global : "constraint is global";
        assert readOnlyConstraint != null : "invalid cast";
        this.groupsHolder.specific = new LinkedHashSet<String>(allowed);
    }

    @Override
    public boolean violates(Constraint other, Action.ActionEffect actionEffect) {
        if (other instanceof ServerGroupEffectConstraint) {
            ServerGroupEffectConstraint sgec = (ServerGroupEffectConstraint) other;
            Set<String> ourSpecific = groupsHolder.specific;
            Set<String> sgecSpecific = sgec.groupsHolder.specific;
            if (user) {
                assert !sgec.user : "illegal comparison";
                if (readOnly) {
                    // Allow global or any matching server group
                    // Also, since this is readOnly, allow nonServerHost
                    if (!sgec.global && !sgec.nonServerHost) {
                        boolean anyMatch = anyMatch(ourSpecific, sgecSpecific);
                        if (!anyMatch) ControllerLogger.ACCESS_LOGGER.tracef("read-only server-group constraint violated " +
                                "for action %s due to no match between groups %s and allowed groups %s",
                                actionEffect, sgecSpecific, ourSpecific);
                        return !anyMatch;
                    }
                } else if (!global) {
                    if (sgec.global) {
                        // Only the readOnlyConstraint gets global
                        ControllerLogger.ACCESS_LOGGER.tracef("server-group constraint violated for action %s due to " +
                            "requirement for access to global resources", actionEffect);
                        return true;
                    } else if (!sgec.unassigned) {
                        if (actionEffect == Action.ActionEffect.WRITE_RUNTIME || actionEffect == Action.ActionEffect.WRITE_CONFIG) {
                            //  Writes must not effect other groups
                            boolean containsAll = ourSpecific.containsAll(sgecSpecific);
                            if (!containsAll) {
                                ControllerLogger.ACCESS_LOGGER.tracef("server-group constraint violated for action %s due to " +
                                        "mismatch of groups %s vs allowed %s", actionEffect, sgecSpecific, ourSpecific);
                            } else if (sgec.groupAdd) {
                                ControllerLogger.ACCESS_LOGGER.tracef("server-group constraint violated for action %s due to " +
                                        "attempt to add the server group", actionEffect);
                            } else if (sgec.groupRemove) {
                                ControllerLogger.ACCESS_LOGGER.tracef("server-group constraint violated for action %s due to " +
                                        "attempt to remove the server group", actionEffect);
                            }
                            return !containsAll || sgec.groupAdd || sgec.groupRemove;
                        } else {
                            // Reads ok as long as one of our groups match
                            boolean anyMatch = anyMatch(ourSpecific, sgecSpecific);
                            if (!anyMatch)  {
                                // Allow access for server-group add so there's no bizarre "no such resource"
                                if (sgec.groupAdd && actionEffect == Action.ActionEffect.ADDRESS) {
                                    return false;
                                }
                                ControllerLogger.ACCESS_LOGGER.tracef("server-group constraint violated " +
                                    "for action %s due to no match between groups %s and allowed groups %s",
                                    actionEffect, sgecSpecific, ourSpecific);
                            }
                            return !anyMatch;
                        }
                    } // else fall through
                }
            } else {
                assert sgec.user : "illegal comparison";
                return other.violates(this, actionEffect);
            }
        }
        return false;
    }

    private boolean anyMatch(Set<String> ourSpecific, Set<String> sgecSpecific) {

        boolean matched = false;
        for (String ourGroup : ourSpecific) {
            if (sgecSpecific.contains(ourGroup)) {
                matched = true;
                break;
            }
        }
        if (!matched) {
            // WFLY-2089
            matched = sgecSpecific.size() == 1 && sgecSpecific.contains("*");
        }

        return matched;
    }

    @Override
    public boolean replaces(Constraint other) {
        return other instanceof ServerGroupEffectConstraint && (readOnly || readOnlyConstraint != null);
    }

    // Scoping Constraint

    @Override
    public ScopingConstraintFactory getFactory() {
        assert readOnlyConstraint != null : "invalid cast";
        return FACTORY;
    }

    @Override
    public Constraint getStandardConstraint() {
        assert readOnlyConstraint != null : "invalid cast";
        return this;
    }

    @Override
    public Constraint getOutofScopeReadConstraint() {
        assert readOnlyConstraint != null : "invalid cast";
        return readOnlyConstraint;
    }

    private static class GroupsHolder {
        private volatile Set<String> specific = new LinkedHashSet<String>();
        private GroupsHolder() {
          // no-op
        }
        private GroupsHolder(Collection<String> groups) {
            this.specific.addAll(groups);
        }
    }

    private static class Factory extends AbstractConstraintFactory implements ScopingConstraintFactory {

        @Override
        public Constraint getStandardUserConstraint(StandardRole role, Action.ActionEffect actionEffect) {
            return GLOBAL_USER;
        }

        @Override
        public Constraint getRequiredConstraint(Action.ActionEffect actionEffect, Action action, TargetAttribute target) {
            return getRequiredConstraint(target.getServerGroupEffect(), target.getHostEffect());
        }

        @Override
        public Constraint getRequiredConstraint(Action.ActionEffect actionEffect, Action action, TargetResource target) {
            return getRequiredConstraint(target.getServerGroupEffect(), target.getHostEffect());
        }

        @Override
        public Constraint getRequiredConstraint(Action.ActionEffect actionEffect, JmxAction action, JmxTarget target) {
            return getRequiredConstraint(target.getServerGroupEffect(), target.getHostEffect());
        }

        private Constraint getRequiredConstraint(ServerGroupEffect serverGroupEffect, HostEffect hostEffect) {
            boolean nonServerHost = hostEffect != null && !hostEffect.isHostEffectGlobal() && !hostEffect.isServerEffect();
            if (serverGroupEffect == null || serverGroupEffect.isServerGroupEffectGlobal()) {
                if (nonServerHost) {
                    return HOST_GLOBAL_REQUIRED;
                }
                return DOMAIN_GLOBAL_REQUIRED;
            } else if (serverGroupEffect.isServerGroupEffectUnassigned()) {
                return UNASSIGNED;
            }
            return new ServerGroupEffectConstraint(serverGroupEffect.getAffectedServerGroups(),
                    nonServerHost, serverGroupEffect.isServerGroupAdd(), serverGroupEffect.isServerGroupRemove());
        }

        @Override
        protected int internalCompare(AbstractConstraintFactory other) {
            // We prefer going first
            return this.equals(other) ? 0 : -1;
        }
    }
}
