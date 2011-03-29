package fi.aluesarjat.prototype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.servlet.ServletException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.junit.Test;

public class SearchServletTest extends AbstractServletTest{

    private SearchServlet servlet;

    @Override
    public void setUp(){
        super.setUp();
        servlet = new SearchServlet(new SearchServiceImpl(repository));
    }

    @Test
    public void JSONP_Is_Supported() throws ServletException, IOException{
        request.setParameter("callback", "handleResponse");
        request.addParameter("value", "dataset:A01HKIS_Vaestotulot");
        request.addParameter("include", "values");
        servlet.service(request, response);

        String res = response.getContentAsString();
        assertTrue(res.startsWith("handleResponse("));
        assertTrue(res.endsWith(")"));
    }

    @Test
    public void By_Dataset_FacetsOnly() throws ServletException, IOException{
        request.addParameter("value", "dataset:A01HKIS_Vaestotulot");
        request.addParameter("include", "values");
        servlet.service(request, response);

        JSONObject obj = JSONObject.fromObject(response.getContentAsString());
        assertNotNull(obj.get("availableValues"));
        validateFacets((JSONObject) obj.get("availableValues"));
        assertNull(obj.get("items"));
    }


    @Test
    public void By_Region_Dataset_and_Year_FacetsOnly() throws ServletException, IOException{
        request.addParameter("value", "alue:_091_603_Laajasalon_peruspiiri");
        request.addParameter("value", "dataset:A01HKIS_Vaestotulot");
        request.addParameter("value", "vuosi:_2001");
        request.addParameter("include", "values");
        servlet.service(request, response);

        JSONObject obj = JSONObject.fromObject(response.getContentAsString());
        assertNotNull(obj.get("availableValues"));
        validateFacets((JSONObject) obj.get("availableValues"));
        assertNull(obj.get("items"));
    }


    @Test
    public void By_Region_Dataset_and_Year() throws ServletException, IOException{
        request.addParameter("value", "vuosi:_2001");
        request.addParameter("value", "alue:_091_603_Laajasalon_peruspiiri");
        request.addParameter("value", "dataset:A01HKIS_Vaestotulot");
        request.addParameter("include", "items");
        request.addParameter("include", "values");
        servlet.service(request, response);

        System.err.println(response.getContentAsString());
        JSONObject obj = JSONObject.fromObject(response.getContentAsString());
        assertNotNull(obj.get("availableValues"));
        validateFacets((JSONObject) obj.get("availableValues"));
        assertNotNull(obj.get("items"));
        validateItems((JSONArray) obj.get("items"));
    }

    @Test
    public void By_Region_Dataset_and_Year_with_Limit() throws ServletException, IOException{
        request.addParameter("value", "vuosi:_2001");
        request.addParameter("value", "tuloluokka:_15-vuotta_täyttäneet_yhteensä");
        request.addParameter("value", "dataset:A01HKIS_Vaestotulot");
        request.addParameter("include", "items");
        request.addParameter("include", "values");
        request.addParameter("limit", "10");
        servlet.service(request, response);

        JSONObject obj = JSONObject.fromObject(response.getContentAsString());
        assertNotNull(obj.get("items"));
        JSONArray items = (JSONArray)obj.get("items");
        assertEquals(10, items.size());
    }

    @Test
    public void By_Region_Dataset_and_Year_with_Limit_and_Offset() throws ServletException, IOException{
        request.addParameter("value", "vuosi:_2001");
        request.addParameter("value", "tuloluokka:_15-vuotta_täyttäneet_yhteensä");
        request.addParameter("value", "dataset:A01HKIS_Vaestotulot");
        request.addParameter("include", "items");
        request.addParameter("include", "values");
        request.addParameter("limit", "10");
        request.addParameter("offset", "10");
        servlet.service(request, response);

        JSONObject obj = JSONObject.fromObject(response.getContentAsString());
        assertNotNull(obj.get("items"));
        JSONArray items = (JSONArray)obj.get("items");
        assertEquals(10, items.size());
    }

    private void validateItems(JSONArray jsonArray) {
        for (Object obj : jsonArray.toArray()){
            JSONObject o = (JSONObject)obj;
            assertNotNull(o.get("value"));
            assertNotNull(o.get("values"));
        }
    }

    private void validateFacets(JSONObject jsonObject){
        for(Object key : jsonObject.keySet()){
            assertEquals(true, jsonObject.get(key));
        }
    }
}
