Simple test project for neo4j A* performance tests. The loaded OSM is lombardia.osm.bz2. Sadly the file dimension is too big for my github account, anyway you can download this file from this site: <a href="http://geodati.fmach.it/gfoss_geodata/osm/italia_osm.html">http://geodati.fmach.it/gfoss_geodata/osm/italia_osm.html</a>; you have to select the italian region <strong>Lombardia</strong> and download the file .osm.bz2
<br/> In order to execute tests you have to properly configure the configuration.properties file under src/main/resources
Above all you have to configure:
<ul>
<li>
the path where to create the neo4j DB by filling the property: pinf.neo4j.db.path 
</li>
<li>
the path where to find the OSM file you want to load by filling the property pinf.osm.files.directory
</li>
</ul>
In order to execute tests, you have to use the class it.angelo.routing.test.GraphCreatorTest; first of all you have to build the neo4j DB by using the method createGraph; then you can test A* by using the method aStarTest
By using the provided OSM file I did some tests and these are results:
<ul>
<li>
A* from node 1 to node 2: 1416 millis
</li>
<li>
A* from node 1 to node 300000: 3428 millis
</li>
<li>
A* from node 1 to node 525440: 4128 millis
</li>
</ul>

In the file "LaptopInformation.html" you can fidn my laptop information

What I'm wondering is if these low performances are normal or I can improve something or this is the best I can have
