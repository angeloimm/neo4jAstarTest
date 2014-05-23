package it.angelo.routing.osm.reader.sink;

import it.angelo.routing.osm.reader.util.AbstractChecker;
import it.angelo.routing.osm.reader.util.OSMWayWrapper;

import java.util.List;
import java.util.Map;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import com.carrotsearch.hppc.LongLongOpenHashMap;

public class PrepareBatchNodeIdsSinkImpl implements Sink
{

	public static final byte MAIN_NODE = 2;
	public static final byte SECONDARY_NODE = 1;

	private LongLongOpenHashMap nodesMap;
	private AbstractChecker car;

	public PrepareBatchNodeIdsSinkImpl(AbstractChecker checker)
	{

		nodesMap = new LongLongOpenHashMap();
		car = checker;
	}

	@Override
	public void initialize(Map<String, Object> metaData)
	{
	}

	@Override
	public void complete()
	{
	}

	@Override
	public void release()
	{
	}

	@Override
	public void process(EntityContainer entityContainer)
	{
		Entity entity = entityContainer.getEntity();
		if (entity instanceof Way)
		{

			Way aWay = (Way) entity;
			OSMWayWrapper oww = new OSMWayWrapper(aWay);

			// Controllo se la strada è valida o meno; se si aggiungo i nodi....
			if (oww.isValid(car))
			{
				List<Long> wayNodeIds = oww.getWayNodeIds();
				if (wayNodeIds != null && !wayNodeIds.isEmpty())
				{

					for (Long wayNodeId : wayNodeIds)
					{

						addNodeToMap(wayNodeId);
					}
				}
			}
		}
	}

	private void addNodeToMap(long nodeId)
	{

		// Controllo se il map contiene già l'id del nodo o meno
		if (nodesMap.containsKey(nodeId))
		{

			nodesMap.put(nodeId, MAIN_NODE);
		}
		else
		{

			nodesMap.put(nodeId, SECONDARY_NODE);
		}
	}

	public LongLongOpenHashMap getNodesMap()
	{

		return this.nodesMap;
	}
}