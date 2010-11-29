package com.mysema.stat;

import com.mysema.rdfbean.model.UID;

public final class SKOS {
    
    public static final String NS = "http://www.w3.org/2004/02/skos/core#";
    
    public static final UID broader = uid("broader");

    private static UID uid(String ln) {
        return new UID(NS, ln);
    }
    
    private SKOS() {}

}
