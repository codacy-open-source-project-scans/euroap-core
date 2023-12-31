/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.process.stdin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test of {@link org.jboss.as.process.stdin.Base64InputStream}.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public class Base64InputStreamTestCase {

    private static final byte[] input = {(byte) 1, (byte) 2,(byte) 3, (byte) 4,(byte) 5,(byte) 6,
            (byte) 7,(byte) 8,(byte) 9, (byte) 10};

    @Test
    public void testRemainOpenOnPad() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Base64OutputStream b64os = getOut(baos);
        b64os.write(input, 0, 1);
        b64os.close();
        b64os = getOut(baos);
        b64os.write(input, 1, 2);
        b64os.close();
        b64os = getOut(baos);
        b64os.write(input, 3, 3);
        b64os.close();
        b64os = getOut(baos);
        b64os.write(input, 6, 4);
        b64os.close();

        byte[] output = baos.toByteArray();

        Base64InputStream b64is = getIn(output);
        byte[] result = new byte[10];
        Assert.assertEquals(10, b64is.read(result, 0, 10));
        Assert.assertArrayEquals(input, result);

        b64is = getIn(output);
        result = new byte[10];
        Assert.assertEquals(1, b64is.read(result, 0, 1));
        byte[] correct = new byte[10];
        correct[0] = (byte) 1;
        Assert.assertArrayEquals(correct, result);

        b64is = getIn(output);
        result = new byte[10];
        Assert.assertEquals(2, b64is.read(result, 0, 2));
        correct[1] = (byte) 2;
        Assert.assertArrayEquals(correct, result);

        b64is = getIn(output);
        result = new byte[10];
        Assert.assertEquals(3, b64is.read(result, 0, 3));
        correct[2] = (byte) 3;
        Assert.assertArrayEquals(correct, result);

        b64is = getIn(output);
        result = new byte[10];
        Assert.assertEquals(4, b64is.read(result, 0, 4));
        correct[3] = (byte) 4;
        Assert.assertArrayEquals(correct, result);

        b64is = getIn(output);
        result = new byte[10];
        Assert.assertEquals(2, b64is.read(result, 0, 2));
        Assert.assertEquals(5, b64is.read(result, 2, 5));
        correct[4] = (byte) 5;
        correct[5] = (byte) 6;
        correct[6] = (byte) 7;
        Assert.assertArrayEquals(correct, result);
    }


    private static Base64OutputStream getOut(OutputStream sink) {
        return new Base64OutputStream(sink);
    }

    private static Base64InputStream getIn(byte[] source) {
        ByteArrayInputStream bais = new ByteArrayInputStream(source);
        return new Base64InputStream(bais);
    }
}
