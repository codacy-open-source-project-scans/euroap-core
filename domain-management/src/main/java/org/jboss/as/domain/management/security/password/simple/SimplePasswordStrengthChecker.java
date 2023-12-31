/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.domain.management.security.password.simple;

import static org.wildfly.common.Assert.checkNotNullParam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.as.domain.management.logging.DomainManagementLogger;
import org.jboss.as.domain.management.security.password.CompoundRestriction;
import org.jboss.as.domain.management.security.password.Dictionary;
import org.jboss.as.domain.management.security.password.Keyboard;
import org.jboss.as.domain.management.security.password.PasswordRestriction;
import org.jboss.as.domain.management.security.password.PasswordStrengthCheckResult;
import org.jboss.as.domain.management.security.password.PasswordStrengthChecker;
import org.jboss.as.domain.management.security.password.PasswordValidationException;
import org.jboss.as.domain.management.security.password.RegexRestriction;

/**
 * @author baranowb
 *
 */
public class SimplePasswordStrengthChecker implements PasswordStrengthChecker {

    public static final String REGEX_DIGITS = "[0-9]";
    public static final String REGEX_SYMBOLS = "[^0-9a-zA-Z]";
    public static final String REGEX_ALPHA_UC = "[A-Z]";
    public static final String REGEX_ALPHA_LC = "[a-z]";
    public static final String REGEX_ALPHA = "[a-zA-Z]";

    protected static final int PWD_LEN_WEIGHT = 2;
    protected static final int REQUIREMENTS_WEIGHT = 10;
    protected static final int SYMBOLS_WEIGHT = 6;
    protected static final int DIGITS_WEIGHT = 4;
    protected static final int MIDDLE_NONCHAR_WEIGHT = 2;
    protected static final int ALPHA_WEIGHT = 2;

    protected static final int ALPHA_ONLY_WEIGHT = 2;
    protected static final int DIGITS_ONLY_WEIGHT = 4;
    protected static final int CONSECUTIVE_ALPHA_WEIGHT = 2;
    protected static final int CONSECUTIVE_DIGITS_WEIGHT = 2;
    protected static final int CONSECUTIVE_SYMBOLS_WEIGHT = 2;
    protected static final int DICTIONARY_WORD_WEIGHT = 1;
    protected static final int SEQUENTIAL_WEIGHT = 3;

    // this is not thread safe.
    private String userName;
    private String password;
    private int passwordLength;
    private final List<PasswordRestriction> restrictionsInPlace;
    private final Dictionary dictionary;
    private final Keyboard keyboard;
    private List<PasswordRestriction> adHocRestrictions;
    private SimplePasswordStrengthCheckResult result;

    public SimplePasswordStrengthChecker() {
        this.restrictionsInPlace = new ArrayList<PasswordRestriction>();
        this.dictionary = new SimpleDictionary();
        this.keyboard = new SimpleKeyboard();
    }

