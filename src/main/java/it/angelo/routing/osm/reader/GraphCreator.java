package it.angelo.routing.osm.reader;

import it.angelo.routing.osm.reader.sink.GraphCreatorSink;
import it.angelo.routing.osm.reader.sink.PrepareBatchNodeIdsSinkImpl;
import it.angelo.routing.osm.reader.util.AbstractChecker;
import it.angelo.routing.osm.reader.util.CarWayChecker;
import it.angelo.routing.osm.reader.util.PropertiesLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;

import com.carrotsearch.hppc.LongLongOpenHashMap;

public class GraphCreator
{
	private static final Log logger = LogFactory.getLog(GraphCreator.class.getName());
	PropertiesLoader propsLoad;

	public GraphCreator()
	{
		propsLoad = PropertiesLoader.getInstance();
	}

	public void createGraph()
	{
		if (propsLoad.getOsmFilesDirectory() == null || propsLoad.getOsmFilesDirectory().trim().equals(""))
		{

			throw new IllegalArgumentException("Impossibile importare dati OSM; none directory vuoto o nullo <" + propsLoad.getOsmFilesDirectory() + ">");
		}
		File graphDb = new File(propsLoad.getNeo4jDbPath());
		if (graphDb.exists() && graphDb.listFiles() != null && graphDb.listFiles().length > 0)
		{

			if (logger.isWarnEnabled())
			{

				logger.warn("Nuovo processo di import iniziato; cancellazione vecchio grafo");
			}
			try
			{
				FileUtils.cleanDirectory(graphDb);
			}
			catch (IOException e)
			{

				logger.fatal("Errore nella cancellazione del vecchio grafo", e);
			}
		}
		File osm = new File(propsLoad.getOsmFilesDirectory());
		// Se è una directory leggo tutti i file OSM
		if (osm.isDirectory())
		{
			File[] osmFiles = (new File(propsLoad.getOsmFilesDirectory())).listFiles();
			for (int i = 0; i < osmFiles.length; i++)
			{
				File file = osmFiles[i];
				try
				{
					process(file);
				}
				catch (Exception e)
				{

					String message = "Errore nella lettura del file OSM " + file.getAbsolutePath() + ". Messaggio errore: " + e.getMessage();
					logger.fatal(message, e);
					throw new IllegalStateException(message);
				}
			}
		}
		else
		{

			try
			{
				process(osm);
			}
			catch (Exception e)
			{

				String message = "Errore nella lettura del file OSM " + osm.getAbsolutePath() + ". Messaggio errore: " + e.getMessage();
				logger.fatal(message, e);
				throw new IllegalStateException(message);
			}
		}
	}

	private void process(File file) throws Exception
	{
		if (logger.isInfoEnabled())
		{

			logger.info("Inizio prima fase creazione grafo; recupero gli ID dei nodi da creare; file da parserizzare: " + file.getName());
		}
		AbstractChecker checker = new CarWayChecker();
		Sink prepareNodeIdsSink = new PrepareBatchNodeIdsSinkImpl(checker);
		readOsmFile(file, prepareNodeIdsSink);
		PrepareBatchNodeIdsSinkImpl pbn = ((PrepareBatchNodeIdsSinkImpl) prepareNodeIdsSink);
		LongLongOpenHashMap nodesMap = pbn.getNodesMap();

		Sink createGraphSink = new GraphCreatorSink(file.getAbsolutePath(), propsLoad.getNeo4jDbPath(), nodesMap, checker);
		readOsmFile(file, createGraphSink);
	}

	private void readOsmFile(File file, Sink currentReader) throws FileNotFoundException
	{
		boolean pbf = false;
		CompressionMethod compression = CompressionMethod.None;
		if (file.getName().endsWith(".pbf"))
		{
			pbf = true;
		}
		else if (file.getName().endsWith(".gz"))
		{
			compression = CompressionMethod.GZip;
		}
		else if (file.getName().endsWith(".bz2"))
		{
			compression = CompressionMethod.BZip2;
		}
		RunnableSource reader;
		if (pbf)
		{
			reader = new crosby.binary.osmosis.OsmosisReader(new FileInputStream(file));
		}
		else
		{
			reader = new XmlReader(file, false, compression);
		}
		reader.setSink(currentReader);
		Thread readerThread = new Thread(reader);
		readerThread.start();
		while (readerThread.isAlive())
		{
			try
			{
				readerThread.join();
			}
			catch (InterruptedException e)
			{
				/* do nothing */
			}
		}
	}
}