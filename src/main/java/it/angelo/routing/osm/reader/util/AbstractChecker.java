package it.angelo.routing.osm.reader.util;

import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

public abstract class AbstractChecker
{

	private static final Log logger = LogFactory.getLog(AbstractChecker.class.getName());
	protected String[] restrictions;
	protected HashSet<String> intended = new HashSet<String>();
	protected HashSet<String> restrictedValues = new HashSet<String>(5);
	protected HashSet<String> ferries = new HashSet<String>(5);
	protected HashSet<String> oneways = new HashSet<String>(5);
	protected HashSet<String> acceptedRailways = new HashSet<String>(5);
	protected HashSet<String> absoluteBarriers = new HashSet<String>(5);
	protected HashSet<String> potentialBarriers = new HashSet<String>(5);

	protected long directionBitMask = 0;
	public final static double KM_MILE = 1.609344;
	protected EncodedValue speedEncoder;

	public AbstractChecker()
	{

		// Valori comuni a tutti i checker
		oneways.add("yes");
		oneways.add("true");
		oneways.add("1");
		oneways.add("-1");
		ferries.add("shuttle_train");
		ferries.add("ferry");
		acceptedRailways.add("tram");
	}

	public abstract boolean isWayValid(OSMWayWrapper way);

	public abstract Integer handleRelationTags(OSMRelationWrapper relation, Integer oldCode);

	// public abstract RelTypes getRelType();

	public abstract List<String> getRelType();

	public abstract void analyzeTurnRelation(OSMTurnRelation turnRelation, Sink sink);

	/**
	 * Parse tags on nodes. Node tags can add to speed (like traffic_signals)
	 * where the value is strict negative or blocks access (like a barrier),
	 * then the value is strict positive.
	 */
	public boolean analyzeNodeTags(OsmNodeWrapper node)
	{
		// movable barriers block if they are not marked as passable
		if (node.hasTag("barrier", potentialBarriers) && !node.hasTag(restrictions, intended) && !node.hasTag("locked", "no"))
			return true;

		if ((node.hasTag("highway", "ford") || node.hasTag("ford")) && !node.hasTag(restrictions, intended))
			return true;

		return false;
	}

	public int getMaxSpeed()
	{
		return (int) speedEncoder.getMaxValue();
	}

	/**
	 * Special handling for ferry ways.
	 */
	protected long handleFerry(OSMWayWrapper way, int unknownSpeed, int shortTripsSpeed, int longTripsSpeed)
	{
		// to hours
		double durationInHours = parseDuration(way.getTag("duration")) / 60d;
		if (durationInHours > 0)
			try
			{
				Double estimatedLength = way.getInternalTag("estimated_distance", null);
				if (estimatedLength != null)
				{
					estimatedLength /= 1000;
					shortTripsSpeed = (int) Math.round(estimatedLength / durationInHours / 1.4);
					if (shortTripsSpeed > getMaxSpeed())
						shortTripsSpeed = getMaxSpeed();
					longTripsSpeed = shortTripsSpeed;
				}
			}
			catch (Exception ex)
			{
			}

		if (durationInHours == 0)
		{
			return speedEncoder.setValue(unknownSpeed);
		}
		else if (durationInHours > 1)
		{
			return speedEncoder.setValue(longTripsSpeed);
		}
		else
		{
			return speedEncoder.setValue(shortTripsSpeed);
		}
	}

	public abstract boolean handleWayTags(OSMWayWrapper way);

	protected static int parseDuration(String str)
	{
		if (str == null)
			return 0;

		int index = str.indexOf(":");
		if (index > 0)
		{
			try
			{
				String hourStr = str.substring(0, index);
				String minStr = str.substring(index + 1);
				index = minStr.indexOf(":");
				int minutes = 0;
				if (index > 0)
				{
					// string contains hours too
					String dayStr = hourStr;
					hourStr = minStr.substring(0, index);
					minStr = minStr.substring(index + 1);
					minutes = Integer.parseInt(dayStr) * 60 * 24;
				}

				minutes += Integer.parseInt(hourStr) * 60;
				minutes += Integer.parseInt(minStr);
				return minutes;
			}
			catch (Exception ex)
			{
				logger.error("Cannot parse " + str + " using 0 minutes");
			}
		}
		return 0;
	}

	/**
	 * @return the speed in km/h
	 */
	static int parseSpeed(String str)
	{
		if (StringUtils.isEmpty(str))
		{
			return -1;
		}

		try
		{
			int val;
			// see https://en.wikipedia.org/wiki/Knot_%28unit%29#Definitions
			int mpInteger = str.indexOf("mp");
			if (mpInteger > 0)
			{
				str = str.substring(0, mpInteger).trim();
				val = Integer.parseInt(str);
				return (int) Math.round(val * KM_MILE);
			}

			int knotInteger = str.indexOf("knots");
			if (knotInteger > 0)
			{
				str = str.substring(0, knotInteger).trim();
				val = Integer.parseInt(str);
				return (int) Math.round(val * 1.852);
			}

			int kmInteger = str.indexOf("km");
			if (kmInteger > 0)
			{
				str = str.substring(0, kmInteger).trim();
			}
			else
			{
				kmInteger = str.indexOf("kph");
				if (kmInteger > 0)
				{
					str = str.substring(0, kmInteger).trim();
				}
			}

			return Integer.parseInt(str);
		}
		catch (Exception ex)
		{
			return -1;
		}
	}
}