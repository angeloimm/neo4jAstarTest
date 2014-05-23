package it.angelo.routing.osm.reader.util;

public abstract class StringUtils
{
	public static boolean isEmpty(String theString)
	{
		return (theString == null) || (theString.trim().equals(""));
	}
}
