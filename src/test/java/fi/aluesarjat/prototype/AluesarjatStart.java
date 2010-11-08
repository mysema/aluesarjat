package fi.aluesarjat.prototype;

import com.mysema.commons.jetty.JettyHelper;

public class AluesarjatStart {
    
    public static void main(String[] args) throws Exception{
        JettyHelper.startJetty("src/main/webapp", "/", 8080, 8443);
    }

}