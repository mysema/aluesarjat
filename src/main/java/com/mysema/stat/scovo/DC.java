package com.mysema.stat.scovo;

import com.mysema.rdfbean.model.UID;

public final class DC {

    public static final String NS = "http://purl.org/dc/elements/1.1/";
    
    public static final UID title = new UID(NS, "title");
    
    public static final UID description = new UID(NS, "description");
    
    private DC() {}
}
