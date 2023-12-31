/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.controller.interfaces;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for AS7-3041.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class IPv6ScopeIdMatchUnitTestCase {

    private static NetworkInterface loopbackInterface;
    private static Inet6Address loopbackAddress;
    private static Map<NetworkInterface, Set<Inet6Address>> addresses = new HashMap<NetworkInterface, Set<Inet6Address>>();

    @BeforeClass
    public static void setup() throws Exception {
        Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
        while (nifs.hasMoreElements()) {
            NetworkInterface nif = nifs.nextElement();
            processNetworkInterface(nif);
            Enumeration<NetworkInterface> subs = nif.getSubInterfaces();
            while (subs.hasMoreElements()) {
                NetworkInterface sub = subs.nextElement();
                processNetworkInterface(sub);
            }
        }
        System.out.println("loopback: " + loopbackInterface + " " + loopbackAddress);
        for (Map.Entry<NetworkInterface, Set<Inet6Address>> entry : addresses.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue());
        }

    }

    private static void processNetworkInterface(NetworkInterface nif) {
        for (InterfaceAddress ifaddr : nif.getInterfaceAddresses()) {
            InetAddress inetAddress = ifaddr.getAddress();
            if (inetAddress instanceof Inet6Address) {
                Inet6Address inet6 = (Inet6Address) inetAddress;
                if (inet6.isLoopbackAddress()) {
                    loopbackInterface = nif;
                    loopbackAddress = inet6;
                } else if (addresses.containsKey(nif)) {
                    addresses.get(nif).add(inet6);
                } else {
                    Set<Inet6Address> set = new HashSet<Inet6Address>();
                    set.add(inet6);
                    addresses.put(nif, set);
                }
            }
        }
    }

    @Test
    public void testLoopback() throws Exception {
        if (loopbackAddress == null) {
            return;
        }

        InetAddressMatchInterfaceCriteria criteria = new InetAddressMatchInterfaceCriteria("::1");
        assertEquals(loopbackAddress, criteria.isAcceptable(loopbackInterface, loopbackAddress));
        criteria = new InetAddressMatchInterfaceCriteria("::1%" + loopbackInterface.getName());
        if (loopbackAddress.getScopeId() > 0) {
            assertEquals(loopbackAddress, criteria.isAcceptable(loopbackInterface, loopbackAddress));
        } else {
            InetAddress match = criteria.isAcceptable(loopbackInterface, loopbackAddress);
            if (!loopbackAddress.equals(match)) {
                // This match fails because ::1%lo becomes ::1%<number_of_lo> which isn't 0
                assertNull(match + " is invalid", criteria.isAcceptable(loopbackInterface, loopbackAddress));
            }
        }
        criteria = new InetAddressMatchInterfaceCriteria("::1%" + loopbackAddress.getScopeId());
        assertEquals(loopbackAddress, criteria.isAcceptable(loopbackInterface, loopbackAddress));
        criteria = new InetAddressMatchInterfaceCriteria("::1%" + (loopbackAddress.getScopeId() + 1));
        assertNull(criteria.isAcceptable(loopbackInterface, loopbackAddress));
    }

    @Test
    public void testNonLoopback() throws Exception {

        for (Map.Entry<NetworkInterface, Set<Inet6Address>> entry : addresses.entrySet()) {
            NetworkInterface nif = entry.getKey();
            for (Inet6Address address : entry.getValue()) {
                String hostAddress = address.getHostAddress();
                int pos = hostAddress.indexOf('%');
                if (pos > -1) {
                    hostAddress = hostAddress.substring(0, pos);
                }

                matchCriteriaTest(hostAddress, nif, address, true);
                if (!nif.isVirtual() && (address.isLinkLocalAddress() || address.isSiteLocalAddress())) {
                    matchCriteriaTest(hostAddress + "%" + nif.getName(), nif, address, true);
                    matchCriteriaTest(hostAddress + "%" + address.getScopeId(), nif, address, true);
                    matchCriteriaTest(hostAddress + "%" + (address.getScopeId() + 1), nif, address, false);
                    matchCriteriaTest(hostAddress + "%bogus", nif, address, false);
                }
            }
        }
    }

    private static void matchCriteriaTest(String criteriaString, NetworkInterface nif, InetAddress address,
                                     boolean expectMatch) throws SocketException {
        InetAddressMatchInterfaceCriteria criteria = new InetAddressMatchInterfaceCriteria(criteriaString);
        if (expectMatch) {
            assertEquals(criteriaString, address, criteria.isAcceptable(nif, address));
        } else {
            assertNull(criteriaString, criteria.isAcceptable(nif, address));
        }
    }
}
