package it.angelo.routing.osm.reader.util;

import static java.lang.Math.asin;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;

import org.apache.commons.math3.util.Precision;

public class OSMElementsUtil
{

	/**
	 * mean radius of the earth
	 */
	public final static double R = 6371000; // m

	public OSMElementsUtil()
	{
	}

	public static boolean isPrepareWaysWithRelationInfo(OSMRelationWrapper osmRelationWrapper)
	{

		return !osmRelationWrapper.isMetaRelation() && osmRelationWrapper.hasTag("type", "route");
	}

	/**
	 * Calculates distance of (from, to) in meter.
	 * <p/>
	 * http://en.wikipedia.org/wiki/Haversine_formula a = sin²(Δlat/2) +
	 * cos(lat1).cos(lat2).sin²(Δlong/2) c = 2.atan2(√a, √(1−a)) d = R.c
	 */
	public static double getDistanzaInMetri(double fromLat, double fromLon, double toLat, double toLon)
	{

		double sinDeltaLat = sin(toRadians(toLat - fromLat) / 2);
		double sinDeltaLon = sin(toRadians(toLon - fromLon) / 2);
		double normedDist = sinDeltaLat * sinDeltaLat + sinDeltaLon * sinDeltaLon * cos(toRadians(fromLat)) * cos(toRadians(toLat));

		return Precision.round(R * 2 * asin(sqrt(normedDist)), 3);
	}

}