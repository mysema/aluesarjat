package com.mysema.stat.scovo;

import com.mysema.rdfbean.model.UID;

public final class META {
    
    public static final String NS = "http://data.mysema.com/schemas/meta#";
    
    public static final UID instances = uid("instances");

    private static UID uid(String ln) {
        return new UID(NS, ln);
    }
    
    private META() {}

}
