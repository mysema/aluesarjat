package com.mysema.stat;

import com.mysema.rdfbean.model.UID;

public final class META {
    
    public static final String NS = "http://data.mysema.com/schemas/meta#";
    
    public static final UID instances = uid("instances");
    
    public static final UID nsPrefix = uid("nsPrefix");

    private static UID uid(String ln) {
        return new UID(NS, ln);
    }
    
    private META() {}

}
