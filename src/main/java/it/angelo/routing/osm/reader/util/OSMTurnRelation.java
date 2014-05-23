package it.angelo.routing.osm.reader.util;

import java.util.HashMap;
import java.util.Map;

public class OSMTurnRelation
{

	private long fromOsm;
	private long viaOsm;
	private long toOsm;
	private Type restriction;
	private long osmRelationId;

	public enum Type
	{
		UNSUPPORTED, NOT, ONLY;

		private static Map<String, Type> tags = new HashMap<String, Type>();

		static
		{
			tags.put("no_left_turn", NOT);
			tags.put("no_right_turn", NOT);
			tags.put("no_straight_on", NOT);
			tags.put("no_u_turn", NOT);
			tags.put("only_right_turn", ONLY);
			tags.put("only_left_turn", ONLY);
			tags.put("only_straight_on", ONLY);
		}

		public static Type getRestrictionType(String tag)
		{
			Type result = null;
			if (tag != null)
			{
				result = tags.get(tag);
			}
			return (result != null) ? result : UNSUPPORTED;
		}
	}

	public OSMTurnRelation(long fromWayID, long viaNodeID, long toWayID, Type restrictionType, long osmRelationId)
	{
		this.fromOsm = fromWayID;
		this.viaOsm = viaNodeID;
		this.toOsm = toWayID;
		this.restriction = restrictionType;
		this.osmRelationId = osmRelationId;
	}

	/**
	 * @return the viaOsm
	 */
	public long getViaOsm()
	{
		return viaOsm;
	}

	/**
	 * @return the fromOsm
	 */
	public long getFromOsm()
	{
		return fromOsm;
	}

	/**
	 * @return the toOsm
	 */
	public long getToOsm()
	{
		return toOsm;
	}

	/**
	 * @return the restriction
	 */
	public Type getRestriction()
	{
		return restriction;
	}

	/**
	 * 
	 * @return the osmRelationId
	 */
	public long getOsmRelationId()
	{
		return osmRelationId;
	}
}