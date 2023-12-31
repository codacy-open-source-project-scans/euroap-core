/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.core.cli.command.aesh;

import java.util.Collection;
import java.util.List;
import org.aesh.readline.AeshContext;
import org.aesh.command.Command;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.readline.terminal.formatting.TerminalString;
import org.jboss.as.cli.CommandContext;

/**
 * A CLI specific {@code CompleterInvocation} that exposes the
 * {@link org.jboss.as.cli.CommandContext}.
 *
 * @author jdenise@redhat.com
 */
public class CLICompleterInvocation implements CompleterInvocation {

    private final CompleterInvocation delegate;
    private final CommandContext commandContext;

    public CLICompleterInvocation(CompleterInvocation delegate, CommandContext ctx) {
        this.delegate = delegate;
        this.commandContext = ctx;
    }

    @Override
    public String getGivenCompleteValue() {
        return delegate.getGivenCompleteValue();
    }

    @Override
    public Command getCommand() {
        return delegate.getCommand();
    }

    @Override
    public List<TerminalString> getCompleterValues() {
        return delegate.getCompleterValues();
    }

    @Override
    public void setCompleterValuesTerminalString(List<TerminalString> terminalStrings) {
        delegate.setCompleterValuesTerminalString(terminalStrings);
    }

    @Override
    public void clearCompleterValues() {
        delegate.clearCompleterValues();
    }

    @Override
    public void addCompleterValue(String s) {
        delegate.addCompleterValue(s);
    }

    @Override
    public void addCompleterValueTerminalString(TerminalString terminalString) {
        delegate.addCompleterValueTerminalString(terminalString);
    }

    @Override
    public boolean isAppendSpace() {
        return delegate.isAppendSpace();
    }

    @Override
    public void setAppendSpace(boolean b) {
        delegate.setAppendSpace(b);
    }

    @Override
    public void setIgnoreOffset(boolean ignoreOffset) {
        delegate.setIgnoreOffset(ignoreOffset);
    }

    @Override
    public boolean doIgnoreOffset() {
        return delegate.doIgnoreOffset();
    }

    @Override
    public void setOffset(int offset) {
        delegate.setOffset(offset);
    }

    @Override
    public int getOffset() {
        return delegate.getOffset();
    }

    @Override
    public void setIgnoreStartsWith(boolean ignoreStartsWith) {
        delegate.setIgnoreStartsWith(ignoreStartsWith);
    }

    @Override
    public boolean isIgnoreStartsWith() {
        return delegate.isIgnoreStartsWith();
    }

    @Override
    public AeshContext getAeshContext() {
        return delegate.getAeshContext();
    }

    public CommandContext getCommandContext() {
        return commandContext;
    }

    @Override
    public void setCompleterValues(Collection<String> completerValues) {
        delegate.setCompleterValues(completerValues);
    }

    @Override
    public void addAllCompleterValues(Collection<String> completerValues) {
        delegate.addAllCompleterValues(completerValues);
    }
}
