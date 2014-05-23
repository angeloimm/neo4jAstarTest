package it.angelo.routing.graph.manager;

import it.angelo.routing.graph.relation.types.RelTypes;
import it.angelo.routing.osm.reader.util.OSMAttribute;
import it.angelo.routing.osm.reader.util.PropertiesLoader;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.graphalgo.CommonEvaluators;
import org.neo4j.graphalgo.EstimateEvaluator;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.tooling.GlobalGraphOperations;

public class NeoGraphMgr
{

	private GraphDatabaseService graphDbService;
	private GlobalGraphOperations ggo;
	private static final Log logger = LogFactory.getLog(NeoGraphMgr.class.getName());
	private static NeoGraphMgr theInstance;
	
	private NeoGraphMgr()
	{
		initialize();
	}
	public static NeoGraphMgr getInstance()
	{
		if( theInstance == null )
		{
			theInstance = new NeoGraphMgr();
		}
		return theInstance;
	}
	private void initialize()
	{
		PropertiesLoader pl = PropertiesLoader.getInstance();
		GraphDatabaseFactory gdbf = new GraphDatabaseFactory();
		GraphDatabaseBuilder gdbb = gdbf.newEmbeddedDatabaseBuilder(pl.getNeo4jDbPath());
		gdbb.setConfig(GraphDatabaseSettings.nodestore_mapped_memory_size, pl.getNodestoreMappedMemorySize());
		gdbb.setConfig(GraphDatabaseSettings.relationshipstore_mapped_memory_size, pl.getRelationshipstoreMappedMemorySize());
		gdbb.setConfig(GraphDatabaseSettings.nodestore_propertystore_mapped_memory_size, pl.getNodestorePropertystoreMappedMemorySize());
		gdbb.setConfig(GraphDatabaseSettings.strings_mapped_memory_size, pl.getStringsMappedMemorySize());
		gdbb.setConfig(GraphDatabaseSettings.arrays_mapped_memory_size, pl.getArraysMappedMemorySize());
		gdbb.setConfig(GraphDatabaseSettings.cache_type,pl.getCacheType());
		gdbb.setConfig(GraphDatabaseSettings.use_memory_mapped_buffers, "true");
		graphDbService = gdbb.newGraphDatabase();
		ggo = GlobalGraphOperations.at(graphDbService);
		registerShutdownHook(graphDbService);
		logger.info("Loading in memory the full graph");
		Transaction tx = this.graphDbService.beginTx();
		try
		{
			loadWholeGraphInMemory();
		}
		catch (Exception e)
		{

			tx.failure();
			logger.fatal(e.getMessage(), e);
			throw new IllegalStateException(e);
		}
		finally
		{
			if (tx != null)
			{

				tx.close();
			}
		}
	}

	private static void registerShutdownHook(final GraphDatabaseService graphDb)
	{

		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				graphDb.shutdown();
			}
		});
	}

	@SuppressWarnings({ "rawtypes", "unused" })
	public void testAStar(long idNodeStart, long idNodeEnd)
	{

		Transaction tx = this.graphDbService.beginTx();
		try
		{
			Node startNode = graphDbService.getNodeById(idNodeStart);
			Node endNode = graphDbService.getNodeById(idNodeEnd);
			EstimateEvaluator<Double> estimateEvaluator = new EstimateEvaluator<Double>()
			{
				public Double getCost(final Node node, final Node goal)
				{
					double dx = (Double) node.getProperty(OSMAttribute.LONGITUDE_PROPERTY) - (Double) goal.getProperty(OSMAttribute.LONGITUDE_PROPERTY);
					double dy = (Double) node.getProperty(OSMAttribute.LATITUDE_PROPERTY) - (Double) goal.getProperty(OSMAttribute.LATITUDE_PROPERTY);
					double result = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
					return result;
				}
			};
			PathExpander expander = PathExpanders.forTypesAndDirections(RelTypes.CAR_BIDIRECTIONAL_RELATION, Direction.BOTH, RelTypes.CAR_ONEWAY_RELATION, Direction.OUTGOING);
			List<WeightedPath> paths = new ArrayList<WeightedPath>();
			WeightedPath path = GraphAlgoFactory.aStar(expander, CommonEvaluators.doubleCostEvaluator(OSMAttribute.EDGE_LENGTH_PROPERTY), estimateEvaluator).findSinglePath(startNode, endNode);// tas.findSinglePath(startNode,
			tx.success();
		}
		catch (Exception e)
		{

			tx.failure();
			logger.fatal(e.getMessage(), e);
			throw new IllegalStateException(e);
		}
		finally
		{
			if (tx != null)
			{

				tx.close();
			}
			graphDbService.shutdown();
		}
	}

	@SuppressWarnings("unused")
	private void loadWholeGraphInMemory()
	{
		long startLoadTime = System.currentTimeMillis();
		Node start;
		int nodesNumber = 0;
		int relationShipNumber = 0;
		for (Node n : ggo.getAllNodes())
		{
			n.getPropertyKeys();
			nodesNumber++;
			for (Relationship relationship : n.getRelationships())
			{
				start = relationship.getStartNode();
			}
		}
		for (Relationship r : ggo.getAllRelationships())
		{
			start = r.getStartNode();
			r.getPropertyKeys();
			relationShipNumber++;
		}
		long finalLoadTime = System.currentTimeMillis();
		logger.info("Graph loaded into memory; loading time: "+( finalLoadTime-startLoadTime )+" millis. Loaded "+nodesNumber+" nodes and "+relationShipNumber+" relations"); 
	}
}