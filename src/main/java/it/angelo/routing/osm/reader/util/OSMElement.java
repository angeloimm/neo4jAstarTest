package it.angelo.routing.osm.reader.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class OSMElement
{

	protected Map<String, String> tags;
	protected Map<String, Object> iProperties;

	public OSMElement()
	{

	}

	public String getTagValue(String tagName)
	{
		if (tags.isEmpty())
		{

			return null;
		}
		return tags.get(tagName);
	}

	public Map<String, String> getTags()
	{
		return tags;
	}

	public void replaceTags(HashMap<String, String> newTags)
	{
		tags = newTags;
	}

	public boolean hasTags()
	{
		return tags != null && !tags.isEmpty();
	}

	public String getTag(String name)
	{
		if (tags == null)
			return null;

		return tags.get(name);
	}

	public void setTag(String name, String value)
	{
		if (tags == null)
			tags = new HashMap<String, String>();

		tags.put(name, value);
	}

	/**
	 * Chaeck that the object has a given tag with a given value.
	 */
	public boolean hasTag(String key, String value)
	{
		if (tags == null)
			return false;

		String val = tags.get(key);
		return value.equals(val);
	}

	/**
	 * Check that a given tag has one of the specified values. If no values are
	 * given, just checks for presence of the tag
	 */
	public boolean hasTag(String key, String... values)
	{
		if (tags == null)
			return false;

		String osmValue = tags.get(key);
		if (osmValue == null)
			return false;

		// tag present, no values given: success
		if (values.length == 0)
			return true;

		for (String val : values)
		{
			if (val.equals(osmValue))
				return true;
		}
		return false;
	}

	/**
	 * Check that a given tag has one of the specified values.
	 */
	public final boolean hasTag(String key, Set<String> values)
	{
		if (tags == null)
			return false;

		String osmValue = tags.get(key);
		return osmValue != null && values.contains(osmValue);
	}

	/**
	 * Check a number of tags in the given order for the any of the given
	 * values. Used to parse hierarchical access restrictions
	 */
	public boolean hasTag(String[] keyList, Set<String> values)
	{
		if (tags == null)
			return false;

		for (int i = 0; i < keyList.length; i++)
		{
			String osmValue = tags.get(keyList[i]);
			if (osmValue != null && values.contains(osmValue))
				return true;
		}
		return false;
	}

	public void removeTag(String name)
	{
		if (tags != null)
			tags.remove(name);
	}

	public void clearTags()
	{
		tags = null;
	}

	public void setInternalTag(String key, Object value)
	{
		if (iProperties == null)
			iProperties = new HashMap<String, Object>();

		iProperties.put(key, value);
	}

	public boolean hasInternalTag(String key)
	{
		if (iProperties == null)
			return false;
		return iProperties.containsKey(key);
	}

	@SuppressWarnings("unchecked")
	public <T> T getInternalTag(String key, T defaultValue)
	{
		if (iProperties == null)
			return defaultValue;
		T val = (T) iProperties.get(key);
		if (val == null)
			return defaultValue;
		return val;
	}

}
