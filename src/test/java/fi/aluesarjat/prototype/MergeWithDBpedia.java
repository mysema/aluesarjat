package fi.aluesarjat.prototype;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.rdfbean.model.DC;
import com.mysema.rdfbean.model.Format;
import com.mysema.rdfbean.model.LIT;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.STMT;
import com.mysema.rdfbean.model.UID;
import com.mysema.rdfbean.model.io.RDFSource;
import com.mysema.rdfbean.model.io.TurtleStringWriter;
import com.mysema.rdfbean.owl.OWL;
import com.mysema.rdfbean.sesame.MemoryRepository;

public class MergeWithDBpedia {

    public static void main(String[] args) throws IOException {
        MemoryRepository repository = new MemoryRepository();
        File abstracts = new File("short_abstracts_fi.nt");
        repository.setSources(
                new RDFSource(new FileInputStream(abstracts), Format.NTRIPLES, "test:test1"),
                new RDFSource("classpath:/area-titles.ttl", Format.TURTLE, "test:test2"));
        repository.initialize();

        Locale fi = new Locale("fi");
        Set<STMT> links = new HashSet<STMT>();
        Set<STMT> comments = new HashSet<STMT>();
        try {
            RDFConnection conn = repository.openConnection();
            try {
                CloseableIterator<STMT> titleStmts = conn.findStatements(null, DC.title, null, null, false);
                try {
                    while (titleStmts.hasNext()) {
                        STMT titleStmt = titleStmts.next();
                        String title = titleStmt.getObject().getValue().replace(' ', '_');
                        UID dbpediaSubject = new UID("http://dbpedia.org/resource/" + URLEncoder.encode(title,"UTF-8"));
                        CloseableIterator<STMT> dbpediaStmts = conn.findStatements(dbpediaSubject, null, null, null, false);
                        try {
                            if (dbpediaStmts.hasNext()) {
                                boolean linkAdded = false;
                                while (dbpediaStmts.hasNext()) {
                                    STMT dbpediaStmt = dbpediaStmts.next();

                                    if (!linkAdded) {
                                        links.add(new STMT(titleStmt.getSubject(), OWL.sameAs, dbpediaStmt.getSubject()));
                                        System.out.println(titleStmt.getSubject() + " owl:sameAs " + dbpediaStmt.getSubject());
                                    }
                                    String comment = dbpediaStmt.getObject().getValue();
                                    try {
                                        comment = URLDecoder.decode(comment,"UTF-8");
                                    } catch (IllegalArgumentException e) {
                                        System.err.println(e.getMessage());
                                    }
                                    comments.add(new STMT(dbpediaSubject, dbpediaStmt.getPredicate(), new LIT(comment, fi)));
                                    comments.add(dbpediaStmt);

                                }
                            } else {
                                System.err.println("Got no match for " + title);
                            }

                        } finally {
                            dbpediaStmts.close();
                        }
                    }
                } finally {
                    titleStmts.close();
                }

            } finally {
                conn.close();
            }

        } finally {
            repository.close();
        }

        // dump links
        dump(links, new File("src/main/resources/ext/dbpedia-links.ttl"));

        // dump comments
        dump(comments, new File("src/main/resources/ext/dbpedia-comments.ttl"));
    }

    private static void dump(Set<STMT> stmts, File file) throws IOException {
        TurtleStringWriter writer = new TurtleStringWriter();
        writer.begin();
        for (STMT stmt : stmts) {
            writer.handle(stmt);
        }
        writer.end();

        file.createNewFile();
        FileUtils.writeStringToFile(file, writer.toString(), "UTF-8");
    }

}
