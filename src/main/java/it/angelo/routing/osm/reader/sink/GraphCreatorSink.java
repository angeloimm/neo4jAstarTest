package it.angelo.routing.osm.reader.sink;

import it.angelo.routing.graph.relation.types.RelTypes;
import it.angelo.routing.osm.reader.util.AbstractChecker;
import it.angelo.routing.osm.reader.util.DouglasPeucker;
import it.angelo.routing.osm.reader.util.IConstants;
import it.angelo.routing.osm.reader.util.OSMAttribute;
import it.angelo.routing.osm.reader.util.OSMElementsUtil;
import it.angelo.routing.osm.reader.util.OSMRelationWrapper;
import it.angelo.routing.osm.reader.util.OSMTurnRelation;
import it.angelo.routing.osm.reader.util.OSMTurnRelation.Type;
import it.angelo.routing.osm.reader.util.OSMWayWrapper;
import it.angelo.routing.osm.reader.util.OsmNodeWrapper;
import it.angelo.routing.osm.reader.util.PointList;
import it.angelo.routing.osm.reader.util.PropertiesLoader;
import it.angelo.routing.osm.reader.util.RoutPoint;
import it.angelo.routing.osm.reader.util.StringUtils;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.index.lucene.unsafe.batchinsert.LuceneBatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserterIndex;
import org.neo4j.unsafe.batchinsert.BatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserters;
import org.neo4j.unsafe.batchinsert.BatchRelationship;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import com.carrotsearch.hppc.LongLongOpenHashMap;
import com.carrotsearch.hppc.LongObjectOpenHashMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.io.WKTWriter;

public class GraphCreatorSink implements Sink
{

	/**
	 * The logger
	 */
	private static final Log logger = LogFactory.getLog(GraphCreatorSink.class.getName());

	private GeometryFactory geometryFactory = null;
	private static final String NO_NAME_STREET = "Strada Senza Nome";
	private Label mainNodeLabel;
	private long relazioniTotali;
	private String neo4jDbPath;
	private LongLongOpenHashMap nodesMap;
	private LongLongOpenHashMap graphMainNodes;
	private LongObjectOpenHashMap<OsmNodeWrapper> graphSecondaryNodes;
	private WKTWriter wktWrit;
	private LongObjectOpenHashMap<OsmNodeWrapper> osmNodeIdToNodeFlagsMap;
	private LongObjectOpenHashMap<String> nodeGeometryMap;
	private AbstractChecker wayCheckers;
	private long newUniqueOSMId = -Long.MAX_VALUE;
	private Double traffic_signal_penalty_default;
	private final DouglasPeucker simplifyAlgo = new DouglasPeucker();
	private BatchInserter inserter = null;
	private BatchInserterIndexProvider indexProvider = null;
	private BatchInserterIndex osmWayIdPropertyIndex = null;

