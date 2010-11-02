package com.mysema.stat.scovo;

import com.mysema.commons.lang.Assert;

public final class XMLID {

    private static class CharRange {
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
    
    private static class CharRanges {
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
    
    private static final CharRanges ID_START_CHAR = new CharRanges(
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
 
    private static final CharRanges ID_CHAR = new CharRanges(
            ID_START_CHAR,
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

        append(sb, chars[0], ID_START_CHAR);
        
        return sb.toString();
    }

    private static void append(StringBuilder sb, char ch, CharRanges ranges) {
        if (ranges.contains(ch)) {
            sb.append(ch);
        } else {
            
        }
    }
    
    private XMLID(){}
    
}
