package fi.aluesarjat.prototype;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import com.mysema.commons.lang.IteratorAdapter;
import com.mysema.rdfbean.model.DC;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.RDFConnectionCallback;
import com.mysema.rdfbean.model.STMT;
import com.mysema.rdfbean.sesame.MemoryRepository;
import com.mysema.rdfbean.sesame.SesameRepository;
import com.mysema.stat.pcaxis.PCAxisParser;
import com.mysema.stat.scovo.NamespaceHandler;
import com.mysema.stat.scovo.RDFDatasetHandler;

import fi.aluesarjat.prototype.guice.ModuleUtils;

public class DumpData {

    public static void main(String[] args) throws IOException{
        String baseURI = ModuleUtils.DEFAULT_BASE_URI;
        SesameRepository repository = new MemoryRepository();

        try{
            repository.setSources(ModuleUtils.getSources(baseURI));
            repository.initialize();

            NamespaceHandler namespaceHandler = new NamespaceHandler(repository);
            RDFDatasetHandler handler = new RDFDatasetHandler(repository, namespaceHandler, baseURI);
            PCAxisParser parser = new PCAxisParser(handler);
            parser.parse("A01HKIS_Vaestotulot",   getResourceAsStream("/data/A01HKIS_Vaestotulot.px"));
            parser.parse("B02S_ESP_Vakiluku1975", getResourceAsStream("/data/B02S_ESP_Vakiluku1975.px"));
            parser.parse("C02S_VAN_Vakiluku1971", getResourceAsStream("/data/C02S_VAN_Vakiluku1971.px"));

            repository.execute(new RDFConnectionCallback<Void>(){
                @Override
                public Void doInConnection(RDFConnection connection) throws IOException {
                    Set<STMT> stmts = new HashSet<STMT>(IteratorAdapter.asList(connection.findStatements(null, DC.title, null, null, false)));
                    RDFUtil.dump(stmts, new File("src/test/resources/area-titles.ttl"));
                    return null;
                }
            });

        }finally{
            repository.close();
        }
    }

    private static InputStream getResourceAsStream(String res){
        return DumpData.class.getResourceAsStream(res);
    }

}