    public SimplePasswordStrengthChecker(final List<PasswordRestriction> initRestrictions, final Dictionary dictionary,
            final Keyboard keyboard) {
        this.restrictionsInPlace = Collections.unmodifiableList(checkNotNullParam("initRestrictions", initRestrictions));
        this.dictionary = dictionary;
        this.keyboard = keyboard;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.as.domain.management.security.password.PasswordStrengthChecker#check(java.lang.String, java.util.List)
     */
    @Override
    public PasswordStrengthCheckResult check(String userName, String password, List<PasswordRestriction> restictions) {
        try {
            this.userName = userName;
            this.password = password;
            this.passwordLength = this.password.length();
            this.adHocRestrictions = restictions;
            this.result = new SimplePasswordStrengthCheckResult();

            this.checkRestrictions();
            // positive checks
            result.positive(password.length() * PWD_LEN_WEIGHT);
            this.checkSymbols();
            this.checkDigits();
            this.checkMiddleNonChar();
            this.checkAlpha();

            // negatives checks
            this.checkAlphaOnly();
            this.checkNumbersOnly();
            this.checkConsecutiveAlpha();
            this.checkConsecutiveNumbers();
            this.checkConsecutiveSymbols();
            this.checkSequential();

            // now evaluate.
            this.result.calculateStrength();
        } finally {
            this.password = null;
            this.passwordLength = 0;
            this.adHocRestrictions = null;
        }
        return result;
    }

    protected void checkRestrictions() {
        int met = 0;
        // check addhoc first, those may be more important
        if (this.adHocRestrictions != null) {
            for (PasswordRestriction pr : this.adHocRestrictions) {
                if (pr instanceof CompoundRestriction) {
                    for (PasswordRestriction wrapped : ((CompoundRestriction) pr).getRestrictions()) {
                        try {
                            wrapped.validate(userName, password);
                            result.addPassedRestriction(wrapped);
                            met++;
                        } catch (PasswordValidationException pve) {
                            result.addRestrictionFailure(pve);
                        }
                    }
                } else {
                    try {
                        pr.validate(userName, password);
                        result.addPassedRestriction(pr);
                        met++;
                    } catch (PasswordValidationException pve) {
                        result.addRestrictionFailure(pve);
                    }
                }
            }
        }

        for (PasswordRestriction pr : this.restrictionsInPlace) {
            if (pr instanceof CompoundRestriction) {
                for (PasswordRestriction wrapped : ((CompoundRestriction) pr).getRestrictions()) {
                    try {
                        wrapped.validate(userName, password);
                        result.addPassedRestriction(wrapped);
                        met++;
                    } catch (PasswordValidationException pve) {
                        result.addRestrictionFailure(pve);
                    }
                }
            } else {
                try {
                    pr.validate(userName, password);
                    result.addPassedRestriction(pr);
                    met++;
                } catch (PasswordValidationException pve) {
                    result.addRestrictionFailure(pve);
                }
            }
        }
        this.result.positive(met * REQUIREMENTS_WEIGHT);
    }

    protected void checkSymbols() {
        int met = 0;
        Pattern symbolsPatter = Pattern.compile(REGEX_SYMBOLS + "?");
        Matcher matcher = symbolsPatter.matcher(this.password);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            if (start == end) {
                continue;
            }
            met++;
        }
        this.result.positive(met * REQUIREMENTS_WEIGHT);
    }

    protected void checkDigits() {
        int met = 0;
        Pattern symbolsPatter = Pattern.compile(REGEX_DIGITS + "?");
        Matcher matcher = symbolsPatter.matcher(this.password);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            if (start == end) {
                continue;
            }
            met++;
        }

