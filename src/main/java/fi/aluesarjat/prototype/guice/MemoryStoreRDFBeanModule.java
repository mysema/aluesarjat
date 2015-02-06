/*
* Copyright 2013 Mysema Ltd
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package fi.aluesarjat.prototype.guice;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.mysema.rdfbean.guice.Config;
import com.mysema.rdfbean.guice.RDFBeanRepositoryModule;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.sesame.MemoryRepository;

public class MemoryStoreRDFBeanModule extends RDFBeanRepositoryModule {

    @Override
    public List<String> getConfiguration() {
        return Arrays.asList("/aluesarjat.properties", "/import-parallel.properties");
    }

    @Override
    public Repository createRepository(@Config Properties properties) {
        final MemoryRepository repository = new MemoryRepository();
        repository.setSources(ModuleUtils.getSources(properties.getProperty("baseURI")));
        repository.initialize();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                repository.close();
            }
        });
        return repository;
    }

}
