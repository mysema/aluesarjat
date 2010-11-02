package com.mysema.stat.scovo;

import com.mysema.commons.lang.Assert;

public final class XMLID {

    public static class CharRange {
        private char start;
        private char end;
        public CharRange(char ch) {
            this(ch, ch);
        }
        public CharRange(char start, char end) {
            Assert.assertThat(start <= end, "invalid range", null, null);
            this.start = start;
            this.end = end;
        }
        public boolean contains(char ch) {
            return start <= ch && ch <= end;
        }
    }
    
    public static class CharRanges {
        private CharRanges[] nestedRanges;
        private CharRange[] ranges;
        public CharRanges(CharRanges... nestedRanges) {
            this(nestedRanges, (CharRange[]) null);
        }
        public CharRanges(CharRange... ranges) {
            this((CharRanges[]) null, ranges);
        }
        public CharRanges(CharRanges nestedRange, CharRange... ranges) {
            this(new CharRanges[] {nestedRange}, ranges);
        }
        public CharRanges(CharRanges[] nestedRanges, CharRange... ranges) {
            this.nestedRanges = nestedRanges;
            this.ranges = ranges;
        }
        public boolean contains(char ch) {
            if (nestedRanges != null) {
                for (CharRanges ranges : nestedRanges) {
                    if (ranges.contains(ch)) {
                        return true;
                    }
                }
            }
            if (ranges != null) {
                for (CharRange range : ranges) {
                    if (range.contains(ch)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
    
    public static CharRange range(char ch) {
        return new CharRange(ch);
    }
    
    public static CharRange range(char start, char end) {
        return new CharRange(start, end);
    }
    
    private static final CharRanges ID_START_CHARS = new CharRanges(
            range('A', 'Z'),
            range('_'),
            range('a', 'z'),
            range('\u00C0', '\u00D6'),
            range('\u00D8', '\u00F6'), 
            range('\u00F8', '\u02FF'), 
            range('\u0370', '\u037D'), 
            range('\u037F', '\u1FFF'), 
            range('\u200C', '\u200D'), 
            range('\u2070', '\u218F'), 
            range('\u2C00', '\u2FEF'), 
            range('\u3001', '\uD7FF'), 
            range('\uF900', '\uFDCF'), 
            range('\uFDF0', '\uFFFD')
    );
 
    private static final CharRanges ID_CHARS = new CharRanges(
            ID_START_CHARS,
            range('-'),
            range('.'),
            range('0', '9'),
            range('\u00B7'),
            range('\u0300', '\u036F'),
            range('\u203F', '\u2040')
    );
    
    public static String toXMLID(String name) {
        Assert.hasLength(name, "name");

        StringBuilder sb = new StringBuilder(name.length() + 10);
        char[] chars = name.toCharArray();

        if (ID_START_CHARS.contains(chars[0])) {
            sb.append(chars[0]);
        } else {
            sb.append('_');
            if (ID_CHARS.contains(chars[0])) {
                sb.append(chars[0]);
            } else if (!Character.isWhitespace(chars[0])) {
                sb.append(encode(chars[0]));
            }
        }
        
        for (int i=1; i < chars.length; i++) {
            if (ID_CHARS.contains(chars[i])) {
                sb.append(chars[i]);
            } else if (Character.isWhitespace(chars[i])) {
                sb.append('_');
            } else {
                sb.append(encode(chars[i]));
            }
        }
        return sb.toString();
    }
    
    private static String encode(char ch) {
        return Integer.toString(ch, Character.MAX_RADIX);
    }
    
    private XMLID(){}
    
}
