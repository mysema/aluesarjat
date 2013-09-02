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
