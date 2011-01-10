package fi.aluesarjat.prototype;

import static org.apache.commons.lang.StringUtils.leftPad;

import java.io.IOException;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.rdfbean.model.NODE;
import com.mysema.rdfbean.model.Operation;
import com.mysema.rdfbean.model.QueryLanguage;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.SPARQLQuery;
import com.mysema.rdfbean.model.STMT;
import com.mysema.rdfbean.virtuoso.VirtuosoRepository;
import com.mysema.stat.META;

import fi.aluesarjat.prototype.guice.ModuleUtils;

public class TabularResults {
    
    interface RowCallback {
        
        void handle(Map<String,NODE> row);
    }
    
    private static VirtuosoRepository repository;
    
    private static String prefixes;
    
    @BeforeClass
    public static void beforeClass(){
        String baseURI = ModuleUtils.DEFAULT_BASE_URI;
        repository = new VirtuosoRepository("localhost:1111", "dba", "dba", baseURI);
        repository.setSources(ModuleUtils.getSources(baseURI));
        repository.initialize();
        
        // get prefixes
        repository.execute(new Operation<Void>(){
            @Override
            public Void execute(RDFConnection connection) throws IOException {
                CloseableIterator<STMT> stmts = connection.findStatements(null, META.nsPrefix, null, null, false);
                StringBuilder p = new StringBuilder();
                try{
                    while (stmts.hasNext()){
                        STMT stmt = stmts.next();
                        p.append("PREFIX " + stmt.getObject().getValue() + ": <" + stmt.getSubject().getId() + ">\n");
                    }       
                    prefixes = p.toString();
                }finally{
                    stmts.close();
                }
                return null;
            }            
        });
    }
    
    @AfterClass
    public static void afterClass(){
        repository.close();
    }

    @Test
    public void Table1(){
        // Taulukko 1. Väkiluku ikäryhmittäin 1. tammikuuta ja ennuste
        StringBuilder query = new StringBuilder(prefixes);
        query.append("SELECT ?ik ?v ?val \n");
        query.append("WHERE { \n");
        query.append(" ?i scv:dimension ?v . ?v rdf:type dimension:Vuosi . \n");
        query.append(" FILTER ( ?v = vuosi:_2000 || ?v = vuosi:_2008 || ?v = vuosi:_2009 || ?v = vuosi:_2010 ) \n");
        query.append(" ?i scv:dimension yksikkö:Henkilöä . \n");
        query.append(" ?i scv:dimension ?ik . ?ik rdf:type dimension:Ikäryhmä . \n");
        query.append(" ?i scv:dimension alue:_091_101_Vironniemen_peruspiiri . \n");
        query.append(" ?i rdf:value ?val . FILTER ( datatype(?val) = xsd:double ) \n");
        query.append("} \n");
        query.append("ORDER BY ?ik ?v \n");

        query(query.toString(), new RowCallback(){
            @Override
            public void handle(Map<String, NODE> row) {
                String ik = row.get("ik").asURI().getLocalName();
                String v = row.get("v").asURI().getLocalName();
                String val = row.get("val").getValue();
                System.out.println(leftPad(ik, 15) + leftPad(v, 15) + leftPad(val, 15));
            }            
        });
    }
    

    @Test
    public void Table2(){
        // Taulukko 2. Väkiluku äidinkielen mukaan 1. tammikuuta
        StringBuilder query = new StringBuilder(prefixes);
        query.append("SELECT ?k ?v ?val \n");
        query.append("WHERE { \n");
        query.append(" ?i scv:dimension ?v . ?v rdf:type dimension:Vuosi . \n");
        query.append(" FILTER ( ?v = vuosi:_2000 || ?v = vuosi:_2008 || ?v = vuosi:_2009 || ?v = vuosi:_2010 ) \n");
        query.append(" ?i scv:dimension yksikkö:Henkilö . \n");
        query.append(" ?i scv:dimension ?k . ?k rdf:type dimension:Äidinkieli . \n");
        query.append(" ?i scv:dimension alue:_091_101_Vironniemen_peruspiiri . \n");
        query.append(" ?i rdf:value ?val . FILTER ( datatype(?val) = xsd:double ) \n");
        query.append("} \n");
        query.append("ORDER BY ?k ?v");
        
        query(query.toString(), new RowCallback(){
            @Override
            public void handle(Map<String, NODE> row) {
                String k = row.get("k").asURI().getLocalName();
                String v = row.get("v").asURI().getLocalName();
                String val = row.get("val").getValue();
                System.out.println(leftPad(k, 15) + leftPad(v, 15) + leftPad(val, 15));                
            }            
        });
    }
    
    @Test
    public void Table3(){
        // Taulukko 3. Väestönmuutokset

    }
    
    @Test
    public void Table4(){
        // Taulukko 4. Asuntokanta hallintaperusteen ja huoneistotyypin mukaan 31.12.2009 (viimeinen vuosi)

    }
    
    @Test
    public void Table5(){
        // Taulukko 5. Rakennukset 31.12.2008 (viimeinen vuosi)

    }
    
    @Test
    public void Table6(){
        // Taulukko 6. Asuntotuotanto (kolme viimeistä vuotta)
   
    }
    
    @Test
    public void Table7_1(){
//        Taulukko 7. Väestön keskitulo, euroa, vuonna 2008 (viimeinen vuosi)

    }
    
    @Test
    public void Table7_2(){
        // Taulukko 7. Työpaikat toimialan mukaan (kolme viimeistä vuotta)

    }
    
    private void query(String queryString, RowCallback callback) {
//        System.out.println(queryString);
        RDFConnection connection = repository.openConnection();
        try{
            SPARQLQuery query = connection.createQuery(QueryLanguage.SPARQL, queryString);
            CloseableIterator<Map<String, NODE>> tuplesResult = query.getTuples();
            try{
                while (tuplesResult.hasNext()){
                    callback.handle(tuplesResult.next());
                }
                System.out.println();
            }finally{
                tuplesResult.close();
            }
        }finally{
            connection.close();
        }
        
    }
}
