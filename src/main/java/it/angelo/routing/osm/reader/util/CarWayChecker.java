package it.angelo.routing.osm.reader.util;

import it.angelo.routing.graph.relation.types.RelTypes;
import it.angelo.routing.osm.reader.sink.GraphCreatorSink;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.osmosis.core.task.v0_6.Sink;

public class CarWayChecker extends AbstractChecker
{
	private static final Map<String, Integer> TRACKTYPE_SPEED = new HashMap<String, Integer>();
	private static final Set<String> BAD_SURFACE = new HashSet<String>();
	private static final Map<String, Integer> SPEED = new HashMap<String, Integer>();

	static
	{

		TRACKTYPE_SPEED.put("grade1", 20); // paved
		TRACKTYPE_SPEED.put("grade2", 15); // now unpaved - gravel mixed with
											// ...
		TRACKTYPE_SPEED.put("grade3", 10); // ... hard and soft materials
		TRACKTYPE_SPEED.put("grade4", 5); // ... some hard or compressed
											// materials
		TRACKTYPE_SPEED.put("grade5", 5); // ... no hard materials.
											// soil/sand/grass
		BAD_SURFACE.add("cobblestone");
		BAD_SURFACE.add("grass_paver");
		BAD_SURFACE.add("gravel");
		BAD_SURFACE.add("sand");
		BAD_SURFACE.add("paving_stones");
		BAD_SURFACE.add("dirt");
		BAD_SURFACE.add("ground");
		BAD_SURFACE.add("grass");
		// autobahn
		SPEED.put("motorway", 100);
		SPEED.put("motorway_link", 70);
		// bundesstraße
		SPEED.put("trunk", 70);
		SPEED.put("trunk_link", 65);
		// linking bigger town
		SPEED.put("primary", 65);
		SPEED.put("primary_link", 60);
		// linking towns + villages
		SPEED.put("secondary", 60);
		SPEED.put("secondary_link", 50);
		// streets without middle line separation
		SPEED.put("tertiary", 50);
		SPEED.put("tertiary_link", 40);
		SPEED.put("unclassified", 30);
		SPEED.put("residential", 30);
		// spielstraße
		SPEED.put("living_street", 5);
		SPEED.put("service", 20);
		// unknown road
		SPEED.put("road", 20);
		// forestry stuff
		SPEED.put("track", 15);
	}

	protected int getSpeed(OSMWayWrapper way)
	{
		String highwayValue = way.getTag("highway");
		Integer speed = SPEED.get(highwayValue);
		if (speed == null)
			throw new IllegalStateException("car, no speed found for:" + highwayValue);

		if (highwayValue.equals("track"))
		{
			String tt = way.getTag("tracktype");
			if (!StringUtils.isEmpty(tt))
			// if (!Helper.isEmpty(tt))
			{
				Integer tInt = TRACKTYPE_SPEED.get(tt);
				if (tInt != null)
					speed = tInt;
			}
		}
		return speed;
	}

	public CarWayChecker()
	{

		super();
		restrictions = new String[] { "motorcar", "motor_vehicle", "vehicle", "access" };
		restrictedValues.add("private");
		restrictedValues.add("agricultural");
		restrictedValues.add("forestry");
		restrictedValues.add("no");
		restrictedValues.add("restricted");
		intended.add("yes");
		intended.add("permissive");
		potentialBarriers.add("gate");
		potentialBarriers.add("lift_gate");
		potentialBarriers.add("kissing_gate");
		potentialBarriers.add("swing_gate");
		absoluteBarriers.add("bollard");
		absoluteBarriers.add("stile");
		absoluteBarriers.add("turnstile");
		absoluteBarriers.add("cycle_barrier");
		absoluteBarriers.add("block");
		speedEncoder = new EncodedValue("Speed", SPEED.get("secondary"), SPEED.get("motorway"));
	}

