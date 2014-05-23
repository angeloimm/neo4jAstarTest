package it.angelo.routing.osm.reader.util;

import static java.lang.Math.*;

public class DistancePlaneProjection
{
	/**
	 * mean radius of the earth
	 */
	public final static double R = 6371000; // m

	public double calcDist(double fromLat, double fromLon, double toLat, double toLon)
	{
		double dLat = toRadians(toLat - fromLat);
		double dLon = toRadians(toLon - fromLon);
		// use mean latitude as reference point for delta_lon
		double tmp = cos(toRadians((fromLat + toLat) / 2)) * dLon;
		double normedDist = dLat * dLat + tmp * tmp;
		return R * sqrt(normedDist);
	}

	public double calcDenormalizedDist(double normedDist)
	{
		return R * sqrt(normedDist);
	}

	public double calcNormalizedDist(double dist)
	{
		double tmp = dist / R;
		return tmp * tmp;
	}

	public double calcNormalizedDist(double fromLat, double fromLon, double toLat, double toLon)
	{
		double dLat = toRadians(toLat - fromLat);
		double dLon = toRadians(toLon - fromLon);
		double left = cos(toRadians((fromLat + toLat) / 2)) * dLon;
		return dLat * dLat + left * left;
	}

	/**
	 * New edge distance calculation where no validEdgeDistance check would be
	 * necessary
	 * <p>
	 * 
	 * @return the normalized distance of the query point "r" to the project
	 *         point "c" onto the line segment a-b
	 */
	public double calcNormalizedEdgeDistance(double r_lat_deg, double r_lon_deg, double a_lat_deg, double a_lon_deg, double b_lat_deg, double b_lon_deg, boolean reduceToSegment)
	{
		double shrink_factor = cos((toRadians(a_lat_deg) + toRadians(b_lat_deg)) / 2);
		double a_lat = a_lat_deg;
		double a_lon = a_lon_deg * shrink_factor;

		double b_lat = b_lat_deg;
		double b_lon = b_lon_deg * shrink_factor;

		double r_lat = r_lat_deg;
		double r_lon = r_lon_deg * shrink_factor;

		double delta_lon = b_lon - a_lon;
		double delta_lat = b_lat - a_lat;

		if (delta_lat == 0)
			// special case: horizontal edge
			return calcNormalizedDist(a_lat_deg, r_lon_deg, r_lat_deg, r_lon_deg);

		if (delta_lon == 0)
			// special case: vertical edge
			return calcNormalizedDist(r_lat_deg, a_lon_deg, r_lat_deg, r_lon_deg);

		double norm = delta_lon * delta_lon + delta_lat * delta_lat;
		double factor = ((r_lon - a_lon) * delta_lon + (r_lat - a_lat) * delta_lat) / norm;

		// make new calculation compatible to old
		if (reduceToSegment)
		{
			if (factor > 1)
				factor = 1;
			else if (factor < 0)
				factor = 0;
		}
		// x,y is projection of r onto segment a-b
		double c_lon = a_lon + factor * delta_lon;
		double c_lat = a_lat + factor * delta_lat;
		return calcNormalizedDist(c_lat, c_lon / shrink_factor, r_lat_deg, r_lon_deg);
	}

	public String toString()
	{
		return "PLANE_PROJ";
	}
}