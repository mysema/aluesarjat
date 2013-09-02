package fi.aluesarjat.prototype;

import static org.apache.commons.lang.StringUtils.leftPad;

import java.io.IOException;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.rdfbean.model.NODE;
import com.mysema.rdfbean.model.QueryLanguage;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.RDFConnectionCallback;
import com.mysema.rdfbean.model.SPARQLQuery;
import com.mysema.rdfbean.model.STMT;
import com.mysema.rdfbean.virtuoso.VirtuosoRepository;
import com.mysema.stat.META;

import fi.aluesarjat.prototype.guice.ModuleUtils;

@Ignore // FIXME: Use sub-properties of scv:dimension
public class TabularResults {

    interface RowCallback {

        void handle(Map<String,NODE> row);
    }

    private static VirtuosoRepository repository;

    private static String prefixes;

    @BeforeClass
    public static void beforeClass() {
        String baseURI = ModuleUtils.DEFAULT_BASE_URI;
        repository = new VirtuosoRepository("localhost:1111", "dba", "dba", baseURI);
        repository.setSources(ModuleUtils.getSources(baseURI));
        repository.initialize();

        // get prefixes
        repository.execute(new RDFConnectionCallback<Void>() {
            @Override
            public Void doInConnection(RDFConnection connection) throws IOException {
                CloseableIterator<STMT> stmts = connection.findStatements(null, META.nsPrefix, null, null, false);
                StringBuilder p = new StringBuilder();
                try {
                    while (stmts.hasNext()) {
                        STMT stmt = stmts.next();
                        p.append("PREFIX " + stmt.getObject().getValue() + ": <" + stmt.getSubject().getId() + ">\n");
                    }
                    prefixes = p.toString();
                } finally {
                    stmts.close();
                }
                return null;
            }
        });
    }

    @AfterClass
    public static void afterClass() {
        repository.close();
    }

    @Test
    public void Table1() {
        String id = "Taulukko 1. Väkiluku ikäryhmittäin 1. tammikuuta ja ennuste";

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

        query(id, query.toString(), new RowCallback() {
            @Override
            public void handle(Map<String, NODE> row) {
                String ik = row.get("ik").asURI().getLocalName();
                String v = row.get("v").asURI().getLocalName();
                String val = row.get("val").getValue();
                System.out.println(leftPad(ik, 20) + leftPad(v, 10) + leftPad(val, 10));
            }
        });
    }


    @Test
    public void Table2() {
        String id = "Taulukko 2. Väkiluku äidinkielen mukaan 1. tammikuuta";

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

        query(id, query.toString(), new RowCallback() {
            @Override
            public void handle(Map<String, NODE> row) {
                String k = row.get("k").asURI().getLocalName();
                String v = row.get("v").asURI().getLocalName();
                String val = row.get("val").getValue();
                System.out.println(leftPad(k, 20) + leftPad(v, 10) + leftPad(val, 10));
            }
        });
    }

    @Test
    public void Table3() {
        String id = "Taulukko 3. Väestönmuutokset";

        StringBuilder query = new StringBuilder(prefixes);
        query.append("SELECT ?k ?v ?val \n");
        query.append("WHERE { \n");
        query.append(" ?i scv:dimension ?v . ?v rdf:type dimension:Vuosi . \n");
        query.append(" FILTER ( ?v = vuosi:_2000 || ?v = vuosi:_2008 || ?v = vuosi:_2009 || ?v = vuosi:_2010 ) \n");
        query.append(" ?i scv:dimension yksikkö:Henkilö , ikä:Väestö_yhteensä . \n");
        query.append(" ?i scv:dimension ?k . ?k rdf:type dimension:Muuttosuunta . \n");
        query.append(" ?i scv:dimension alue:_091_101_Vironniemen_peruspiiri . \n");
        query.append(" ?i rdf:value ?val . FILTER ( datatype(?val) = xsd:double ) \n");
        query.append("} \n");
        query.append("ORDER BY ?k ?v");

        query(id, query.toString(), new RowCallback() {
            @Override
            public void handle(Map<String, NODE> row) {
                String k = row.get("k").asURI().getLocalName();
                String v = row.get("v").asURI().getLocalName();
                String val = row.get("val").getValue();
                System.out.println(leftPad(k, 20) + leftPad(v, 10) + leftPad(val, 10));
            }
        });
    }

