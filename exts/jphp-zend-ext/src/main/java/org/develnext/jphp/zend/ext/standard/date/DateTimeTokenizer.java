package org.develnext.jphp.zend.ext.standard.date;

import static org.develnext.jphp.zend.ext.standard.date.DateTimeTokenizer.BufferCharacteristics.DIGITS;
import static org.develnext.jphp.zend.ext.standard.date.DateTimeTokenizer.BufferCharacteristics.LETTERS;
import static org.develnext.jphp.zend.ext.standard.date.DateTimeTokenizer.BufferCharacteristics.PUNCTUATION;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.regex.Pattern;

class DateTimeTokenizer {
    static final Pattern HOUR_hh = Pattern.compile("0?[1-9]|1[0-2]");
    static final Pattern HOUR_HH = Pattern.compile("[01][0-9]|2[0-4]");
    static final Pattern HOUR_12 = HOUR_hh;
    static final Pattern HOUR_24 = HOUR_HH;
    static final Pattern TWO_DIGIT_MINUTE = Pattern.compile("[0-5][0-9]");
    static final Pattern MINUTE_ii = Pattern.compile("0?[0-9]|[0-5][0-9]");
    static final Pattern SECOND_ss = MINUTE_ii;
    static final Pattern TWO_DIGIT_SECOND = TWO_DIGIT_MINUTE;
    static final Pattern FRACTION = Pattern.compile("\\.[0-9]+");
    static final Pattern MERIDIAN = Pattern.compile("[AaPp]\\.?[Mm]\\.?\t?");
    static final Pattern TWO_DIGIT_MONTH = Pattern.compile("[0-1][0-9]");
    static final Pattern MONTH_mm = Pattern.compile("0?[0-9]|1[0-2]");
    static final Pattern DAY_dd = Pattern.compile("([0-2]?[0-9]|3[01])");
    static final Pattern TWO_DIGIT_DAY = Pattern.compile("0[0-9]|[1-2][0-9]|3[01]");
    static final Pattern DAY_OF_YEAR = Pattern.compile("00[1-9]|0[1-9][0-9]|[1-2][0-9][0-9]|3[0-5][0-9]|36[0-6]");
    static final Pattern WEEK = Pattern.compile("0[1-9]|[1-5][0-3]");

    private static final int UNDEFINED_POSITION = -1;
    private final char[] chars;
    private final int length;
    private final EnumSet<BufferCharacteristics> characteristics;
    private final StringBuilder buff;
    private int cursor;
    private int tokenStart;

    public DateTimeTokenizer(String dateTime) {
        this.chars = dateTime.toCharArray();
        this.length = chars.length;
        this.tokenStart = UNDEFINED_POSITION;
        this.buff = new StringBuilder();
        this.characteristics = EnumSet.noneOf(BufferCharacteristics.class);
    }

    Token next() {
        if (isEnd()) // end reached
            return Token.EOF;

        Token next = null;

        char[] chars = this.chars;
        int i = cursor;

        loop:
        while (i < length) {
            char c = chars[i];

            s0:
            switch (c) {
                case 'a':
                case 'A':
                case 'p':
                case 'P':
                case 'm':
                case 'M':
                case 'W':
                    if (isUndefined())
                        tokenStart = i;

                    buff.append(c);
                    characteristics.add(LETTERS);
                    break;
                case 't':
                case 'T': {
                    buff.append(c);
                    if (isNextDigit(i)) {
                        next = Token.of(Symbol.CHARACTER, i++, 1);
                        resetBuffer();
                        break loop;
                    }
                    break;
                }
                // DIGITS
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9': {
                    if (isUndefined())
                        tokenStart = i;

                    if (Character.isLetter(lastChar())) {
                        next = createAndReset(Symbol.CHARACTER);
                        break loop;
                    }

                    buff.append(c);
                    characteristics.add(DIGITS);

                    if (isNextDigit(i)) {
                        // next character is digit. continuing...
                        break;
                    } else {
                        // advancing pointer to non digit character
                        i++;
                        next = createAndReset(Symbol.DIGITS);

                        // break iteration and hoping to read token with digits.
                        break loop;
                    }
                }
                // PUNCTUATION
                case ':': {
                    if (isUndefined()) {
                        // empty buffer indicates that nothing has been parsed
                        next = Token.of(Symbol.COLON, i++, 1);
                    } else {
                        // buffer is not empty and we reached colon so we must return
                        next = createWithGuessedSymbol();
                    }
                    resetBuffer();
                    break loop;
                }
                case '.': {
                    if (isUndefined()) {
                        next = Token.of(Symbol.DOT, i++, 1);
                        resetBuffer();
                        break loop;
                    } else {
                        char lc = lastChar();
                        buff.append(c);

                        // lookbehind
                        switch (lc) {
                            case 'a':
                            case 'A':
                            case 'p':
                            case 'P':
                                break s0; // hoping to read MERIDIAN. Breaks main switch
                            case 'm':
                            case 'M':
                                if (MERIDIAN.matcher(buff).matches()) {
                                    next = createAndReset(Symbol.MERIDIAN);
                                }
                                break s0;
                            default:
                                // most probably this is DIGITS
                                next = createWithGuessedSymbol();
                                resetBuffer();
                                break loop;

                        }
                    }
                }
                case '/': {
                    next = Token.of(Symbol.SLASH, i++, 1);
                    break loop;
                }
                case '@': {
                    next = Token.of(Symbol.AT, i++, 1);
                    break loop;
                }
                case '-': {
                    // previous one not digit next one is digit
                    if (!Character.isDigit(lookahead(i, -1)) && isNextDigit(i)) {
                        // this is most probably negative number
                        if (isUndefined())
                            tokenStart = i;

                        buff.append(i);
                        break;
                    }

                    next = Token.of(Symbol.MINUS, i++, 1);
                    break loop;
                }
                case ' ':
                case '\t': {
                    next = Token.of(Symbol.SPACE, i++, 1);
                    break loop;
                }
            }

            i++;
        }

