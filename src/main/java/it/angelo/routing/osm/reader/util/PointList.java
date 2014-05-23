package it.angelo.routing.osm.reader.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;

public class PointList
{
	private double[] latitudes;
	private double[] longitudes;
	private int size = 0;

	public PointList()
	{
		this(10);
	}

	public PointList(int cap)
	{
		latitudes = new double[cap];
		longitudes = new double[cap];
	}

	public PointList(Coordinate[] geoInfo)
	{
		this(geoInfo.length);
		for (int i = 0; i < geoInfo.length; i++)
		{
			add(geoInfo[i].y, geoInfo[i].x);
		}

	}

	public void set(int index, double lat, double lon)
	{
		if (index >= size)
			throw new ArrayIndexOutOfBoundsException("index has to be smaller than size " + size);

		latitudes[index] = lat;
		longitudes[index] = lon;
	}

	public void add(double lat, double lon)
	{
		int newSize = size + 1;
		if (newSize >= latitudes.length)
		{
			int cap = (int) (newSize * 1.7);
			if (cap < 8)
				cap = 8;
			latitudes = Arrays.copyOf(latitudes, cap);
			longitudes = Arrays.copyOf(longitudes, cap);
		}

		latitudes[size] = lat;
		longitudes[size] = lon;
		size = newSize;
	}

	public int size()
	{
		return size;
	}

	public int getSize()
	{
		return size;
	}

	public boolean isEmpty()
	{
		return size == 0;
	}

	public double getLatitude(int index)
	{
		if (index >= size)
		{
			throw new ArrayIndexOutOfBoundsException("Tried to access PointList with too big index! " + "index:" + index + ", size:" + size);
		}
		return latitudes[index];
	}

	public double getLongitude(int index)
	{
		if (index >= size)
			throw new ArrayIndexOutOfBoundsException("Tried to access PointList with too big index! " + "index:" + index + ", size:" + size);

		return longitudes[index];
	}

	public void reverse()
	{
		int max = size / 2;
		for (int i = 0; i < max; i++)
		{
			int swapIndex = size - i - 1;

			double tmp = latitudes[i];
			latitudes[i] = latitudes[swapIndex];
			latitudes[swapIndex] = tmp;

			tmp = longitudes[i];
			longitudes[i] = longitudes[swapIndex];
			longitudes[swapIndex] = tmp;
		}
	}

	public void clear()
	{
		size = 0;
	}

	public void trimToSize(int newSize)
	{
		if (newSize > size)
			throw new IllegalArgumentException("new size needs be smaller than old size");

		size = newSize;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < size; i++)
		{
			if (i > 0)
				sb.append(", ");

			sb.append('(');
			sb.append(latitudes[i]);
			sb.append(',');
			sb.append(longitudes[i]);
			sb.append(')');
		}
		return sb.toString();
	}

	/**
	 * Attention: geoJson is LON,LAT
	 */
	public List<Double[]> toGeoJson()
	{
		ArrayList<Double[]> points = new ArrayList<Double[]>(size);
		for (int i = 0; i < size; i++)
		{
			points.add(new Double[] { getLongitude(i), getLatitude(i) });
		}
		return points;
	}

	public Coordinate[] toCoordinateArray()
	{
		Coordinate[] geoInfo = new Coordinate[size];
		for (int i = 0; i < size; i++)
		{
			geoInfo[i] = new Coordinate(getLongitude(i), getLatitude(i));
		}

		return geoInfo;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
			return false;

		final PointList other = (PointList) obj;
		if (this.size != other.size)
			return false;

		for (int i = 0; i < size; i++)
		{
			if (!NumHelper.equalsEps(latitudes[i], other.latitudes[i]))
				return false;

			if (!NumHelper.equalsEps(longitudes[i], other.longitudes[i]))
				return false;
		}
		return true;
	}

	public PointList clone(boolean reverse)
	{
		PointList clonePL = new PointList(size);
		for (int i = 0; i < size; i++)
		{
			clonePL.add(latitudes[i], longitudes[i]);
		}
		if (reverse)
			clonePL.reverse();
		return clonePL;
	}

	public PointList copy(int from, int end)
	{
		if (from > end)
			throw new IllegalArgumentException("from must be smaller or equals to end");
		if (from < 0 || end > size)
			throw new IllegalArgumentException("Illegal interval: " + from + ", " + end + ", size:" + size);

		PointList copyPL = new PointList(size);
		for (int i = from; i < end; i++)
		{
			copyPL.add(latitudes[i], longitudes[i]);
		}
		return copyPL;
	}

	@Override
	public int hashCode()
	{
		int hash = 5;
		for (int i = 0; i < latitudes.length; i++)
		{
			hash = 73 * hash + (int) Math.round(latitudes[i] * 1000000);
			hash = 73 * hash + (int) Math.round(longitudes[i] * 1000000);
		}
		hash = 73 * hash + this.size;
		return hash;
	}

	/**
	 * Takes the string from a json array ala [lon1,lat1], [lon2,lat2], ... and
	 * fills the list from it.
	 */
	public void parseJSON(String str)
	{
		for (String latlon : str.split("\\["))
		{
			if (latlon.trim().length() == 0)
				continue;

			String ll[] = latlon.split(",");
			String lat = ll[1].replace("]", "").trim();
			add(Double.parseDouble(lat), Double.parseDouble(ll[0].trim()));
		}
	}

	public static final PointList EMPTY = new PointList(0)
	{
		@Override
		public void set(int index, double lat, double lon)
		{
			throw new RuntimeException("cannot change EMPTY PointList");
		}

		@Override
		public void add(double lat, double lon)
		{
			throw new RuntimeException("cannot change EMPTY PointList");
		}

		@Override
		public double getLatitude(int index)
		{
			throw new RuntimeException("cannot access EMPTY PointList");
		}

		@Override
		public double getLongitude(int index)
		{
			throw new RuntimeException("cannot access EMPTY PointList");
		}

		@Override
		public void clear()
		{
			throw new RuntimeException("cannot change EMPTY PointList");
		}

		@Override
		public void trimToSize(int newSize)
		{
			throw new RuntimeException("cannot change EMPTY PointList");
		}

		@Override
		public void parseJSON(String str)
		{
			throw new RuntimeException("cannot change EMPTY PointList");
		}

		@Override
		public PointList copy(int from, int end)
		{
			throw new RuntimeException("cannot copy EMPTY PointList");
		}

		@Override
		public PointList clone(boolean reverse)
		{
			return this;
		}
	};

	public RoutPoint toGHPoint(int index)
	{
		return new RoutPoint(getLatitude(index), getLongitude(index));
	}
}