        this.result.positive(met * DIGITS_WEIGHT);
    }

    protected void checkMiddleNonChar() {
        int met = 0;
        Pattern symbolsPatter = Pattern.compile(REGEX_SYMBOLS + "?");
        Matcher matcher = symbolsPatter.matcher(this.password);

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            if (start == end && start != 0 && end != this.passwordLength) {
                continue;
            }
            met++;
        }

        this.result.positive(met * MIDDLE_NONCHAR_WEIGHT);
    }

    protected void checkAlpha() {
        int met = 0;

        Pattern symbolsPatter = Pattern.compile(REGEX_ALPHA_UC + "?");
        Matcher matcher = symbolsPatter.matcher(this.password);

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            if (start == end) {
                continue;
            }
            met++;
        }

        met = 0;

        symbolsPatter = Pattern.compile(REGEX_ALPHA_LC + "?");
        matcher = symbolsPatter.matcher(this.password);

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            if (start == end) {
                continue;
            }
            met++;
        }

        this.result.positive((this.passwordLength - met) * ALPHA_WEIGHT);

    }

    protected void checkAlphaOnly() {
        Pattern symbolsPatter = Pattern.compile(REGEX_ALPHA + "*");
        Matcher matcher = symbolsPatter.matcher(this.password);
        if (matcher.find()) {
            if (matcher.end() == this.passwordLength) {
                // negative.
                this.result.negative(this.passwordLength * ALPHA_ONLY_WEIGHT);
            }
        }
    }

    protected void checkNumbersOnly() {
        Pattern symbolsPatter = Pattern.compile(REGEX_DIGITS + "*");
        Matcher matcher = symbolsPatter.matcher(this.password);
        if (matcher.find()) {
            if (matcher.end() == this.passwordLength) {
                // negative.
                this.result.negative(this.passwordLength * DIGITS_ONLY_WEIGHT);
            }
        }
    }

    // those could be incorporated with above, but that would blurry everything.
    protected void checkConsecutiveAlpha() {

        Pattern symbolsPatter = Pattern.compile(REGEX_ALPHA_UC + "+");
        Matcher matcher = symbolsPatter.matcher(this.password);
        int met = 0;
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            if (start == end) {
                continue;
            }
            int diff = end - start;
            if (diff >= 3) {
                met += diff;
            }

        }

        this.result.negative(met * CONSECUTIVE_ALPHA_WEIGHT);

        // alpha lower case

        symbolsPatter = Pattern.compile(REGEX_ALPHA_LC + "+");
        matcher = symbolsPatter.matcher(this.password);
        met = 0;
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            if (start == end) {
                continue;
            }
            int diff = end - start;
            if (diff >= 3) {
                met += diff;
            }

        }

        this.result.negative(met * CONSECUTIVE_ALPHA_WEIGHT);
    }

    protected void checkConsecutiveNumbers() {
        Pattern symbolsPatter = Pattern.compile(REGEX_DIGITS + "+");
        Matcher matcher = symbolsPatter.matcher(this.password);
        int met = 0;
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            if (start == end) {
                continue;
            }
            int diff = end - start;
            if (diff >= 3) {
                met += diff;
            }

        }

        this.result.negative(met * CONSECUTIVE_DIGITS_WEIGHT);
    }

    protected void checkConsecutiveSymbols() {
        Pattern symbolsPatter = Pattern.compile(REGEX_SYMBOLS + "+");
        Matcher matcher = symbolsPatter.matcher(this.password);
        int met = 0;
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            if (start == end) {
                continue;
            }
            int diff = end - start;
            if (diff >= 3) {
                met += diff;
            }

        }

        this.result.negative(met * CONSECUTIVE_SYMBOLS_WEIGHT);
    }

    protected void checkSequential() {
        // iterate over chars and check if siblings, if so, check how long is chain
        int chainSize = 0;
        for (int index = 0; index < this.passwordLength - 1; index++) {
            if (this.keyboard.siblings(this.password, index)) {
                chainSize += this.keyboard.sequence(this.password, index);
            } else {
                // nop
            }

        }
        if (chainSize > 0) {
            this.result.negative(chainSize * SEQUENTIAL_WEIGHT);
        }
    }

    protected void checkDictionary() {
        if (dictionary != null) {
            int score = dictionary.dictionarySequence(password);
            if (score > 0) {
                this.result.negative(score * DICTIONARY_WORD_WEIGHT);
            }
        }
    }

    public static PasswordRestriction getRestrictionAlpha(int minAlpha) {
        return createRegExRestriction(minAlpha, REGEX_ALPHA, DomainManagementLogger.ROOT_LOGGER.passwordMustHaveAlphaInfo(minAlpha),
                DomainManagementLogger.ROOT_LOGGER.passwordMustHaveAlpha(minAlpha));
    }

    public static PasswordRestriction getRestrictionDigit(int minDigit) {
        return createRegExRestriction(minDigit, REGEX_DIGITS, DomainManagementLogger.ROOT_LOGGER.passwordMustHaveDigitInfo(minDigit),
                DomainManagementLogger.ROOT_LOGGER.passwordMustHaveDigit(minDigit));
    }

    public static PasswordRestriction getRestrictionSymbol(int minSymbol) {
        return createRegExRestriction(minSymbol, REGEX_SYMBOLS, DomainManagementLogger.ROOT_LOGGER.passwordMustHaveSymbolInfo(minSymbol),
                DomainManagementLogger.ROOT_LOGGER.passwordMustHaveSymbol(minSymbol));
    }

    private static PasswordRestriction createRegExRestriction(int minChar, String regex, String requirementsMessage,
            String failureMessage) {
        if (minChar > 0) {
            return new RegexRestriction(String.format("(.*%s.*){%d}", REGEX_ALPHA, minChar), requirementsMessage,
                    failureMessage);
        } else {
            return new PasswordRestriction() {

                @Override
                public void validate(String userName, String password) throws PasswordValidationException {
                }

                @Override
                public String getRequirementMessage() {
                    return "";
                }
            };
        }
    }
}
