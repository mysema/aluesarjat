package com.mysema.stat.scovo;

import com.mysema.rdfbean.model.UID;

public final class SCV {
    
    public static final String NS = "http://purl.org/NET/scovo#";
    
    public static final UID Dataset = uid("Dataset");

    public static final UID Dimension = uid("Dimension");

    public static final UID dimension = uid("dimension");

    public static final UID Item = uid("Item");

    public static final UID dataset = uid("dataset");

    private static final UID uid(String ln) {
        return new UID(NS, ln);
    }
    
    private SCV() {}

}
