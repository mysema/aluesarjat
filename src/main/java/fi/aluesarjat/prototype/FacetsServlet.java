package fi.aluesarjat.prototype;

import static fi.aluesarjat.prototype.Constants.dimension;
import static fi.aluesarjat.prototype.Constants.dimensionDescription;
import static fi.aluesarjat.prototype.Constants.dimensionName;
import static fi.aluesarjat.prototype.Constants.dimensionType;
import static fi.aluesarjat.prototype.Constants.dimensionTypeName;
import static fi.aluesarjat.prototype.Constants.parent;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import com.mysema.rdfbean.model.Blocks;
import com.mysema.rdfbean.model.DC;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.RDFQuery;
import com.mysema.rdfbean.model.RDFQueryImpl;
import com.mysema.rdfbean.model.RDFS;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.SKOS;
import com.mysema.rdfbean.model.UID;
import com.mysema.stat.scovo.SCV;


public class FacetsServlet extends AbstractFacetSearchServlet {

    private static final long serialVersionUID = 2149808648205848159L;

    private final Repository repository;

    public FacetsServlet(Repository repository) {
        this.repository = repository;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse)res;
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        RDFConnection conn = repository.openConnection();
        try {
            Map<UID,JSONObject> dimensionTypes = new LinkedHashMap<UID,JSONObject>();

            // NAMESPACES
            Map<String,String> namespaces = getNamespaces(conn);

            // DIMENSIONS
            RDFQuery query = new RDFQueryImpl(conn);
            query.where(
                  dimensionType.has(RDFS.subClassOf, SCV.Dimension),
                  dimensionType.has(DC.title, dimensionTypeName),
                  dimension.a(dimensionType),
                  dimension.has(DC.title, dimensionName),
                  Blocks.optional(dimension.has(DC.description, dimensionDescription)),
                  Blocks.optional(dimension.has(new UID(SKOS.NS, "broader"), parent)));
            query.orderBy(dimensionName.asc());

            addFacets(conn, namespaces, dimensionTypes,query.selectAll());

            // DATASETS
            query = new RDFQueryImpl(conn);
            // Query datasets as a kind of dimension
            query.where(
                  dimension.a(dimensionType), // dimensionType = scv:Dataset
                  dimension.has(DC.title, dimensionName), // datasetName
                  Blocks.optional(dimension.has(DC.description, dimensionDescription)));
            query.set(dimensionType, SCV.Dataset);
            query.orderBy(dimensionName.asc());

            addFacets(conn, namespaces, dimensionTypes, query.selectAll());

            JSONObject result = new JSONObject();
            result.put("facets", dimensionTypes.values());
            Writer out = response.getWriter();
            String jsonpCallback = request.getParameter("callback");
            if (jsonpCallback != null){
                out.write(jsonpCallback + "(");
                result.write(out);
                out.write(")");
            }else{
                result.write(out);
            }
            out.flush();
        } finally {
            conn.close();
        }
    }

}
