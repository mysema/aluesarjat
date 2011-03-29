package fi.aluesarjat.prototype;

import com.mysema.rdfbean.model.QLIT;
import com.mysema.rdfbean.model.QUID;

public final class Constants {

    public static final QUID dataset = new QUID("dataset");

    public static final QUID dimensionType = new QUID("dimensionType");

    public static final QLIT dimensionTypeName = new QLIT("dimensionTypeName");

    public static final QUID dimension = new QUID("dimension");

    public static final QLIT dimensionDescription = new QLIT("dimensionDescription");

    public static final QLIT dimensionName = new QLIT("dimensionName");

    public static final QLIT value = new QLIT("value");

    public static final QUID item = new QUID("item");

    public static final QUID parent = new QUID("parent");

    private Constants(){}

}
