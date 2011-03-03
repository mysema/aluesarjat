package fi.aluesarjat.prototype;

import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.rdfbean.model.LIT;
import com.mysema.rdfbean.model.NODE;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.UID;
import com.mysema.stat.scovo.SCV;

public abstract class AbstractFacetSearchServlet extends AbstractSPARQLServlet {

    private static final long serialVersionUID = 1L;

    protected void addFacets(RDFConnection conn, Map<String,String> namespaces, Map<UID,JSONObject> dimensionTypes, CloseableIterator<Map<String,NODE>> results) {
        try {
            Map<String, NODE> row;
            while (results.hasNext()) {
                row = results.next();
                addFacet(row, namespaces, dimensionTypes);
            }
        } finally {
            results.close();
        }
    }

    protected void addFacet(Map<String, NODE> row, Map<String,String> namespaces, Map<UID,JSONObject> facets) {
        UID type = (UID) row.get("dimensionType");
        JSONObject dimensionType = facets.get(type);
        if (dimensionType == null) {
            // Create dimension type
            dimensionType = new JSONObject();
            dimensionType.put("id", getPrefixed(type, namespaces));
            if (type.equals(SCV.Dataset)) {
                dimensionType.put("name", "Tilasto");
            } else {
                LIT typeName = (LIT) row.get("dimensionTypeName");
                if (typeName != null) {
                    dimensionType.put("name", typeName.getValue());
                }
            }
            facets.put(type, dimensionType);
        }
        // Create dimension
        JSONObject dimension = new JSONObject();
        dimension.put("id", getPrefixed((UID) row.get("dimension"), namespaces));

        // Literal metadata
        LIT lit = (LIT) row.get("dimensionName");
        if (lit != null) {
            dimension.put("name", lit.getValue());
        }

        lit = (LIT) row.get("dimensionDescription");
        if (lit != null) {
            dimension.put("description", lit.getValue());
        }

        UID uid = (UID) row.get("parent");
        if (uid != null) {
            JSONObject parent = new JSONObject();
            parent.put("id", getPrefixed(uid, namespaces));
            dimension.put("parent", parent);
        }

        if (!dimensionType.containsKey("values")) {
            dimensionType.put("values", new JSONArray());
        }
        dimensionType.accumulate("values", dimension);
    }



}
