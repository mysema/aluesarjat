package com.mysema.stat.scovo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class XMLIDTest {

    @Test
    public void toXMLID(){
        assertEquals("_123_ABC", XMLID.toXMLID(" 123 ABC"));
        assertEquals("ABC",      XMLID.toXMLID(" ABC"));
        assertEquals("A_B",      XMLID.toXMLID("A  B"));
    }
    
}
