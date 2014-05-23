package it.angelo.routing.osm.reader.util;

public interface IConstants
{
	/**
	 * Il nome del layer spaziale che conterrà i punti principali indicizzati
	 * spazialmente
	 */
	public static final String MAIN_POINTS_LAYER_NAME = "mainPointsLayer";
	public static final String MAIN_POINTS_LABEL_NAME = "nodoPrincipale";
	public static final String SECONDARY_POINTS_LABEL_NAME = "nodoSecondario";
	public static final int DRITTO = 1;
	public static final int GIRARE_LEGGERMENTE_DESTRA = 2;
	public static final int GIRARE_DESTRA = 3;
	public static final int GIRARE_SUBITO_DESTRA = 4;
	public static final int INVERSIONE_U = 5;
	public static final int GIRARE_SUBITO_SINISTRA = 6;
	public static final int GIRARE_SINISTRA = 7;
	public static final int GIRARE_LEGGERMENTE_SINISTRA = 8;
	public static final int DIRIGERSI_A = 9;
	public static final int IMBOCCARE = 10;
	public static final int IMMETTERSI_NELLA_ROTONDA = 11;
	public static final int USCIRE_DALLA_ROTONDA = 12;
	public static final int TENERSI_SULLA_ROTONDA = 13;
	public static final int PARTIRE_DALLA_FINE_DELLA_STRADA = 14;
	public static final int DESTINAZIONE_RAGGIUNTA = 15;
	public static final int IMMETTERSI = 16;
	public static final int USCIRE = 17;

	/* Costanti per la tipologia di calcolo del percorso */
	public static final String BICI = "BICI";
	public static final String PIEDI = "PIEDI";
	public static final String AUTO = "AUTO";

	/* Costanti per il tipo di tragitto da seguire */
	public static final String BREVE = "BREVE";

	/* Costanti per la ricerca del punto più vicino */
	public static final int FIRST_FINDEND_RELATIONSHIP = 0;
	public static final int FIRST_CLOSEST_LINESTRING = 1;
	public static final int NODO_FANTASMA = 2;
}