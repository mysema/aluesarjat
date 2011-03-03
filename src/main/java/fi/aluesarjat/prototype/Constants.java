package fi.aluesarjat.prototype;

import com.mysema.rdfbean.model.QID;
import com.mysema.rdfbean.model.QLIT;

public final class Constants {

    public static final QID dataset = new QID("dataset");

    public static final QID dimensionType = new QID("dimensionType");

    public static final QLIT dimensionTypeName = new QLIT("dimensionTypeName");

    public static final QID dimension = new QID("dimension");

    public static final QLIT dimensionDescription = new QLIT("dimensionDescription");

    public static final QLIT dimensionName = new QLIT("dimensionName");

    public static final QLIT value = new QLIT("value");

    public static final QID item = new QID("item");

    public static final QID parent = new QID("parent");

    private Constants(){}

}
