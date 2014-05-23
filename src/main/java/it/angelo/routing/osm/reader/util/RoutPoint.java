package it.angelo.routing.osm.reader.util;

public class RoutPoint
{

	private final static double DEFAULT_PRECISION = 1e-6;
	public double lat = Double.NaN;
	public double lon = Double.NaN;

	public RoutPoint()
	{
	}

	public RoutPoint(double lat, double lon)
	{
		this.lat = lat;
		this.lon = lon;
	}

	public static RoutPoint parse(String str)
	{
		// if the point is in the format of lat,lon we don't need to call
		// geocoding service
		String[] fromStrs = str.split(",");
		if (fromStrs.length == 2)
		{
			try
			{
				double fromLat = Double.parseDouble(fromStrs[0]);
				double fromLon = Double.parseDouble(fromStrs[1]);
				return new RoutPoint(fromLat, fromLon);
			}
			catch (Exception ex)
			{
			}
		}
		return null;
	}

	public double getLon()
	{
		return lon;
	}

	public double getLat()
	{
		return lat;
	}

	public boolean isValid()
	{
		return lat != Double.NaN && lon != Double.NaN;
	}

	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 83 * hash + (int) (Double.doubleToLongBits(this.lat) ^ (Double.doubleToLongBits(this.lat) >>> 32));
		hash = 83 * hash + (int) (Double.doubleToLongBits(this.lon) ^ (Double.doubleToLongBits(this.lon) >>> 32));
		return hash;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
			return false;

		final RoutPoint other = (RoutPoint) obj;
		return equalsEps(lat, other.lat) && equalsEps(lon, other.lon);
	}

	@Override
	public String toString()
	{
		return lat + "," + lon;
	}

	public static boolean equalsEps(double d1, double d2)
	{
		return Math.abs(d1 - d2) < DEFAULT_PRECISION;
	}
}
