package com.mysema.stat.scovo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.CharSet;

import com.mysema.commons.lang.Assert;

public final class XMLID {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final CharSet ID_START_CHARS, ID_CHARS;

    static{
        List<String> idStartChars = Arrays.asList(
                "A-Z", "_", "a-z",
                "\u00C0-\u00D6", "\u00D8-\u00F6",
                "\u00F8-\u02FF", "\u0370-\u037D",
                "\u037F-\u1FFF", "\u200C-\u200D",
                "\u2070-\u218F", "\u2C00-\u2FEF",
                "\u3001-\uD7FF", "\uF900-\uFDCF",
                "\uFDF0-\uFDCF", "\uFDF0-\uFFFD");

       List<String> idChars = new ArrayList<String>(idStartChars);
       idChars.addAll(Arrays.asList(
               "-", "0-9",
               "\u00B7", "\u0300-\u036F",
               "\u203F-\u2040"));

       ID_START_CHARS = CharSet.getInstance(idStartChars.toArray(new String[idStartChars.size()]));
       ID_CHARS = CharSet.getInstance(idChars.toArray(new String[idChars.size()]));
    }

    public static String toXMLID(String name) {
        Assert.hasLength(name, "name");

        String normalizedName = WHITESPACE.matcher(name.trim()).replaceAll(" ");
        StringBuilder sb = new StringBuilder(normalizedName.length() + 10);
        char[] chars = normalizedName.toCharArray();

        if (ID_START_CHARS.contains(chars[0])) {
            sb.append(chars[0]);
        } else {
            sb.append('_');
            if (ID_CHARS.contains(chars[0])) {
                sb.append(chars[0]);
            } else if (!replaceWithUnderscore(chars[0])) { // No need to duplicate _
                sb.append(encode(chars[0]));
            }
        }

        for (int i=1; i < chars.length; i++) {
            if (ID_CHARS.contains(chars[i])) {
                sb.append(chars[i]);
            } else if (replaceWithUnderscore(chars[i])) {
                sb.append('_');
            } else {
                sb.append(encode(chars[i]));
            }
        }
        return sb.toString();
    }

    private static boolean replaceWithUnderscore(char ch) {
        return Character.isWhitespace(ch) || '.' == ch;
    }

    private static String encode(char ch) {
        return Integer.toString(ch, Character.MAX_RADIX);
    }

    private XMLID(){}

}