	public GraphCreatorSink(String nomeFile, String neo4jDbPath, LongLongOpenHashMap nodesMap, AbstractChecker checker)
	{

		if (nomeFile == null || nomeFile.trim().equals(""))
		{
			throw new IllegalArgumentException("Impossibile proseguire; passato un nome file vuoto o null <" + nomeFile + ">");
		}

		this.nodesMap = nodesMap;
		this.neo4jDbPath = neo4jDbPath;
		graphMainNodes = new LongLongOpenHashMap();
		graphSecondaryNodes = new LongObjectOpenHashMap<OsmNodeWrapper>();
		this.osmNodeIdToNodeFlagsMap = new LongObjectOpenHashMap<OsmNodeWrapper>();
		this.wayCheckers = checker;
		this.geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);
		nodeGeometryMap = new LongObjectOpenHashMap<String>();
		wktWrit = new WKTWriter();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openstreetmap.osmosis.core.task.v0_6.Initializable#initialize(java
	 * .util.Map)
	 */
	@Override
	public void initialize(Map<String, Object> arg0)
	{
		PropertiesLoader props = PropertiesLoader.getInstance();
		Map<String, String> config = new HashMap<String, String>();
		config.put("neostore.nodestore.db.mapped_memory", props.getNodestoreMappedMemorySize());
		config.put("neostore.relationshipstore.db.mapped_memory", props.getRelationshipstoreMappedMemorySize());
		config.put("neostore.propertystore.db.mapped_memory", props.getNodestorePropertystoreMappedMemorySize());
		config.put("neostore.propertystore.db.strings.mapped_memory", props.getStringsMappedMemorySize());
		config.put("neostore.propertystore.db.arrays.mapped_memory", props.getArraysMappedMemorySize());
		config.put("keep_logical_logs", props.getKeepLogicalLogs());
		config.put("dump_configuration", props.getDumpConfiguration());

		// Definizione labels
		mainNodeLabel = DynamicLabel.label(IConstants.MAIN_POINTS_LABEL_NAME);
		// Inizializzazione batchInserter
		this.inserter = BatchInserters.inserter(neo4jDbPath, config);
		indexProvider = new LuceneBatchInserterIndexProvider(inserter);
		inserter.createDeferredSchemaIndex(mainNodeLabel).on(OSMAttribute.LATITUDE_PROPERTY).create();
		inserter.createDeferredSchemaIndex(mainNodeLabel).on(OSMAttribute.LONGITUDE_PROPERTY).create();
		osmWayIdPropertyIndex = indexProvider.relationshipIndex("osmWayIdProperties", MapUtil.stringMap("type", "exact"));
		osmWayIdPropertyIndex.setCacheCapacity(OSMAttribute.OSM_WAY_ID_PROPERTY, 100000);
		traffic_signal_penalty_default = new Double(props.getTrafficSignalPenaltyDefault());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openstreetmap.osmosis.core.lifecycle.Completable#complete()
	 */
	@Override
	public void complete()
	{

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openstreetmap.osmosis.core.lifecycle.Releasable#release()
	 */
	@Override
	public void release()
	{
		try
		{
			indexProvider.shutdown();
			inserter.shutdown();
		}
		catch (Exception e)
		{
			logger.fatal("Errore nel release: " + e.getMessage(), e);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openstreetmap.osmosis.core.task.v0_6.Sink#process(org.openstreetmap
	 * .osmosis.core.container.v0_6.EntityContainer)
	 */
	@Override
	public void process(EntityContainer entityContainer)
	{
		Entity entity = entityContainer.getEntity();

		// Processo i Nodi
		if (entity instanceof Node)
		{
			// Creo il nodo se il suo ID è contenuto nell'array di Long
			Node osmFileNode = (Node) entity;
			OsmNodeWrapper osmNodeWrapper = new OsmNodeWrapper(osmFileNode);
			String wkt = wktWrit.write(geometryFactory.createPoint(new Coordinate(osmNodeWrapper.getLongitude(), osmNodeWrapper.getLatitude())));
			this.nodeGeometryMap.put(osmNodeWrapper.getOsmNodeId(), wkt);
			long osmFileNodeId = osmFileNode.getId();
			// Creo i nodi; controllo se l'ID del nodo è contenuto
			// all'interno dell'array dei nodi principali o secondari
			Long nodeType = this.nodesMap.remove(osmFileNodeId);
			if (nodeType != 0)
			{
				if (nodeType == PrepareBatchNodeIdsSinkImpl.MAIN_NODE)
				{

					processNode(osmNodeWrapper, true);
				}
				else
				{
					processNode(osmNodeWrapper, false);
				}
			}
		}
		else if (entity instanceof Way)
		{
			OSMWayWrapper osmWayWrapper = new OSMWayWrapper((Way) entity);
			if (osmWayWrapper.isValid(this.wayCheckers))
			{

				String nomeStrada = null, refName = null;
				nomeStrada = osmWayWrapper.getTag("name");
				refName = osmWayWrapper.getTag("ref");
				if (refName != null && !refName.isEmpty())
				{
					if (nomeStrada == null || nomeStrada.isEmpty())
					{
						nomeStrada = refName;
					}
					else
					{
						nomeStrada += ", " + refName;
					}
				}
				nomeStrada = nomeStrada == null ? NO_NAME_STREET : nomeStrada;
				List<Long> wayNodeIds = osmWayWrapper.getWayNodeIds();
				if (wayNodeIds.size() > 1)
				{
					long first = wayNodeIds.get(0);
					long last = wayNodeIds.get(wayNodeIds.size() - 1);

					double firstLat = getTmpLatitude(first), firstLon = getTmpLongitude(first);
					double lastLat = getTmpLatitude(last), lastLon = getTmpLongitude(last);
					if (firstLat != Double.NaN && firstLon != Double.NaN && lastLat != Double.NaN && lastLon != Double.NaN)
					{
						double estimatedDist = OSMElementsUtil.getDistanzaInMetri(firstLat, firstLon, lastLat, lastLon);
						osmWayWrapper.setInternalTag("estimated_distance", estimatedDist);
						osmWayWrapper.setInternalTag("estimated_center", new RoutPoint((firstLat + lastLat) / 2, (firstLon + lastLon) / 2));
					}
				}
				boolean res = this.wayCheckers.handleWayTags(osmWayWrapper);
				if (!res)
					return;
				List<Long> subList = null;
				List<RelTypes> relationTypeList = new ArrayList<RelTypes>();
				final int size = wayNodeIds.size();
				int lastBarrier = -1;
				for (int i = 0; i < size; i++)
				{
					long nodeId = wayNodeIds.get(i);
					OsmNodeWrapper nodeFlags = getNodeFlagsMap().get(nodeId);

					if (null != nodeFlags && nodeFlags.isAnyBarriers())
					{
						// Controlliamo se la strada è valida per il
						// percorso di cui il nodo è una barriera
						if (nodeFlags.isBarrierForCar() && osmWayWrapper.isCar())
						{
							getNodeFlagsMap().remove(nodeId);
							Long newGraphNodeId = addBarrierNode(nodeId);
							if (i > 0)
							{
								if (lastBarrier < 0)
									lastBarrier = 0;

								subList = new ArrayList<Long>(wayNodeIds.subList(lastBarrier, i + 1));
								subList.set(subList.size() - 1, newGraphNodeId);
								if (osmWayWrapper.isCar() && osmWayWrapper.isDoppioSensoForCar())
								{
									relationTypeList.add(RelTypes.CAR_BIDIRECTIONAL_RELATION);
								}

								insertNeo4jWays(osmWayWrapper, subList, nomeStrada, relationTypeList);
								subList.clear();
								subList.add(newGraphNodeId);
								subList.add(nodeId);
								relationTypeList.clear();
								if (osmWayWrapper.isCar() && osmWayWrapper.isDoppioSensoForCar() && !nodeFlags.isBarrierForCar())
								{
									relationTypeList.add(RelTypes.CAR_BIDIRECTIONAL_RELATION);
								}
								else if (osmWayWrapper.isCar() && !osmWayWrapper.isDoppioSensoForCar() && !nodeFlags.isBarrierForCar())
								{
									relationTypeList.add(RelTypes.CAR_ONEWAY_RELATION);
								}
								insertNeo4jWays(osmWayWrapper, subList, nomeStrada, relationTypeList);
							}
							else
							{

								subList = new ArrayList<Long>();
								subList.add(nodeId);
								subList.add(newGraphNodeId);
								relationTypeList.clear();
								if (osmWayWrapper.isCar() && osmWayWrapper.isDoppioSensoForCar() && !nodeFlags.isBarrierForCar())
								{
									relationTypeList.add(RelTypes.CAR_BIDIRECTIONAL_RELATION);
								}
								else if (osmWayWrapper.isCar() && !osmWayWrapper.isDoppioSensoForCar() && !nodeFlags.isBarrierForCar())
								{
									relationTypeList.add(RelTypes.CAR_ONEWAY_RELATION);
								}
								insertNeo4jWays(osmWayWrapper, subList, nomeStrada, relationTypeList);
								wayNodeIds.set(0, newGraphNodeId);
							}
							lastBarrier = i;
						}
					}
				}
				relationTypeList.clear();
				if (osmWayWrapper.isCar() && osmWayWrapper.isDoppioSensoForCar())
				{
					relationTypeList.add(RelTypes.CAR_BIDIRECTIONAL_RELATION);
				}
				else if (osmWayWrapper.isCar() && !osmWayWrapper.isDoppioSensoForCar())
				{
					relationTypeList.add(RelTypes.CAR_ONEWAY_RELATION);
				}
				if (lastBarrier >= 0)
				{
					if (lastBarrier < size - 1)
					{

						subList.clear();
						subList = new ArrayList<Long>(wayNodeIds.subList(lastBarrier, size));

						insertNeo4jWays(osmWayWrapper, subList, nomeStrada, relationTypeList);
					}
				}
				else
				{
					insertNeo4jWays(osmWayWrapper, wayNodeIds, nomeStrada, relationTypeList);
				}
			}
			else
			{
				if (logger.isDebugEnabled())
				{
					logger.debug("La strada con id OSM " + osmWayWrapper.getOsmWay().getId() + " non è una strada valida in base ai controlli effettuati; non verrà processata");
				}
			}
		}
		else if (entity instanceof Relation)
		{

			OSMRelationWrapper osmRelationWrapper = new OSMRelationWrapper((Relation) entity);

			processRelation(osmRelationWrapper);
		}
	}

	private void insertNeo4jWays(OSMWayWrapper osmWayWrapper, List<Long> wayNodeIds, String nomeStrada, List<RelTypes> relationTypeList)
	{
		try
		{
			List<OsmNodeWrapper> geometryInfo = new ArrayList<OsmNodeWrapper>();
			long startNodeId = -1;
			long endNodeId = -1;
			for (int i = 0; i < wayNodeIds.size(); i++)
			{
				long nodeId = wayNodeIds.get(i);
				if (i == 0 && !this.graphMainNodes.containsKey(nodeId) && graphSecondaryNodes.containsKey(nodeId))
				{
					addGraphNode(graphSecondaryNodes.get(nodeId), true);
					if (logger.isDebugEnabled())
					{

						logger.debug("Nodo con ID OSM " + nodeId + " convertito in tower");
					}
				}
				else if (i == (wayNodeIds.size() - 1) && !this.graphMainNodes.containsKey(nodeId) && graphSecondaryNodes.containsKey(nodeId))
				{

					addGraphNode(graphSecondaryNodes.get(nodeId), true);
					if (logger.isDebugEnabled())
					{

						logger.debug("Nodo con ID " + nodeId + " convertito in tower");
					}
				}
				if (graphMainNodes.containsKey(nodeId))
				{

					if (startNodeId == -1)
					{

						startNodeId = nodeId;
					}
					else
					{

						endNodeId = nodeId;
					}
				}
				else if (graphSecondaryNodes.containsKey(nodeId))
				{

					geometryInfo.add(graphSecondaryNodes.get(nodeId));
				}
				else
				{

					if (logger.isDebugEnabled())
					{

						logger.debug("Il nodo con ID " + nodeId + " non appartiene né ai nodi principali né a quelli secondari");
					}
				}
				if (startNodeId != -1 && endNodeId != -1)
				{
					for (RelTypes relationType : relationTypeList)
					{

						if (osmWayWrapper.isDoppioSensoForCar() && relationType.equals(RelTypes.CAR_BIDIRECTIONAL_RELATION))
						{
							createRelationship(startNodeId, endNodeId, osmWayWrapper, nomeStrada, geometryInfo, relationType, false);
						}
						else if (osmWayWrapper.isReverseForCar() && relationType.equals(RelTypes.CAR_ONEWAY_RELATION))
						{

							Collections.reverse(geometryInfo);
							createRelationship(endNodeId, startNodeId, osmWayWrapper, nomeStrada, geometryInfo, relationType, true);
							Collections.reverse(geometryInfo);
						}
						else
						{
							createRelationship(startNodeId, endNodeId, osmWayWrapper, nomeStrada, geometryInfo, relationType, true);
						}

					}
					startNodeId = endNodeId;
					endNodeId = -1;
					geometryInfo.clear();
				}
			}
		}
		catch (Exception e)
		{
			logger.fatal("Errore: " + e.getMessage(), e);
			throw new IllegalStateException(e);
		}
	}

	private void createRelationship(long startNodeId, long endNodeId, OSMWayWrapper osmWayWrapper, String nomeStrada, List<OsmNodeWrapper> geometryInfo, RelTypes relationType, boolean isOneWay) throws Exception
	{
		Long osmWayId = osmWayWrapper.getOsmWay().getId();
		Long speed = null;
		boolean canHasTrafficSignalPenalty = false;

		if (relationType.equals(RelTypes.CAR_ONEWAY_RELATION) || relationType.equals(RelTypes.CAR_BIDIRECTIONAL_RELATION))
		{
			speed = osmWayWrapper.getSpeedCar();
			canHasTrafficSignalPenalty = true;
		}
		if (relationType != null)
		{

			try
			{
				Map<String, Object> relationProps = new HashMap<String, Object>();
				Map<String, Object> props = inserter.getNodeProperties(graphMainNodes.get(startNodeId));
				if (props.containsKey(OSMAttribute.TRAFFIC_SIGNAL) && canHasTrafficSignalPenalty)
					relationProps.put(OSMAttribute.TRAFFIC_SIGNAL_PENALTY, traffic_signal_penalty_default);

				relationProps.put(OSMAttribute.OSM_WAY_ID_PROPERTY, osmWayWrapper.getOsmWay().getId());
				relationProps.put(OSMAttribute.EDGE_SPEED_PROPERTY, speed);
				if (logger.isDebugEnabled())
				{

					logger.debug("nomeStrada : " + nomeStrada + "; way id : " + osmWayId);
				}

				relationProps.put(OSMAttribute.NOME_STRADA, nomeStrada);
				Geometry geom = null;
				double lunghezzaArco = 0;
				if (geometryInfo != null && !geometryInfo.isEmpty())
				{
					final int size = geometryInfo.size();
					Object[] elements = geometryInfo.toArray();
					Coordinate[] geoInfo = new Coordinate[size];
					double prevLat = Double.NaN;
					double prevLon = Double.NaN;
					if (props.containsKey(OSMAttribute.LATITUDE_PROPERTY))
					{
						prevLat = (Double) props.get(OSMAttribute.LATITUDE_PROPERTY);
					}
					if (props.containsKey(OSMAttribute.LONGITUDE_PROPERTY))
					{
						prevLon = (Double) props.get(OSMAttribute.LONGITUDE_PROPERTY);
					}
					double lat;
					double lon;
					for (int i = 0; i < size; i++)
					{

						lat = ((OsmNodeWrapper) elements[i]).getLatitude();
						lon = ((OsmNodeWrapper) elements[i]).getLongitude();
						geoInfo[i] = new Coordinate(lon, lat);

						lunghezzaArco += OSMElementsUtil.getDistanzaInMetri(prevLat, prevLon, lat, lon);
						prevLat = lat;
						prevLon = lon;
					}
					double endLat = Double.NaN;
					double endLon = Double.NaN;
					Map<String, Object> endNodeProps = inserter.getNodeProperties(graphMainNodes.get(endNodeId));
					if (endNodeProps.containsKey(OSMAttribute.LATITUDE_PROPERTY))
					{
						endLat = (Double) endNodeProps.get(OSMAttribute.LATITUDE_PROPERTY);
					}
					if (endNodeProps.containsKey(OSMAttribute.LONGITUDE_PROPERTY))
					{
						endLon = (Double) endNodeProps.get(OSMAttribute.LONGITUDE_PROPERTY);
					}
					lunghezzaArco += OSMElementsUtil.getDistanzaInMetri(prevLat, prevLon, endLat, endLon);

					if (geometryInfo.size() > 1)
					{

						PointList pillarNodes = new PointList(geoInfo);
						simplifyAlgo.simplify(pillarNodes);
						geom = new LineString(new CoordinateArraySequence(pillarNodes.toCoordinateArray()), geometryFactory);

					}
					else
					{

						geom = new Point(new CoordinateArraySequence(geoInfo), geometryFactory);
					}

					// Aggiungo le informazioni sulla geometria di nodi
					// secondari, sulla relazione tra startGraphNode e
					// endGraphNode
					relationProps.put(OSMAttribute.GEOMETRY_INFO, getWkt(geom));

				}
				else
				{
					if (logger.isDebugEnabled())
					{

						logger.debug("Strada con ID osm " + osmWayId + " numero di punti che compongono la strada: " + (geometryInfo != null ? geometryInfo.size() : 0) + ". Non conservo nessuna informazione sulla strada;");
					}
					double startLat = Double.NaN;
					double startLon = Double.NaN;
					Map<String, Object> startNodeProps = inserter.getNodeProperties(graphMainNodes.get(startNodeId));
					if (startNodeProps.containsKey(OSMAttribute.LATITUDE_PROPERTY))
					{
						startLat = (Double) startNodeProps.get(OSMAttribute.LATITUDE_PROPERTY);
					}
					if (startNodeProps.containsKey(OSMAttribute.LONGITUDE_PROPERTY))
					{
						startLon = (Double) startNodeProps.get(OSMAttribute.LONGITUDE_PROPERTY);
					}
					double endLat = Double.NaN;
					double endLon = Double.NaN;
					Map<String, Object> endNodeProps = inserter.getNodeProperties(graphMainNodes.get(endNodeId));
					if (endNodeProps.containsKey(OSMAttribute.LATITUDE_PROPERTY))
					{
						endLat = (Double) endNodeProps.get(OSMAttribute.LATITUDE_PROPERTY);
					}
					if (endNodeProps.containsKey(OSMAttribute.LONGITUDE_PROPERTY))
					{
						endLon = (Double) endNodeProps.get(OSMAttribute.LONGITUDE_PROPERTY);
					}
					// Calcolo la lunghezza dell'arco considerando solo
					// startNode e endNode
					lunghezzaArco = OSMElementsUtil.getDistanzaInMetri(startLat, startLon, endLat, endLon);

				}

				// Setto la lunghezza dell'arco
				if (lunghezzaArco == 0)
				{
					// As investigation shows often two paths should have
					// crossed via
					// one identical point
					// but end up in two very close points.
					lunghezzaArco = 0.0001;
				}

				// Inserisco la lunghezza dell'arco, utile per il calcolo del
				// percorso
				relationProps.put(OSMAttribute.EDGE_LENGTH_PROPERTY, lunghezzaArco);

				// #AG - 20/05/2014 : Aggiunta gestione del Tag loc_ref
				String via_code = null, id_arco_amat = null;
				String loc_ref = osmWayWrapper.getTag("loc_ref");
				if (!StringUtils.isEmpty(loc_ref))
				{
					String[] splitTag = loc_ref.split("_");
					if (splitTag.length == 2)
					{
						via_code = splitTag[0];
						id_arco_amat = splitTag[1];

						relationProps.put(OSMAttribute.VIA_CODE, via_code);
						relationProps.put(OSMAttribute.ID_ARCO_AMAT, id_arco_amat);
					}
				}

				// Creo relazione su neo4j
				long relId = inserter.createRelationship(graphMainNodes.get(startNodeId), graphMainNodes.get(endNodeId), relationType, relationProps);
				// Aggiungo l'indice sull'OSM
				osmWayIdPropertyIndex.add(relId, relationProps);
				// Se il numero di oggetti da indicizzare è pari a 10000
				// effettuo il flush; NB: utilizzare il flush il meno possibile
				if (relazioniTotali >= 10000)
				{
					osmWayIdPropertyIndex.flush();
					relazioniTotali = 0;
				}
			}
			catch (Exception e)
			{
				logger.fatal("Errore nella creazione delle relazioni; messaggio errore: " + e.getMessage(), e);
			}
		}
	}

	private void processNode(OsmNodeWrapper osmNodeWrapper, boolean isMainNode)
	{

		addGraphNode(osmNodeWrapper, isMainNode);

		// Analizzo i tag dei nodi per trovare eventuali Nodi Barriera
		if (osmNodeWrapper.hasTags())
		{

			osmNodeWrapper.analizeNodeForBarriers(this.wayCheckers);

			if (osmNodeWrapper.isBarrierForCar())

				getNodeFlagsMap().put(osmNodeWrapper.getOsmNode().getId(), osmNodeWrapper);
		}
	}

	private void addGraphNode(OsmNodeWrapper osmNodeWrapper, boolean isMainNode)
	{

		if (isMainNode)
		{

			long osmFileNodeId = osmNodeWrapper.getOsmNodeId();
			long graphNodeId = createGraphNode(osmNodeWrapper);
			graphMainNodes.put(osmFileNodeId, graphNodeId);
		}
		else
		{

			graphSecondaryNodes.put(osmNodeWrapper.getOsmNodeId(), osmNodeWrapper);
		}
	}

	private long createGraphNode(OsmNodeWrapper osmNodeWrapper)
	{

		try
		{
			Map<String, Object> nodeProps = new HashMap<String, Object>();
			double x = osmNodeWrapper.getLongitude();
			double y = osmNodeWrapper.getLatitude();
			nodeProps.put(OSMAttribute.LATITUDE_PROPERTY, y);
			nodeProps.put(OSMAttribute.LONGITUDE_PROPERTY, x);
			nodeProps.put(OSMAttribute.OSM_NODE_ID_PROPERTY, osmNodeWrapper.getOsmNodeId());
			// Se il nodo rappresenta un semaforo, aggiungo questa informazione
			// utile per il Routing al nodo in NEO4j
			if (osmNodeWrapper.isTrafficSignal())
				nodeProps.put(OSMAttribute.TRAFFIC_SIGNAL, true);
			// Creo il nodo
			long graphNodeId = inserter.createNode(nodeProps, mainNodeLabel);
			// aggiungo l'identificativo del nodo sul grafo
			osmNodeWrapper.setGraphNodeId(graphNodeId);
			// Inserimento NODO in DB Postgres
			insertPostgresNodo(osmNodeWrapper, x, y);
			// Indicizzo solo i nodi principali
			// mainPointsLayer.add(graphNode);

			return graphNodeId;
		}
		catch (Exception e)
		{

			String message = "Errore durante la creazione del nodo con ID osm " + osmNodeWrapper.getOsmNode().getId() + ". Messaggio errore: " + e.getMessage();
			logger.fatal(message, e);
			throw new IllegalStateException(message);
		}

	}

	/**
	 * Create a copy of the barrier node
	 */
	private Long addBarrierNode(Long nodeId)
	{

		OsmNodeWrapper newBarrierNode = new OsmNodeWrapper();
		double longitude = getTmpLongitude(nodeId);
		double latitude = getTmpLatitude(nodeId);

		newBarrierNode.setLongitude(longitude);
		newBarrierNode.setLatitude(latitude);

		Long id = createNewNodeId();
		newBarrierNode.setOsmNodeId(id);

		// aggiungo alla mappa dei nodi secondari, il nodo barriera appena
		// creato,
		// così verrà gestito dal metodo insertOsmWay nella stessa maniera degli
		// altri
		graphSecondaryNodes.put(id, newBarrierNode);
		return id;
	}

	private String getWkt(Geometry geometry) throws Exception
	{
		WKTWriter wktWriter = new WKTWriter();
		StringWriter sw = new StringWriter();
		wktWriter.write(geometry, sw);
		return sw.toString();
	}

	/**
	 * Inserisce un nodo all'interno del DB Postgres
	 * 
	 * @param osmNodeWrapper
	 *            - Nodo da inserire nel DB
	 * @param x
	 *            - coordinata X del Nodo : Longitudine
	 * @param y
	 *            - coordinata Y del Nodo : Latitudine
	 * @throws Exception
	 */
	private void insertPostgresNodo(OsmNodeWrapper osmNodeWrapper, double x, double y) throws Exception
	{

		// Inserisco il nodo appena creato in Neo4j anche in Postgres
		Point geometry = geometryFactory.createPoint(new Coordinate(osmNodeWrapper.getLongitude(), osmNodeWrapper.getLatitude()));
		if (logger.isDebugEnabled())
		{

			StringBuilder sb = new StringBuilder();
			sb.append("Coordinata X : " + x + ". ");
			sb.append("Coordinata Y : " + y + ". ");
			sb.append("Geometry : " + geometry);
			logger.debug(sb.toString());
		}
	}

	private void processRelation(OSMRelationWrapper osmRelationWrapper)
	{

		if (osmRelationWrapper.hasTag("type", "restriction"))
		{

			OSMTurnRelation turnRelation = createTurnRelation(osmRelationWrapper);
			if (turnRelation != null)
			{
				this.wayCheckers.analyzeTurnRelation(turnRelation, this);
			}
		}
	}

	/**
	 * Creates an OSM turn relation out of an unspecified OSM relation
	 * 
	 * @param OSMRelationWrapper
	 *            relation
	 * 
	 * @return the OSM turn relation, <code>null</code>, if unsupported turn
	 *         relation
	 */
	private OSMTurnRelation createTurnRelation(OSMRelationWrapper relation)
	{
		OSMTurnRelation.Type type = OSMTurnRelation.Type.getRestrictionType(relation.getTag("restriction"));
		if (type != OSMTurnRelation.Type.UNSUPPORTED)
		{
			long fromWayID = -1;
			long viaNodeID = -1;
			long toWayID = -1;

			for (RelationMember member : relation.getOsmRelation().getMembers())
			{
				if (EntityType.Way == member.getMemberType())
				{
					if ("from".equals(member.getMemberRole()))
					{
						fromWayID = member.getMemberId();
					}
					else if ("to".equals(member.getMemberRole()))
					{
						toWayID = member.getMemberId();
					}
				}
				else if (EntityType.Node == member.getMemberType() && "via".equals(member.getMemberRole()))
				{
					viaNodeID = member.getMemberId();
				}
			}
			if (type != OSMTurnRelation.Type.UNSUPPORTED && fromWayID >= 0 && toWayID >= 0 && viaNodeID >= 0)
			{
				return new OSMTurnRelation(fromWayID, viaNodeID, toWayID, type, relation.getOsmRelationId());
			}
		}
		return null;
	}

	/**
	 * Dato l'identificativo del nodo osm, ne ritorna la latitudine (coordinata
	 * Y) recuperandola dalla mappa dei nodi secondari (in caso di nodi PILLAR)
	 * o direttamente dal grafo NEO4j (nel caso di nodi TOWER)
	 * 
	 * @param id
	 *            - identificativo del nodo osm
	 * @return double latitude - coordinata Y del nodo dato
	 */
	double getTmpLatitude(long osmNodeId)
	{

		try
		{
			if (osmNodeId == 0)
			{
				return Double.NaN;
			}
			else if (!graphMainNodes.containsKey(osmNodeId))
			{
				OsmNodeWrapper node = graphSecondaryNodes.get(osmNodeId);
				if (null != node)
					return node.getLatitude();
				else
					return Double.NaN;
			}
			else
			{
				Map<String, Object> props = inserter.getNodeProperties(graphMainNodes.get(osmNodeId));
				double lat = Double.NaN;
				if (props.containsKey(OSMAttribute.LATITUDE_PROPERTY))
				{
					lat = (Double) props.get(OSMAttribute.LATITUDE_PROPERTY);
				}
				return lat;
			}
		}
		catch (Exception e)
		{

			String message = "Errore durante il recupero della latitudine del nodo con ID osm " + osmNodeId + ". Messaggio errore: " + e.getMessage();
			logger.fatal(message, e);
			throw new IllegalStateException(message);
		}

	}

	/**
	 * Dato l'identificativo del nodo osm, ne ritorna la longitudine (coordinata
	 * X) recuperandola dalla mappa dei nodi secondari (in caso di nodi PILLAR)
	 * o direttamente dal grafo NEO4j (nel caso di nodi TOWER)
	 * 
	 * @param id
	 *            - identificativo del nodo osm
	 * @return double longitude - coordinata X del nodo dato
	 */
	double getTmpLongitude(long osmNodeId)
	{

		try
		{
			if (osmNodeId == 0)
			{
				return Double.NaN;
			}
			else if (!graphMainNodes.containsKey(osmNodeId))
			{
				OsmNodeWrapper node = graphSecondaryNodes.get(osmNodeId);
				if (null != node)
					return node.getLongitude();
				else
					return Double.NaN;
			}
			else
			{
				Map<String, Object> props = inserter.getNodeProperties(graphMainNodes.get(osmNodeId));
				double lon = Double.NaN;
				if (props.containsKey(OSMAttribute.LONGITUDE_PROPERTY))
				{
					lon = (Double) props.get(OSMAttribute.LONGITUDE_PROPERTY);
				}
				return lon;
			}
		}
		catch (Exception e)
		{

			String message = "Errore durante il recupero della longitudine del nodo con ID osm " + osmNodeId + ". Messaggio errore: " + e.getMessage();
			logger.fatal(message, e);
			throw new IllegalStateException(message);
		}
	}

	/**
	 * Dato l'id del nodo sul file osm (nodeOsmId) ritorna il nodo su Neo4j
	 * (neo4jId)
	 */
	public long getGraphNodeId(long nodeOsmId)
	{
		return graphMainNodes.get(nodeOsmId);
	}

	public void manageRestrictions(OSMTurnRelation turnRelation, List<String> relTypesList)
	{
		long graphNodeId = getGraphNodeId(turnRelation.getViaOsm());
		try
		{
			if (graphNodeId == 0)
			{
				if (logger.isDebugEnabled())
				{

					logger.debug("Unknown node osm id : " + turnRelation.getViaOsm());
				}
				return;
			}

			Long neo4jEdgeIdFrom = null;
			Long toOsmWayId = null;
			Long fromOsmWayId = null;
			Iterable<BatchRelationship> batchRels = inserter.getRelationships(graphNodeId);
			for (BatchRelationship batchRel : batchRels)
			{
				// Se il nodo finale della relazione ha ID pari a graphNodeId ed
				// il tipo di relazione è uguale a relTypes
				// --> relazione entrante
				if (batchRel.getEndNode() == graphNodeId && relTypesList.contains(batchRel.getType().name())
				/* batchRel.getType().name().equals(relTypesList.name()) */)
				{
					Map<String, Object> relProps = inserter.getRelationshipProperties(batchRel.getId());
					if (relProps.containsKey(OSMAttribute.OSM_WAY_ID_PROPERTY))
					{
						fromOsmWayId = (Long) relProps.get(OSMAttribute.OSM_WAY_ID_PROPERTY);
					}
					if (fromOsmWayId == turnRelation.getFromOsm())
					{
						neo4jEdgeIdFrom = batchRel.getId();
						break;
					}
				}
			}
			for (BatchRelationship batchRel : batchRels)
			{
				// Se il nodo iniziale della relazione ha ID pari a graphNodeId
				// ed il tipo di relazione è uguale a relTypes
				// --> relazione uscente
				if (batchRel.getStartNode() == graphNodeId && relTypesList.contains(batchRel.getType().name())
				/* batchRel.getType().name().equals(relTypesList.name()) */)
				{
					Map<String, Object> nodeProperties = inserter.getNodeProperties(graphNodeId);
					if (neo4jEdgeIdFrom != null)
					{
						if (turnRelation.getRestriction() == Type.NOT)
						{

							Map<String, Object> relProps = inserter.getRelationshipProperties(batchRel.getId());
							if (relProps.containsKey(OSMAttribute.OSM_WAY_ID_PROPERTY))
							{
								toOsmWayId = (Long) relProps.get(OSMAttribute.OSM_WAY_ID_PROPERTY);
							}

							if (batchRel.getId() != neo4jEdgeIdFrom && toOsmWayId == turnRelation.getToOsm())
							{

								nodeProperties.put(OSMAttribute.FROM_RESTRICTION_GRAPH_WAY_ID, neo4jEdgeIdFrom);
								nodeProperties.put(OSMAttribute.TO_RESTRICTION_GRAPH_WAY_ID, batchRel.getId());
								inserter.setNodeProperties(graphNodeId, nodeProperties);
							}
						}
						else if (turnRelation.getRestriction() == Type.ONLY)
						{

							// Aggiungo la restrizione a tutti i Turn eccetto a
							// quello dato
							Map<String, Object> relProps = inserter.getRelationshipProperties(batchRel.getId());
							if (relProps.containsKey(OSMAttribute.OSM_WAY_ID_PROPERTY))
							{
								toOsmWayId = (Long) relProps.get(OSMAttribute.OSM_WAY_ID_PROPERTY);
							}
							if (batchRel.getId() != neo4jEdgeIdFrom && toOsmWayId != turnRelation.getFromOsm() // Questo
																												// controllo
																												// è
																												// fatto
																												// perchè
																												// per
																												// una
																												// stessa
																												// osmWayId
																												// noi
									// possiamo avere più di una relazione
									// (Doppio Senso)
									&& toOsmWayId != turnRelation.getToOsm())
							{

								nodeProperties.put(OSMAttribute.FROM_RESTRICTION_GRAPH_WAY_ID, neo4jEdgeIdFrom);
								nodeProperties.put(OSMAttribute.TO_RESTRICTION_GRAPH_WAY_ID, batchRel.getId());
								inserter.setNodeProperties(graphNodeId, nodeProperties);
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Could not built node costs table for relation of node [osmId:" + turnRelation.getViaOsm() + "].", e);
		}
	}

	private long createNewNodeId()
	{
		return newUniqueOSMId++;
	}

	LongObjectOpenHashMap<OsmNodeWrapper> getNodeFlagsMap()
	{
		return osmNodeIdToNodeFlagsMap;
	}
}