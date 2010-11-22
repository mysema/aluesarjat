package com.mysema.stat;

import com.mysema.rdfbean.model.UID;

public final class STAT {
    
    public static final String NS = "http://data.mysema.com/schemas/stat#";
    
    public static final UID datasetDimension = uid("datasetDimension");
    
    public static final UID units = uid("units");

    private static UID uid(String ln) {
        return new UID(NS, ln);
    }
    
    private STAT() {}

}
