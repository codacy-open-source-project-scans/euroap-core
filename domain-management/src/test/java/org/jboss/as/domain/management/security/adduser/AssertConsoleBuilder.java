/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */


package org.jboss.as.domain.management.security.adduser;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import static org.junit.Assert.*;

import org.jboss.as.domain.management.logging.DomainManagementLogger;

/**
 *  Assert builder for the console. Use this together with the ConsoleMock object
 *  to validate the expected messages are displayed and answers. You should always
 *  recorded in the same order as the user gets it present.
 *
 *  This example shows how you can record  a chain of messages and answers
 *
 *  AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().
 *  expectedDisplayText("Please enter valid password").
 *  expectedDisplayText("\n").
 *  expectedInput("mysecretpassword");
 *
 *  ConsoleMock consoleMock = new ConsoleMock();
 *  consoleMock.setResponses(consoleBuilder);
 *
 *  ...
 *  ...
 *
 *  consoleBuilder.validate()
 *
*
* @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
*/
public class AssertConsoleBuilder {
    private static String NEW_LINE = String.format("%n");

    private enum Type {
        DISPLAY, INPUT
    }

    private class AssertConsole {

        private String text;
        private Type type;

        private AssertConsole() {
        }

        private AssertConsole(String text, Type type) {
            this.text = text;
            this.type = type;
        }
    }

    private Queue<AssertConsole> queue = new LinkedList<AssertConsole>();

    /**
     * Recorded the expected display text
     * @param text - the display text
     * @return this
     */
    public AssertConsoleBuilder expectedDisplayText(String text) {
        AssertConsole assertConsole = new AssertConsole();
        assertConsole.text = text;
        assertConsole.type = Type.DISPLAY;
        queue.add(assertConsole);
        return this;
    }

    /**
     * Expected input string from the user
     * @param text
     * @return this
     */
    public AssertConsoleBuilder expectedInput(String text) {
        AssertConsole assertConsole = new AssertConsole();
        assertConsole.text = text;
        assertConsole.type = Type.INPUT;
        queue.add(assertConsole);
        return this;
    }

    /**
     * Expected error message
     * @param text
     * @return this
     */
    public AssertConsoleBuilder expectedErrorMessage(String text) {
        queue.add(new AssertConsole(NEW_LINE,Type.DISPLAY));
        queue.add(new AssertConsole(" * ",Type.DISPLAY));
        queue.add(new AssertConsole(DomainManagementLogger.ROOT_LOGGER.errorHeader(),Type.DISPLAY));
        queue.add(new AssertConsole(" * ",Type.DISPLAY));
        queue.add(new AssertConsole(NEW_LINE,Type.DISPLAY));
        queue.add(new AssertConsole(text,Type.DISPLAY));
        queue.add(new AssertConsole(NEW_LINE,Type.DISPLAY));
        queue.add(new AssertConsole(NEW_LINE,Type.DISPLAY));
        return this;
    }

    /**
     * Expected confirm message and answer
     * @param messages - expected display text, if none pass in null
     * @param prompt - expected text for the console prompt
     * @param answer - expected answer
     * @return this
     */
    public AssertConsoleBuilder expectedConfirmMessage(String messages, String prompt, String answer) {
        if (messages!=null) {
            queue.add(new AssertConsole(messages,Type.DISPLAY));
            queue.add(new AssertConsole(NEW_LINE,Type.DISPLAY));
        }
        queue.add(new AssertConsole(prompt,Type.DISPLAY));
        queue.add(new AssertConsole(" ",Type.DISPLAY));
        queue.add(new AssertConsole(answer,Type.INPUT));
        return this;
    }

    /**
     * Assert the display text from the console, with the recorded display text.
     * if the doses't match it will fail
     * @param msg - display text from the console.
     * @return a String with  recorded display text
     */
    public String assertDisplayText(String msg) {
        AssertConsole assertConsole = queue.poll();

        if (assertConsole == null) {
            fail("Expected display text '"+msg+"' was not recorded");
        }

        if (!assertConsole.type.equals(Type.DISPLAY)) {
            fail("Wrong assert type, expect Type.DISPLAY");
        }
        assertEquals(assertConsole.text, msg);
        return assertConsole.text;
    }

    /**
     * Pop the next recorded answer to the console. if recorded is not
     * the Type.INPUT it will fail.
     * @return the recorded answer
     */
    public String popAnswer() {
        AssertConsole assertConsole = queue.poll();
        if (assertConsole == null) {
            fail("Expected answer was not recorded");
        }
        if (!assertConsole.type.equals(Type.INPUT)) {
            fail("Wrong assert type, expect Type.INPUT");
        }
        return assertConsole.text;
    }

    /**
     * validate if all recorded console assert has been asserted.
     * if not empty it will fail
     */
    public void validate() {
        StringBuffer notValidateAsserts = new StringBuffer();
        if (!queue.isEmpty()) {
            for (Iterator<AssertConsole> iterator = queue.iterator(); iterator.hasNext(); ) {
                AssertConsole assertConsole = iterator.next();
                notValidateAsserts.append("\"");
                notValidateAsserts.append(assertConsole.text);
                notValidateAsserts.append("\" ");
            }
        }
        assertTrue("There are still asserts in the queue that are not validated : "+notValidateAsserts.toString(),queue.isEmpty());
    }

}
