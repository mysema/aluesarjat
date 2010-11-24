package com.mysema.stat.scovo;

import java.util.Collections;

import org.springframework.transaction.annotation.Isolation;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.rdfbean.model.LIT;
import com.mysema.rdfbean.model.RDFBeanTransaction;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.STMT;
import com.mysema.rdfbean.model.UID;
import com.mysema.stat.META;

public class NamespaceHandler {

    private static final int TX_TIMEOUT = -1;

    private static final int TX_ISOLATION = Isolation.DEFAULT.value();

    private final Repository repository;

    public NamespaceHandler(Repository repository) {
        this.repository = repository;
    }

    public void addNamespace(String ns, String prefix) {
        RDFConnection conn = repository.openConnection();
        RDFBeanTransaction tx = conn.beginTransaction(false, TX_TIMEOUT, TX_ISOLATION);
        CloseableIterator<STMT> iter = null;
        try {
            LIT prefixLiteral = new LIT(prefix);
            UID uid = new UID(ns);
            STMT nsStmt = new STMT(uid, META.nsPrefix, prefixLiteral, null);
            boolean found = false;

            // Prefix mapped already
            iter = conn.findStatements(null, META.nsPrefix, prefixLiteral, null, false);
            while(iter.hasNext()) {
                STMT stmt = iter.next();
                if (stmt.equals(nsStmt)) {
                    // Retain valid mapping
                    found = true;
                } else {
                    // Remove duplicate prefix-mapping
                    conn.update(Collections.singleton(stmt), null);
                }
            }
            iter.close();

            // URI mapped already
            iter = conn.findStatements(uid, META.nsPrefix, null, null, false);
            while(iter.hasNext()) {
                STMT stmt = iter.next();
                if (stmt.equals(nsStmt)) {
                    // Retain valid mapping
                    found = true;
                } else {
                    // Remove duplicate URI-mapping
                    conn.update(Collections.singleton(stmt), null);
                }
            }
            iter.close();

            if (!found) {
                // Add new mapping
                conn.update(null, Collections.singleton(nsStmt));
            }
            tx.commit();

        } catch(Exception e){
            tx.rollback();

        } finally {
            if (iter != null) {
                iter.close();
            }
            conn.close();
        }
    }

}
