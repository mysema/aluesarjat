package com.mysema.stat.scovo;

import com.mysema.rdfbean.model.UID;

public class GEO {

    public static final String NS = "http://www.w3.org/2003/01/geo/";
    
    public static final UID Point = new UID(NS, "Point");
    
    public static final UID SpatialThing  = new UID(NS, "SpatialThing "); 
    
    public static final UID where = new UID(NS, "where");
    
    public static final UID shape = new UID(NS, "shape");
    
    public static final UID face = new UID(NS, "face");
    
    public static final UID lat = new UID(NS, "lat");
    
    public static final UID long_ = new UID(NS, "long");
    
    public static final UID lat_long = new UID(NS, "lat_long");
    
    public static final UID line = new UID(NS, "line");
    
    public static final UID polygon = new UID(NS, "polygon");
    
}
