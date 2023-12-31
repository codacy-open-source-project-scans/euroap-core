/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.cli.completion.address.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.jboss.as.cli.completion.mock.MockNode;
import org.junit.Test;


/**
 *
 * @author Alexey Loubyansky
 */
public class PathNavigatorsTestCase extends AbstractAddressCompleterTest {

    public PathNavigatorsTestCase() {
        super();

        MockNode root = addRoot("type1");
        root.addChild("name11");
        root.addChild("name12").addChild("type2").addChild("child21");
    }

    @Test
    public void testRoot1() {
        //assertEquals(Arrays.asList("type1=name11", "type1=name12"), fetchCandidates("./"));
        assertEquals(Arrays.asList("type1="), fetchCandidates("./"));
    }

    @Test
    public void testRoot2() {
        //assertEquals(Arrays.asList("type1=name11", "type1=name12"), fetchCandidates("/"));
        assertEquals(Arrays.asList("type1="), fetchCandidates("/"));
    }

    @Test
    public void testRoot3() {

        try {
            ctx.getCurrentNodePath().toNode("type1", "name12");
            //assertEquals(Arrays.asList("type2=child21"), fetchCandidates("./"));
            assertEquals(Arrays.asList("type2="), fetchCandidates("./"));
            //assertEquals(Arrays.asList("type1=name11", "type1=name12"), fetchCandidates("/"));
            assertEquals(Arrays.asList("type1="), fetchCandidates("/"));
            assertEquals(Arrays.asList("name11", "name12"), fetchCandidates("/type1=n"));
        } finally {
            ctx.getCurrentNodePath().reset();
        }
    }

    @Test
    public void testParentNode() {
        //assertEquals(Arrays.asList("type2=child21"), fetchCandidates("./type1=name11/../type1=name12/t"));
        assertEquals(Arrays.asList("type2="), fetchCandidates("./type1=name11/../type1=name12/t"));
    }

    @Test
    public void testParentAtTheBeginning() {

        try {
            ctx.getCurrentNodePath().toNode("type1", "name12");
            //assertEquals(Arrays.asList("type2=child21"), fetchCandidates("./"));
            assertEquals(Arrays.asList("type2="), fetchCandidates("./"));
            //assertEquals(Arrays.asList("type1=name11", "type1=name12"), fetchCandidates("../"));
            assertEquals(Arrays.asList("type1="), fetchCandidates("../"));
        } finally {
            ctx.getCurrentNodePath().reset();
        }
    }

    @Test
    public void testParentAtTheEnd() {
        assertEquals(Collections.emptyList(), fetchCandidates("/type1=name12/type2=name21/.."));
    }

    @Test
    public void testNodeType() {
        assertEquals(Arrays.asList("name11", "name12"), fetchCandidates("./type1=name11/.type/n"));
    }
}
