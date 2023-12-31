/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.cli.operation;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineFormat;
import org.jboss.as.cli.parsing.ParsingStateCallbackHandler;


/**
 *
 * @author Alexey Loubyansky
 */
public interface CommandLineParser {

    interface CallbackHandler {

        void start(String operationString);

        void rootNode(int index) throws OperationFormatException;

        void parentNode(int index);

        void nodeType(int index);

        void nodeType(int index, String nodeType) throws OperationFormatException;

        void nodeTypeNameSeparator(int index);

        void nodeName(int index, String nodeName) throws OperationFormatException;

        void nodeSeparator(int index);

        void addressOperationSeparator(int index) throws CommandFormatException;

        void operationName(int index, String operationName) throws CommandFormatException;

        void propertyListStart(int index);

        void propertyName(int index, String propertyName) throws CommandFormatException;

        void propertyNameValueSeparator(int index);

        void property(String name, String value, int nameValueSeparatorIndex) throws CommandFormatException;

        void propertyNoValue(int index, String name) throws CommandFormatException;

        void notOperator(int index);

        void propertySeparator(int index);

        void propertyListEnd(int index);

        // TODO this is not good
        void nodeTypeOrName(int index, String typeOrName) throws OperationFormatException;

        void headerListStart(int index);

        ParsingStateCallbackHandler headerName(int index, String name) throws CommandFormatException;

        void header(String name, String value, int nameValueSeparator) throws CommandFormatException;

        void headerNameValueSeparator(int nameValueSeparator) throws CommandFormatException;

        void headerSeparator(int index);

        void headerListEnd(int index);

        void outputTarget(int index, String outputTarget) throws CommandFormatException;

        void setFormat(CommandLineFormat format);

        void operator(int index);
    }

    void parse(String operationRequest, CallbackHandler handler) throws OperationFormatException;
}
