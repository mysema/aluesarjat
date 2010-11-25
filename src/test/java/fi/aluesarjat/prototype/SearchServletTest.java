package fi.aluesarjat.prototype;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import javax.servlet.ServletException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.junit.Test;

public class SearchServletTest extends AbstractFacetSearchServletTest{

    private SearchServlet servlet;

    @Override
    public void setUp(){
        super.setUp();
        servlet = new SearchServlet(repository);
    }

    @Test
    public void By_Dataset_FacetsOnly() throws ServletException, IOException{
        request.addParameter("value", "dataset:A01HKIS_Vaestotulot");
        request.addParameter("include", "facets");
        servlet.service(request, response);

        JSONObject obj = JSONObject.fromObject(response.getContentAsString());
        assertNotNull(obj.get("facets"));
        validateFacets((JSONArray) obj.get("facets"));
        assertNull(obj.get("items"));
    }

    @Test
    public void By_Region_Dataset_and_Year_FacetsOnly() throws ServletException, IOException{
        request.addParameter("value", "alue:_091_603_Laajasalon_peruspiiri");
        request.addParameter("value", "dataset:A01HKIS_Vaestotulot");
        request.addParameter("value", "vuosi:_2001");
        request.addParameter("include", "facets");
        servlet.service(request, response);

        JSONObject obj = JSONObject.fromObject(response.getContentAsString());
        assertNotNull(obj.get("facets"));
        validateFacets((JSONArray) obj.get("facets"));
        assertNull(obj.get("items"));
    }


    @Test
    public void By_Region_Dataset_and_Year() throws ServletException, IOException{
        request.addParameter("value", "alue:_091_603_Laajasalon_peruspiiri");
        request.addParameter("value", "dataset:A01HKIS_Vaestotulot");
        request.addParameter("value", "vuosi:_2001");
        request.addParameter("include", "items");
        request.addParameter("include", "facets");
        servlet.service(request, response);

        JSONObject obj = JSONObject.fromObject(response.getContentAsString());
        assertNotNull(obj.get("facets"));
        validateFacets((JSONArray) obj.get("facets"));
        assertNotNull(obj.get("items"));
        validateItems((JSONArray) obj.get("items"));
    }

    private void validateItems(JSONArray jsonArray) {
        for (Object f : jsonArray.toArray()){
            JSONObject facet = (JSONObject)f;
            assertNotNull(facet.get("value"));
            assertNotNull(facet.get("values"));
        }
    }

    private void validateFacets(JSONArray jsonArray){
        for (Object f : jsonArray.toArray()){
            JSONObject facet = (JSONObject)f;
            assertNotNull(facet.get("id"));
        }
    }
}
