package me.Domplanto.streamLabs.font;

import java.util.Arrays;

public enum DefaultFontInfo {
    f('f', 4),
    I('I', 3),
    i('i', 1),
    k('k', 4),
    l('l', 1),
    t('t', 4),
    EXCLAMATION_POINT('!', 1),
    AT_SYMBOL('@', 6),
    LEFT_PARENTHESIS('(', 4),
    RIGHT_PARENTHESIS(')', 4),
    LEFT_CURL_BRACE('{', 4),
    RIGHT_CURL_BRACE('}', 4),
    LEFT_BRACKET('[', 3),
    RIGHT_BRACKET(']', 3),
    COLON(':', 1),
    SEMI_COLON(';', 1),
    DOUBLE_QUOTE('"', 3),
    SINGLE_QUOTE('\'', 1),
    LEFT_ARROW('<', 4),
    RIGHT_ARROW('>', 4),
    LINE('|', 1),
    TICK('`', 2),
    PERIOD('.', 1),
    COMMA(',', 1),
    SPACE(' ', 3),
    DEFAULT('a', 5);

    private final static int CENTER_PX = 154;
    private final char character;
    private final int length;

    DefaultFontInfo(char character, int length) {
        this.character = character;
        this.length = length;
    }

    public char getCharacter() {
        return this.character;
    }

    public int getLength() {
        return this.length;
    }

    public int getBoldLength() {
        if (this == DefaultFontInfo.SPACE) return this.getLength();
        return this.length + 1;
    }

    public static DefaultFontInfo getDefaultFontInfo(char c) {
        return Arrays.stream(values())
                .filter(info -> info.getCharacter() == c)
                .findFirst().orElse(DEFAULT);
    }

    public static String centerMessage(String message) {
        return centerMessage(message, ' ');
    }

    public static String centerMessage(String message, char spaceChar) {
        if (message == null || message.isEmpty()) return message;

        int messagePxSize = 0;
        boolean isBold = false;
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == '§') continue;
            if (i > 0 && message.charAt(i - 1) == '§') {
                isBold = c == 'l' || c == 'L';
                continue;
            }

            DefaultFontInfo dFI = getDefaultFontInfo(c);
            messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
            messagePxSize++;
        }

        int spacePixelAmount = CENTER_PX - (messagePxSize / 2);
        int spaceLength = getDefaultFontInfo(spaceChar).getLength() + 1;
        int compensated = 0;
        StringBuilder sb = new StringBuilder();
        while(compensated < spacePixelAmount){
            sb.append(spaceChar);
            compensated += spaceLength;
        }

        return sb + message;
    }
}
