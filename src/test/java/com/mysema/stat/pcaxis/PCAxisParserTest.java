package com.mysema.stat.pcaxis;

import static junit.framework.Assert.assertEquals;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.RecognitionException;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mysema.stat.pcaxis.Key;
import com.mysema.stat.pcaxis.PCAxis;

public class PCAxisParserTest {
    
    private static Map<Key, List<Object>> px;

    @BeforeClass
    public static void init() {
        px = PCAxis.parse(PCAxisParserTest.class.getResourceAsStream("/example-1.px"));
    }
    
    @Test
    public void singleStringValue() throws IOException, RecognitionException {
        List<Object> values = px.get(new Key("CHARSET"));
        assertEquals("ANSI", values.get(0));
    }
        
    @Test
    public void multilineString() {
        List<Object> values = px.get(PCAxis.TITLE);
        assertEquals(2, values.size());
        assertEquals("S1. Työpaikat yhteensä alkaen 31.12.1987, toimialaluokitus TOL-95 mukaan 31.12 1993-2000   ", values.get(0));
        assertEquals("   ja TOL-2002 mukaan alkaen 31.12.2001-", values.get(1));
    }
    
    @Test
    public void keySpecifier() {
        List<Object> values = px.get(new Key("VALUES", "Alue"));
        assertEquals(18, values.size());
        assertEquals("049 Espoo", values.get(1));
        assertEquals("Helsingin seutu", values.get(16));
    }
    
    @Test
    public void data() {
        List<Object> values = px.get(PCAxis.DATA);
        assertEquals(21 * 38, values.size()); // 21 columns * (2 * 19) rows 
        assertEquals(new BigDecimal(370772), values.get(0));
        assertEquals(".", values.get(21));
        assertEquals(new BigDecimal(24447), values.get(21*38 - 1));
    }

    @Test
    public void singleBigDecimalValue() {
        List<Object> values = px.get(new Key("DECIMALS"));
        assertEquals(BigDecimal.ZERO, values.get(0));
    }
    
    @Test
    public void keyCount() {
        assertEquals(22, px.size());
    }

}