	@Override
	public boolean isWayValid(OSMWayWrapper way)
	{

		String highwayValue = way.getTag("highway");
		if (highwayValue == null)
		{
			if (way.hasTag("route", ferries))
			{
				String motorcarTag = way.getTag("motorcar");
				if (motorcarTag == null)
					motorcarTag = way.getTag("motor_vehicle");

				if (motorcarTag == null && !way.hasTag("foot") && !way.hasTag("bicycle") || "yes".equals(motorcarTag))
				{
					way.setFerry(true);
					way.setCar(true);
					return true;
				}

			}
			way.setCar(false);
			return false;
		}

		if (!SPEED.containsKey(highwayValue))
		{
			way.setCar(false);
			return false;
		}

		if (way.hasTag("impassable", "yes") || way.hasTag("status", "impassable"))
		{
			way.setCar(false);
			return false;
		}

		// do not drive street cars into fords
		if ((way.hasTag("highway", "ford") || way.hasTag("ford")) && !way.hasTag(restrictions, intended))
		{
			way.setCar(false);
			return false;
		}

		// check access restrictions
		if (way.hasTag(restrictions, restrictedValues))
		{
			way.setCar(false);
			return false;
		}

		// do not drive cars over railways (sometimes incorrectly mapped!)
		if (way.hasTag("railway") && !way.hasTag("railway", acceptedRailways))
		{
			way.setCar(false);
			return false;
		}

		way.setCar(true);
		return true;
	}

	@Override
	public boolean handleWayTags(OSMWayWrapper way)
	{
		// if ((allowed & acceptBit) == 0)
		// return 0;

		if (!way.isCar)
			return false;

		long speed;

		// if ((allowed & ferryBit) != 0)
		if (way.isFerry)
		{

			// encoded = handleFerry(way, SPEED.get("living_street"),
			// SPEED.get("service"), SPEED.get("residential"));

			speed = handleFerry(way, SPEED.get("living_street"), SPEED.get("service"), SPEED.get("residential"));

			// encoded |= directionBitMask;

			// Strada ne reverse ne a senso unico (Doppio senso di default)
			way.setDoppioSensoForCar(true);

		}
		else
		{
			// get assumed speed from highway type
			speed = getSpeed(way);
			int maxspeed = parseSpeed(way.getTag("maxspeed"));
			// apply speed limit no matter of the road type
			if (maxspeed >= 0)
				// reduce speed limit to reflect average speed
				speed = Math.round(maxspeed * 0.9f);

			// limit speed to max 30 km/h if bad surface
			if (speed > 30 && way.hasTag("surface", BAD_SURFACE))
				speed = 30;

			if (speed > getMaxSpeed())
				speed = getMaxSpeed();

			// encoded = speedEncoder.setValue(speed);

			if (way.hasTag("oneway", oneways) || way.hasTag("junction", "roundabout"))
			{

				if (way.hasTag("oneway", "-1"))
					// encoded |= backwardBit;

					// Strada con verso invertito
					way.setReverseForCar(true);
				else
					// encoded |= forwardBit;

					// Strada a senso unico
					way.setOnewayForCar(true);
			}
			else
			{
				// encoded |= directionBitMask;

				// Strada ne reverse ne a senso unico (Doppio senso di default)
				way.setDoppioSensoForCar(true);

			}
		}

		way.setSpeedCar(speed);
		return true;
	}

	@Override
	public Integer handleRelationTags(OSMRelationWrapper relation, Integer oldCode)
	{
		return oldCode;
	}

	@Override
	public boolean analyzeNodeTags(OsmNodeWrapper node)
	{
		// absolute barriers always block
		if (node.hasTag("barrier", absoluteBarriers))
		{
			node.setBarrierForCar(true);
			return true;
		}

		boolean res = super.analyzeNodeTags(node);
		node.setBarrierForCar(res);
		return res;
	}

	/**
	 * Metodo che gestisce le restrizioni. Deve essere implementato da tutti i
	 * WayChecker che intendono gestire le restrizioni
	 * 
	 * @param turnRelation
	 *            - Restrizione da gestire
	 * @param createGraphSinkImpl
	 */
	@Override
	public void analyzeTurnRelation(OSMTurnRelation turnRelation, Sink sink)
	{
		if (sink instanceof GraphCreatorSink)
		{
			GraphCreatorSink createBatchGraphSinkImplNew = (GraphCreatorSink) sink;
			createBatchGraphSinkImplNew.manageRestrictions(turnRelation, getRelType());
		}
	}

	@Override
	public List<String> getRelType()
	{
		List<String> list = new ArrayList<String>();
		list.add(RelTypes.CAR_ONEWAY_RELATION.name());
		list.add(RelTypes.CAR_BIDIRECTIONAL_RELATION.name());

		return list;
	}

}