package fi.aluesarjat.prototype;

import com.mysema.commons.jetty.JettyHelper;

public class AluesarjatVirtuosoStart {

    public static void main(String[] args) throws Exception{
        System.setProperty("rdfbean.store", "virtuoso");
        JettyHelper.startJetty("src/main/webapp", "/", 8080, 8443);
    }

}
