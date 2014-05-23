package it.angelo.routing.osm.reader.util;

public class DouglasPeucker
{
	private double normedMaxDist;
	private DistancePlaneProjection calc;

	public DouglasPeucker()
	{
		calc = new DistancePlaneProjection();
		// 1m
		setMaxDistance(1);
	}

	/**
	 * maximum distance of discrepancy (from the normal way) in meter
	 */
	public DouglasPeucker setMaxDistance(double dist)
	{
		this.normedMaxDist = calc.calcNormalizedDist(dist);
		return this;
	}

	/**
	 * This method removes points which are close to the line (defined by
	 * maxDist).
	 * <p/>
	 * 
	 * @return removed nodes
	 */
	public int simplify(PointList points)
	{
		int removed = 0;
		int size = points.getSize();
		int delta = 500;
		int segments = size / delta + 1;
		int start = 0;
		for (int i = 0; i < segments; i++)
		{
			// start of next is end of last segment, except for the last
			removed += simplify(points, start, Math.min(size - 1, start + delta));
			start += delta;
		}

		compressNew(points, removed);
		return removed;
	}

	/**
	 * compress list: move points into EMPTY slots
	 */
	void compressNew(PointList points, int removed)
	{
		int freeIndex = -1;
		for (int currentIndex = 0; currentIndex < points.getSize(); currentIndex++)
		{
			if (Double.isNaN(points.getLatitude(currentIndex)))
			{
				if (freeIndex < 0)
				{
					freeIndex = currentIndex;
				}
				continue;
			}
			else if (freeIndex < 0)
			{
				continue;
			}

			points.set(freeIndex, points.getLatitude(currentIndex), points.getLongitude(currentIndex));
			points.set(currentIndex, Double.NaN, Double.NaN);
			// find next free index
			int max = currentIndex;
			int searchIndex = freeIndex + 1;
			freeIndex = currentIndex;
			for (; searchIndex < max; searchIndex++)
			{
				if (Double.isNaN(points.getLatitude(searchIndex)))
				{
					freeIndex = searchIndex;
					break;
				}
			}
		}
		points.trimToSize(points.getSize() - removed);
	}

	// keep the points of fromIndex and lastIndex
	int simplify(PointList points, int fromIndex, int lastIndex)
	{
		if (lastIndex - fromIndex < 2)
		{
			return 0;
		}
		int indexWithMaxDist = -1;
		double maxDist = -1;
		double firstLat = points.getLatitude(fromIndex);
		double firstLon = points.getLongitude(fromIndex);
		double lastLat = points.getLatitude(lastIndex);
		double lastLon = points.getLongitude(lastIndex);
		for (int i = fromIndex + 1; i < lastIndex; i++)
		{
			double lat = points.getLatitude(i);
			if (Double.isNaN(lat))
			{
				continue;
			}
			double lon = points.getLongitude(i);
			double dist = calc.calcNormalizedEdgeDistance(lat, lon, firstLat, firstLon, lastLat, lastLon, false);
			if (maxDist < dist)
			{
				indexWithMaxDist = i;
				maxDist = dist;
			}
		}

		if (indexWithMaxDist < 0)
		{
			throw new IllegalStateException("maximum not found in [" + fromIndex + "," + lastIndex + "]");
		}

		int counter = 0;
		if (maxDist < normedMaxDist)
		{
			for (int i = fromIndex + 1; i < lastIndex; i++)
			{
				points.set(i, Double.NaN, Double.NaN);
				counter++;
			}
		}
		else
		{
			counter = simplify(points, fromIndex, indexWithMaxDist);
			counter += simplify(points, indexWithMaxDist, lastIndex);
		}
		return counter;
	}
}
