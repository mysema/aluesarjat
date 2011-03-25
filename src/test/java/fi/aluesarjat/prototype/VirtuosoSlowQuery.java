package fi.aluesarjat.prototype;

import java.util.Map;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.rdfbean.model.DC;
import com.mysema.rdfbean.model.NODE;
import com.mysema.rdfbean.model.QueryLanguage;
import com.mysema.rdfbean.model.RDF;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.RDFS;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.SKOS;
import com.mysema.rdfbean.model.SPARQLQuery;
import com.mysema.rdfbean.virtuoso.VirtuosoRepository;
import com.mysema.stat.scovo.SCV;

public class VirtuosoSlowQuery {

    public static void main(String[] args){
        Repository repository = new VirtuosoRepository("localhost:1111", "dba", "dba");
        repository.initialize();
        RDFConnection conn = repository.openConnection();
        try{
            String queryString = "SELECT * WHERE {\n" +
              "?dimensionType ?_c2 ?_c3 ; ?_c4 ?dimensionTypeName .\n" +
              "GRAPH ?dimensionType {\n" +
              "  ?dimension ?_c7 ?dimensionType ; ?_c4 ?dimensionName .\n" +
              "  OPTIONAL {?dimension ?_c9 ?dimensionDescription }\n" +
              "  OPTIONAL {?dimension ?_c11 ?parent }\n" +
              "}\n" +
              "}\n"+
              "ORDER BY ?dimensionName";

            SPARQLQuery query = conn.createQuery(QueryLanguage.SPARQL, queryString);
            query.setBinding("_c2",  RDFS.subClassOf);
            query.setBinding("_c3",  SCV.Dimension);
            query.setBinding("_c4",  DC.title);
            query.setBinding("_c7",  RDF.type);
            query.setBinding("_c9",  DC.description);
            query.setBinding("_c11", SKOS.broader);

            long start = System.currentTimeMillis();
            CloseableIterator<Map<String,NODE>> results = query.getTuples();
            while (results.hasNext()){
                results.next();
            }
            System.err.println(System.currentTimeMillis()-start);

        }finally{
            conn.close();
            repository.close();
        }
    }

}
