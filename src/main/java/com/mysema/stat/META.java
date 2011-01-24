package com.mysema.stat;

import com.mysema.rdfbean.model.UID;

/**
 * @author sasa
 *
 */
public final class META {
    
    public static final String NS = "http://data.mysema.com/schemas/meta#";
    
    public static final UID instances = new UID(NS, "instances");
    
    public static final UID nsPrefix = new UID(NS, "nsPrefix");
    
    private META() {}

}
