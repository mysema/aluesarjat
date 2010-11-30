package com.mysema.stat.scovo;

import java.sql.Connection;
import java.util.Collections;
import java.util.Map;

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

    private static final int TX_ISOLATION = Connection.TRANSACTION_READ_COMMITTED;

    private final Repository repository;

    public NamespaceHandler(Repository repository) {
        this.repository = repository;
    }

    public void addNamespaces(Map<String,String> namespaces) {
        RDFConnection conn = repository.openConnection();
        RDFBeanTransaction tx = conn.beginTransaction(false, TX_TIMEOUT, TX_ISOLATION);
        CloseableIterator<STMT> iter = null;
        try {
            for (Map.Entry<String,String> entry : namespaces.entrySet()){
                LIT prefixLiteral = new LIT(entry.getValue());
                UID uid = new UID(entry.getKey());
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

            }
            iter.close();
            tx.commit();

        } catch(Exception e){
            tx.rollback();
            throw new RuntimeException(e);
        } finally {
            conn.close();
        }
    }

}
