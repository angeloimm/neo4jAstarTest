package it.angelo.routing.osm.reader.util;

import java.util.Collection;
import java.util.HashMap;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

public class OsmNodeWrapper extends OSMElement
{
	private double latitude;
	private double longitude;
	private Long graphNodeId;
	private Long osmNodeId;
	private boolean isBarrierForCar;
	private Node osmNode;

	public OsmNodeWrapper()
	{
	}

	public OsmNodeWrapper(Node osmNode)
	{

		this(osmNode.getLatitude(), osmNode.getLongitude(), osmNode.getId());
		this.osmNode = osmNode;
		this.osmNodeId = osmNode.getId();
		Collection<Tag> wayTags = osmNode.getTags();
		tags = new HashMap<String, String>(wayTags.size());
		for (Tag tag : wayTags)
		{

			tags.put(tag.getKey(), tag.getValue());
		}

	}

	public OsmNodeWrapper(double latitude, double longitude, Long osmNodeId)
	{
		super();
		this.latitude = latitude;
		this.longitude = longitude;
		this.osmNodeId = osmNodeId;
	}

	public double getLatitude()
	{
		return latitude;
	}

	public void setLatitude(double latitude)
	{
		this.latitude = latitude;
	}

	public double getLongitude()
	{
		return longitude;
	}

	public void setLongitude(double longitude)
	{
		this.longitude = longitude;
	}

	public void setGraphNodeId(long graphNodeId)
	{
		this.graphNodeId = graphNodeId;
	}

	public Long getGraphNodeId()
	{
		return graphNodeId;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Latitude (y): ");
		sb.append(getLatitude());
		sb.append(". Longitude (x): ");
		sb.append(getLongitude());
		sb.append(". ID grafo: ");
		sb.append(getGraphNodeId());
		return sb.toString();
	}

	public Node getOsmNode()
	{
		return osmNode;
	}

	public Long getOsmNodeId()
	{
		return osmNodeId;
	}

	public void setOsmNodeId(long osmNodeId)
	{
		this.osmNodeId = osmNodeId;
	}

	/**
	 * Analizza il nodo per capire se si tratta di una barriera, salvando
	 * l'informazione che indica per quale percorso risulta esserlo
	 * 
	 * @param wayCheckersList
	 *            - lista contenente le tipologie di percorsi effettuabili
	 * 
	 * 
	 * @return true se la strada è valida, false altrimenti
	 */
	public void analizeNodeForBarriers(AbstractChecker wayCheckersList)
	{
		wayCheckersList.analyzeNodeTags(this);
	}

	/**
	 * @return the isBarrierForCar
	 */
	public boolean isBarrierForCar()
	{
		return isBarrierForCar;
	}

	public boolean isAnyBarriers()
	{
		return isBarrierForCar;
	}

	/**
	 * @param isBarrierForCar
	 *            the isBarrierForCar to set
	 */
	public void setBarrierForCar(boolean isBarrierForCar)
	{
		this.isBarrierForCar = isBarrierForCar;
	}

	/**
	 * 
	 * @return true se il nodo rappresenta un semaforo; false altrimenti
	 */
	public boolean isTrafficSignal()
	{
		return hasTag("highway", "traffic_signals");
	}
}
