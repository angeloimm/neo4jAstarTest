package it.angelo.routing.test;

import it.angelo.routing.graph.manager.NeoGraphMgr;
import it.angelo.routing.osm.reader.GraphCreator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

public class GraphCreatorTest
{
	private static final Log logger = LogFactory.getLog(GraphCreatorTest.class.getName());

	public void createGraph()
	{
		try
		{

			GraphCreator gc = new GraphCreator();
			gc.createGraph();
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
		}
	}

	@Test
	public void aStarTest()
	{
		try
		{

			NeoGraphMgr mgr = NeoGraphMgr.getInstance();
			long start = 1;
			long end = 525440;
			long startTime = System.currentTimeMillis();
			mgr.testAStar(start, end);
			long finalTime = System.currentTimeMillis();
			long totalTime = finalTime-startTime;
			logger.info("From point "+start+" to point "+end+" taken "+totalTime+" millis");
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
		}
	}
}