    @Test
    public void Table4() {
        String id = "Taulukko 4. Asuntokanta hallintaperusteen ja huoneistotyypin mukaan 31.12.2009 (viimeinen vuosi)";

        StringBuilder query = new StringBuilder(prefixes);
        query.append("SELECT ?ha ?hu sum(?val) \n");
        query.append("WHERE { \n");
        query.append(" ?i scv:dimension vuosi:_2008 , yksikkö:Asunto_ja_neliömetri . \n");
        query.append(" ?i scv:dimension ?ha2 . ?ha2 rdf:type dimension:Hallintaperuste ; skos:broader ?ha . \n");
        query.append(" ?i scv:dimension ?hu2 . ?hu2 rdf:type dimension:Huoneistotyyppi ; skos:broader ?hu . \n");
        query.append(" ?i scv:dimension alue:_091_101_Vironniemen_peruspiiri . \n");
        query.append(" ?i rdf:value ?val . FILTER ( datatype(?val) = xsd:double ) \n");
        query.append("} \n");
        query.append("GROUP BY ?ha ?hu\n");
        query.append("ORDER BY ?ha ?hu");

        query(id, query.toString(), new RowCallback() {
            @Override
            public void handle(Map<String, NODE> row) {
                String ha = row.get("ha").asURI().getLocalName();
                String hu = row.get("hu").asURI().getLocalName();
                String val = row.get("callret-2").getValue();
                System.out.println(leftPad(ha, 30) + leftPad(hu, 30) + leftPad(val, 10));
            }
        });

    }

    @Test
    public void Table5() {
        String id = "Taulukko 5. Rakennukset 31.12.2008 (viimeinen vuosi)";

        StringBuilder query = new StringBuilder(prefixes);
        query.append("SELECT ?kt ?yk ?val \n");
        query.append("WHERE { \n");
        query.append(" ?i scv:dimension vuosi:_2008 , valmistumisvuosi:Yhteensä . \n");
        query.append(" ?i scv:dimension ?kt . ?kt rdf:type dimension:Käyttötarkoitus_ja_kerrosluku . \n");
        query.append(" ?i scv:dimension ?yk . ?yk rdf:type dimension:Yksikkö . \n");
        query.append(" ?i scv:dimension alue:_091_101_Vironniemen_peruspiiri . \n");
        query.append(" ?i rdf:value ?val . FILTER ( datatype(?val) = xsd:double ) \n");
        query.append("} \n");
        query.append("ORDER BY ?kt ?yk");

        query(id, query.toString(), new RowCallback() {
            @Override
            public void handle(Map<String, NODE> row) {
                String kt = row.get("kt").asURI().getLocalName();
                String yk = row.get("yk").asURI().getLocalName();
                String val = row.get("val").getValue();
                System.out.println(leftPad(kt, 40) + leftPad(yk, 30) + leftPad(val, 10));
            }
        });
    }

