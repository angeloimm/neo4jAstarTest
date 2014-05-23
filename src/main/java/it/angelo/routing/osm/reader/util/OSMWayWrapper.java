package it.angelo.routing.osm.reader.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

public class OSMWayWrapper extends OSMElement
{

	private Way osmWay;
	boolean isCar;
	boolean isFerry;
	private Long speedCar;
	private Long speedBike;
	private Long speedFoot;
	private boolean isOnewayForCar;
	private boolean isReverseForCar;
	private boolean isDoppioSensoForCar;
	private boolean isSaveWay;
	private boolean isSterrata;
	private List<Long> wayNodeIds;

	public OSMWayWrapper(Way way)
	{

		osmWay = way;
		Collection<Tag> wayTags = way.getTags();
		tags = new HashMap<String, String>(wayTags.size());
		for (Tag tag : wayTags)
		{

			tags.put(tag.getKey(), tag.getValue());
		}
		wayNodeIds = new ArrayList<Long>();
		for (WayNode wayNode : way.getWayNodes())
		{
			wayNodeIds.add(wayNode.getNodeId());
		}
	}

	public Way getOsmWay()
	{
		return osmWay;
	}

	public boolean isValid(AbstractChecker wayChecker)
	{
		// Se non ci sono nodi nella strada...la strada è da scartare; ignoriamo
		// strade rotte
		List<WayNode> nodes = getOsmWay().getWayNodes();
		if (nodes == null || nodes.isEmpty() || nodes.size() < 2)
		{
			return false;
		}
		// Se non ha tag viene esclusa; ignoriamo la geometria multipoligono
		Collection<Tag> tags = getOsmWay().getTags();
		if (tags == null || tags.isEmpty())
		{
			return false;
		}
		// Controlliamo la strada
		wayChecker.isWayValid(this);
		return isCar;
	}

	public boolean isCar()
	{
		return isCar;
	}

	public void setCar(boolean isCar)
	{
		this.isCar = isCar;
	}

	public boolean isFerry()
	{
		return isFerry;
	}

	public void setFerry(boolean isFerry)
	{
		this.isFerry = isFerry;
	}

	public Long getSpeedCar()
	{
		return speedCar;
	}

	public void setSpeedCar(Long speedCar)
	{
		this.speedCar = speedCar;
	}

	public Long getSpeedBike()
	{
		return speedBike;
	}

	public void setSpeedBike(Long speedBike)
	{
		this.speedBike = speedBike;
	}

	public Long getSpeedFoot()
	{
		return speedFoot;
	}

	public void setSpeedFoot(Long speedFoot)
	{
		this.speedFoot = speedFoot;
	}

	public boolean isOnewayForCar()
	{
		return isOnewayForCar;
	}

	public void setOnewayForCar(boolean isOnewayForCar)
	{
		this.isOnewayForCar = isOnewayForCar;
	}

	public boolean isReverseForCar()
	{
		return isReverseForCar;
	}

	public void setReverseForCar(boolean isReverseForCar)
	{
		this.isReverseForCar = isReverseForCar;
	}

	public boolean isDoppioSensoForCar()
	{
		return isDoppioSensoForCar;
	}

	public void setDoppioSensoForCar(boolean isDoppioSensoForCar)
	{
		this.isDoppioSensoForCar = isDoppioSensoForCar;
	}

	public boolean isSaveWay()
	{
		return isSaveWay;
	}

	public void setSaveWay(boolean isSaveWay)
	{
		this.isSaveWay = isSaveWay;
	}

	public boolean isSterrata()
	{
		return isSterrata;
	}

	public void setSterrata(boolean isSterrata)
	{
		this.isSterrata = isSterrata;
	}

	public List<Long> getWayNodeIds()
	{
		return wayNodeIds;
	}

	public void setWayNodeIds(List<Long> wayNodeIds)
	{
		this.wayNodeIds = wayNodeIds;
	}

	public void setOsmWay(Way osmWay)
	{
		this.osmWay = osmWay;
	}
}