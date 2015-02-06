## aluesarjat

### Usage

Run the following Java class `fi.aluesarjat.prototype.AluesarjatStart`

Access the app via http://localhost:8080

### Deploy

Run the following Maven commands
```
mvn -Pprod clean package
```

Upload to semantic.hri.fi


On semantic.hri.fi
```
mv aluesarjat.war /usr/share/jetty/webapps/ROOT.war
service jetty restart
```

### Data update

Replace the contents of `profiles/prod/data/datasets`

Move the px files to `semantic.hri.fi:/opt/aluesarjat/data`

Reset virtuoso on semantic.hri.fi 

Redeploy aluesarjat to semantic.hri.fi and restart Jetty

### Licence

Copyright 2013 Mysema Ltd

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
