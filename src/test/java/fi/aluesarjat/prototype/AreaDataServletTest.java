package fi.aluesarjat.prototype;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.servlet.ServletException;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class AreaDataServletTest extends AbstractServletTest{
    
    // Väkiluku    
//    value   alue:_091_1_Eteläinen_suurpiiri
//    value   dataset:A02S_HKI_Vakiluku1962
//    value   ikäryhmä:Väestö_yhteensä
//    value   vuosi:_2009

    // Asuntotuotanto (lukumäärä)
//    value   alue:_091_1_Eteläinen_suurpiiri
//    value   yksikkö:Asuntojen_lukumäärä
//    value   hallintaperuste:Asunnot_yhteensä
//    value   rahoitusmuoto:Yhteensä
//    value   talotyyppi:Yhteensä
//    value   huoneistotyyppi:Yhteensä
//    value   vuosi:_2009
    
    // Asuntotuotanto (pinta-ala m2)
//    value   alue:_091_1_Eteläinen_suurpiiri
//    value   yksikkö:Asuntojen_pinta-ala_m2
//    value   hallintaperuste:Asunnot_yhteensä
//    value   rahoitusmuoto:Yhteensä
//    value   talotyyppi:Yhteensä
//    value   huoneistotyyppi:Yhteensä
//    value   vuosi:_2009

    private AreaDataServlet servlet;

    @Override
    public void setUp(){
        super.setUp();
        servlet = new AreaDataServlet(repository, "http://localhost:8080/rdf/");
    }
    
    @Test
    public void Correct_Encoding() throws ServletException, IOException{
        request.setParameter("area", "_091_1_Eteläinen_suurpiiri");
        servlet.service(request, response);
        assertEquals("UTF-8", response.getCharacterEncoding());
        assertEquals("application/json", response.getContentType());
    }
}
