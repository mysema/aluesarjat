package fi.aluesarjat.prototype;

import java.util.Map;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.rdfbean.model.NODE;
import com.mysema.rdfbean.model.QueryLanguage;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.SPARQLQuery;
import com.mysema.rdfbean.model.UID;
import com.mysema.rdfbean.virtuoso.VirtuosoRepository;
import com.mysema.stat.scovo.SCV;

public class VirtuosoSlowQuery2 {

    public static void main(String[] args){
        Repository repository = new VirtuosoRepository("localhost:1111", "dba", "dba");
        repository.initialize();
        RDFConnection conn = repository.openConnection();
        try{
            String queryString = "SELECT DISTINCT ?dataset WHERE { GRAPH ?dataset { ?item ?_c6 ?dataset ; ?_c3 ?_c4 , ?_c5 . } }";

            SPARQLQuery query = conn.createQuery(QueryLanguage.SPARQL, queryString);
            query.setBinding("_c3",  SCV.dimension);
            query.setBinding("_c4",  new UID("http://localhost:8080/rdf/dimensions/Rahoitusmuoto#Yhteensä"));
            query.setBinding("_c5",  new UID("http://localhost:8080/rdf/dimensions/Hallintaperuste#Omistusasunnot"));
            query.setBinding("_c6",  SCV.dataset);

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
