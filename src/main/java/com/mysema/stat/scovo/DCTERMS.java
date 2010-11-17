package com.mysema.stat.scovo;

import com.mysema.rdfbean.model.UID;

public final class DCTERMS {

    public static final String NS = "http://purl.org/dc/terms/";
    
    public static final UID created = new UID(NS, "created");
    
    public static final UID modified = new UID(NS, "modified");
    
    private DCTERMS() {}
}
