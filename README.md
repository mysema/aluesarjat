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