        cursor = i;

        // when reached and still can not find token.
        if (isEnd() && next == null) {
            Symbol symbol = guessSymbol();
            next = Token.of(symbol, tokenStart, buff.length());
            resetBuffer();
        }

        return next;
    }

    private boolean isNextDigit(int i) {
        return Character.isDigit(lookahead(i, 1));
    }

    char[] read(Token token) {
        char[] dst = new char[token.length()];
        System.arraycopy(chars, token.start(), dst, 0, dst.length);
        return dst;
    }

    String readString(Token token) {
        return String.valueOf(read(token));
    }

    CharBuffer readCharBuffer(Token token) {
        return CharBuffer.wrap(chars, token.start(), token.length()).asReadOnlyBuffer();
    }

    CharBuffer readCharBuffer(int start, int length) {
        return CharBuffer.wrap(chars, start, length).asReadOnlyBuffer();
    }

    private char lookahead(int current, int to) {
        if (current + to < length && current + to > 0)
            return chars[current + to];

        return '\0';
    }

    private char lastChar() {
        int idx = buff.length() - 1;
        return idx >= 0 ? buff.charAt(idx) : '\0';
    }

    private Token createWithGuessedSymbol() {
        return Token.of(guessSymbol(), tokenStart, buff.length());
    }

    private Token createAndReset(Symbol symbol) {
        final Token token = Token.of(symbol, tokenStart, buff.length());
        resetBuffer();
        return token;
    }

    private boolean isEnd() {
        return cursor == length;
    }

    private boolean isUndefined() {
        return tokenStart == UNDEFINED_POSITION;
    }

    private void resetBuffer() {
        buff.setLength(0);
        characteristics.clear();
        tokenStart = UNDEFINED_POSITION;
    }

    private Symbol guessSymbol() {
        int buffLen = buff.length();
        boolean onlyDigits = hasOnly(DIGITS);

        switch (buffLen) {
            case 1:
                if (onlyDigits) {
                    return Symbol.HOUR_12;
                }
                break;
            case 2: {
                if (onlyDigits) {
                    if (HOUR_12.matcher(buff).matches()) {
                        return Symbol.HOUR_12;
                    } else if (HOUR_24.matcher(buff).matches()) {
                        return Symbol.TWO_DIGITS;
                    }
                }
                break;
            }
        }

        if (onlyDigits) {
            return Symbol.DIGITS;
        }

        if (hasOnly(DIGITS, PUNCTUATION) && FRACTION.matcher(buff).matches()) {
            return Symbol.FRACTION;
        } else if ((hasOnly(LETTERS) || hasOnly(LETTERS, PUNCTUATION)) && MERIDIAN.matcher(buff).matches()) {
            return Symbol.MERIDIAN;
        }

        throw new IllegalStateException("Cannot guest type of " + buff.toString());
    }

    private boolean hasOnly(BufferCharacteristics... many) {
        return characteristics.size() == many.length && characteristics.containsAll(Arrays.asList(many));
    }

    public char readChar(Token token) {
        return chars[token.start()];
    }

    public long readLong(Token token) {
        if (token.symbol() != Symbol.DIGITS) {
            throw new IllegalArgumentException();
        }

        CharBuffer cb = readCharBuffer(token);
        int start = token.start();
        return toLong(cb, start);
    }

    private long toLong(CharBuffer cb, int start) {
        long result = 0;
        int sign = 1;

        // if number is signed we should consume sign and save it
        char first = cb.get(start);
        if (first == '-' || first == '+') {
            cb.get();
            sign = first == '-' ? -1 : 1;
        }

        while (cb.hasRemaining()) {
            result *= 10;
            result += cb.get() - '0';
        }

        return result * sign;
    }

    public int readInt(Token token) {
        return (int) readLong(token);
    }

    public int readInt(int start, int length) {
        return (int) toLong(readCharBuffer(start, length), start);
    }

    enum BufferCharacteristics {
        DIGITS,
        LETTERS,
        PUNCTUATION
    }
}