    @Test
    public void Table6() {
        String id = "Taulukko 6. Asuntotuotanto (kolme viimeistä vuotta)";

        StringBuilder query = new StringBuilder(prefixes);
        query.append("SELECT ?k ?v sum(?val) \n");
        query.append("WHERE { \n");
        query.append(" GRAPH dataset:A01HKI_Astuot_hper_rahoitus_talotyyppi {\n");
        query.append("   ?i scv:dimension alue:_091_101_Vironniemen_peruspiiri . \n");
        query.append("   ?i scv:dimension yksikkö:Asuntojen_lukumäärä , hallintaperuste:Asunnot_yhteensä , rahoitusmuoto:Yhteensä , talotyyppi:Yhteensä . \n");
        query.append("   ?i rdf:value ?val . FILTER ( datatype(?val) = xsd:double ) \n");
        query.append("   ?i scv:dimension ?v . \n");
        query.append("   FILTER ( ?v = vuosi:_2007 || ?v = vuosi:_2008 || ?v = vuosi:_2009 ) \n");
        query.append("   ?i scv:dimension ?k2 \n");
        query.append(" } \n");
        query.append(" ?k2 rdf:type dimension:Huoneistotyyppi ; skos:broader ?k . \n");
        query.append(" ?v rdf:type dimension:Vuosi . \n");
        query.append("} \n");
        query.append("GROUP BY ?k ?v\n");
        query.append("ORDER BY ?k ?v");

        query(id, query.toString(), new RowCallback() {
            @Override
            public void handle(Map<String, NODE> row) {
                String k = row.get("k").asURI().getLocalName();
                String v = row.get("v").asURI().getLocalName();
                String val = row.get("callret-2").getValue();
                System.out.println(leftPad(k, 30) + leftPad(v, 10) + leftPad(val, 10));
            }
        });

    }

    @Test
    public void Table7_1() {
        String id = "Taulukko 7. Väestön keskitulo, euroa, vuonna 2008 (viimeinen vuosi)";

        StringBuilder query = new StringBuilder(prefixes);
        query.append("SELECT ?val \n");
        query.append("WHERE { \n");
        query.append(" ?i scv:dimension vuosi:_2008 , tuloluokka:Keskitulo , yksikkö:Henkilö_ja_euro . \n");
        query.append(" ?i scv:dimension alue:_091_101_Vironniemen_peruspiiri . \n");
        query.append(" ?i rdf:value ?val . FILTER ( datatype(?val) = xsd:double ) \n");
        query.append("} \n");

        query(id, query.toString(), new RowCallback() {
            @Override
            public void handle(Map<String, NODE> row) {
                String val = row.get("val").getValue();
                System.out.println(leftPad(val, 10));
            }
        });
    }

    @Test
    public void Table7_2() {
        String id = "Taulukko 7. Työpaikat toimialan mukaan (kolme viimeistä vuotta)";

        StringBuilder query = new StringBuilder(prefixes);
        query.append("SELECT ?t ?v sum(?val) \n");
        query.append("WHERE { \n");
        query.append(" ?i scv:dimension yksikkö:Henkilö . \n");
        query.append(" ?i scv:dimension ?v . ?v rdf:type dimension:Vuosi . \n");
        query.append(" FILTER ( ?v = vuosi:_2005 || ?v = vuosi:_2006 || ?v = vuosi:_2007 ) \n");
        query.append(" ?i scv:dimension ?t2 . ?t2 rdf:type dimension:Toimiala ; skos:broader ?t . \n");
        query.append(" ?i scv:dimension alue:_091_101_Vironniemen_peruspiiri . \n");
        query.append(" ?i rdf:value ?val . FILTER ( datatype(?val) = xsd:double ) \n");
        query.append("} \n");
        query.append("GROUP BY ?t ?v\n");
        query.append("ORDER BY ?t ?v");

        query(id, query.toString(), new RowCallback() {
            @Override
            public void handle(Map<String, NODE> row) {
                String t = row.get("t").asURI().getLocalName();
                String v = row.get("v").asURI().getLocalName();
                String val = row.get("callret-2").getValue();
                System.out.println(leftPad(t, 30) + leftPad(v, 10) + leftPad(val, 10));
            }
        });
    }

    private void query(String id, String queryString, RowCallback callback) {
        System.err.println(id);
        System.out.println();

        System.out.println(queryString);
        RDFConnection connection = repository.openConnection();
        try {
            SPARQLQuery query = connection.createQuery(QueryLanguage.SPARQL, queryString);
            CloseableIterator<Map<String, NODE>> tuplesResult = query.getTuples();
            try {
                while (tuplesResult.hasNext()) {
                    callback.handle(tuplesResult.next());
                }
                System.out.println();
            } finally {
                tuplesResult.close();
            }
        } finally {
            connection.close();
        }

    }
}
