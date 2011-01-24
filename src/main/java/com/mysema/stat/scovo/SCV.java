package com.mysema.stat.scovo;

import com.mysema.rdfbean.model.UID;

/**
 * @author sasa
 *
 */
public final class SCV {
    
    public static final String NS = "http://purl.org/NET/scovo#";
    
    public static final UID Dataset = new UID(NS, "Dataset");

    public static final UID Dimension = new UID(NS, "Dimension");

    public static final UID dimension = new UID(NS, "dimension");

    public static final UID Item = new UID(NS, "Item");

    public static final UID dataset = new UID(NS, "dataset");
    
    private SCV() {}

}
