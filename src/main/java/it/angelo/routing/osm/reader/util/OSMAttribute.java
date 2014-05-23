package it.angelo.routing.osm.reader.util;

public interface OSMAttribute
{

	// Proprietà presenti sul NODO
	public static final String OSM_NODE_ID_PROPERTY = "osmNodeId";
	public static final String LATITUDE_PROPERTY = "y";
	public static final String LONGITUDE_PROPERTY = "x";
	public static final String TRAFFIC_SIGNAL = "traffic_signal";
	public static final String FROM_RESTRICTION_GRAPH_WAY_ID = "from_restriction_graph_way_id";
	public static final String TO_RESTRICTION_GRAPH_WAY_ID = "to_restriction_graph_way_id";
	public static final String NEAREST_NODE_ID = "nearest_node_id";
	// Proprietà presenti sulla RELAZIONE
	public static final String OSM_WAY_ID_PROPERTY = "osmWayId";
	public static final String GEOMETRY_INFO = "pinf_geometry_info";
	public static final String NOME_STRADA = "nomeStrada";
	public static final String EDGE_LENGTH_PROPERTY = "edgeLength";
	public static final String EDGE_SPEED_PROPERTY = "edgeSpeed";
	public static final String EDGE_REAL_TIME_SPEED_PROPERTY = "edgeRealTimeSpeed";
	public static final String TRAFFIC_SIGNAL_PENALTY = "traffic_signal_penalty";
	public static final String GRAPH_WAY_ID = "graph_way_id";
	public static final String VIA_CODE = "via_code";
	public static final String ID_ARCO_AMAT = "id_arco_amat";
}
