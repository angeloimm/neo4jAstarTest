package it.angelo.routing.osm.reader.util;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PropertiesLoader
{
	private static final Log logger = LogFactory.getLog(PropertiesLoader.class.getName());
	private static PropertiesLoader theInstance;
	private String tmpFileDir;
	private String tmpNodiFileName;
	private String tmpArchiFileName;
	private String tmpStradeFileName;
	private String tmpTagsFileName;
	private String tmpRestrizioniFileName;
	private String dbConnectionUrl;
	private String dbUsername;
	private String dbPassword;
	private String osmFilesDirectory;
	private String neo4jDbPath;
	private String wayCheckers;
	private String sRid;
	private String nodestoreMappedMemorySize;
	private String relationshipstoreMappedMemorySize;
	private String nodestorePropertystoreMappedMemorySize;
	private String stringsMappedMemorySize;
	private String arraysMappedMemorySize;
	private String keepLogicalLogs;
	private String dumpConfiguration;
	private String trafficSignalPenaltyDefault;
	private String deleteCsvFile;
	private String cacheType;

	private PropertiesLoader()
	{
		try
		{
			Properties props = new Properties();
			props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("configuration.properties"));
			osmFilesDirectory = props.getProperty("pinf.osm.files.directory");
			neo4jDbPath = props.getProperty("pinf.neo4j.db.path");
			wayCheckers = props.getProperty("pinf.importer.wayCheckers");
			tmpFileDir = props.getProperty("tmpFileDir");
			tmpNodiFileName = props.getProperty("tmpNodiFileName");
			tmpArchiFileName = props.getProperty("tmpArchiFileName");
			tmpStradeFileName = props.getProperty("tmpStradeFileName");
			tmpTagsFileName = props.getProperty("tmpTagsFileName");
			tmpRestrizioniFileName = props.getProperty("tmpRestrizioniFileName");
			dbConnectionUrl = props.getProperty("pinf.db.hibernate.jdbcUrl");
			dbUsername = props.getProperty("pinf.db.hibernate.username");
			dbPassword = props.getProperty("pinf.db.hibernate.password");
			sRid = props.getProperty("pinf.grpah.srid");
			nodestoreMappedMemorySize = props.getProperty("nodestore_mapped_memory_size", "100M");
			relationshipstoreMappedMemorySize = props.getProperty("relationshipstore_mapped_memory_size", "3G");
			nodestorePropertystoreMappedMemorySize = props.getProperty("nodestore_propertystore_mapped_memory_size", "100M");
			stringsMappedMemorySize = props.getProperty("strings_mapped_memory_size", "200M");
			arraysMappedMemorySize = props.getProperty("arrays_mapped_memory_size", "50M");
			keepLogicalLogs = props.getProperty("keep_logical_logs", "true");
			dumpConfiguration = props.getProperty("dump_configuration", "true");
			setTrafficSignalPenaltyDefault(props.getProperty("traffic_signal_penalty_default"));
			setDeleteCsvFile(props.getProperty("deleteCsvFile"));
			setCacheType(props.getProperty("cache_type"));
		}
		catch (Exception e)
		{

			String s = "Errore durante il caricamento del file di properties; messaggio errore: " + e.getMessage();
			logger.fatal(s, e);
			throw new IllegalStateException(s, e);
		}
	}

	public static PropertiesLoader getInstance()
	{
		if (theInstance == null)
		{
			theInstance = new PropertiesLoader();
		}
		return theInstance;
	}

	public String getTmpFileDir()
	{
		return tmpFileDir;
	}

	public void setTmpFileDir(String tmpFileDir)
	{
		this.tmpFileDir = tmpFileDir;
	}

	public String getTmpNodiFileName()
	{
		return tmpNodiFileName;
	}

	public void setTmpNodiFileName(String tmpNodiFileName)
	{
		this.tmpNodiFileName = tmpNodiFileName;
	}

	public String getTmpArchiFileName()
	{
		return tmpArchiFileName;
	}

	public void setTmpArchiFileName(String tmpArchiFileName)
	{
		this.tmpArchiFileName = tmpArchiFileName;
	}

	public String getTmpStradeFileName()
	{
		return tmpStradeFileName;
	}

	public void setTmpStradeFileName(String tmpStradeFileName)
	{
		this.tmpStradeFileName = tmpStradeFileName;
	}

	public String getTmpTagsFileName()
	{
		return tmpTagsFileName;
	}

	public void setTmpTagsFileName(String tmpTagsFileName)
	{
		this.tmpTagsFileName = tmpTagsFileName;
	}

	public String getTmpRestrizioniFileName()
	{
		return tmpRestrizioniFileName;
	}

	public void setTmpRestrizioniFileName(String tmpRestrizioniFileName)
	{
		this.tmpRestrizioniFileName = tmpRestrizioniFileName;
	}

	public String getDbConnectionUrl()
	{
		return dbConnectionUrl;
	}

	public void setDbConnectionUrl(String dbConnectionUrl)
	{
		this.dbConnectionUrl = dbConnectionUrl;
	}

	public String getDbUsername()
	{
		return dbUsername;
	}

	public void setDbUsername(String dbUsername)
	{
		this.dbUsername = dbUsername;
	}

	public String getDbPassword()
	{
		return dbPassword;
	}

	public void setDbPassword(String dbPassword)
	{
		this.dbPassword = dbPassword;
	}
	public String getOsmFilesDirectory()
	{
		return osmFilesDirectory;
	}

	public void setOsmFilesDirectory(String osmFilesDirectory)
	{
		this.osmFilesDirectory = osmFilesDirectory;
	}

	public String getNeo4jDbPath()
	{
		return neo4jDbPath;
	}

	public void setNeo4jDbPath(String neo4jDbPath)
	{
		this.neo4jDbPath = neo4jDbPath;
	}

	public String getWayCheckers()
	{
		return wayCheckers;
	}

	public void setWayCheckers(String wayCheckers)
	{
		this.wayCheckers = wayCheckers;
	}

	public String getsRid()
	{
		return sRid;
	}

	public void setsRid(String sRid)
	{
		this.sRid = sRid;
	}

	public String getNodestoreMappedMemorySize()
	{
		return nodestoreMappedMemorySize;
	}

	public void setNodestoreMappedMemorySize(String nodestoreMappedMemorySize)
	{
		this.nodestoreMappedMemorySize = nodestoreMappedMemorySize;
	}

	public String getRelationshipstoreMappedMemorySize()
	{
		return relationshipstoreMappedMemorySize;
	}

	public void setRelationshipstoreMappedMemorySize(String relationshipstoreMappedMemorySize)
	{
		this.relationshipstoreMappedMemorySize = relationshipstoreMappedMemorySize;
	}

	public String getNodestorePropertystoreMappedMemorySize()
	{
		return nodestorePropertystoreMappedMemorySize;
	}

	public void setNodestorePropertystoreMappedMemorySize(String nodestorePropertystoreMappedMemorySize)
	{
		this.nodestorePropertystoreMappedMemorySize = nodestorePropertystoreMappedMemorySize;
	}

	public String getStringsMappedMemorySize()
	{
		return stringsMappedMemorySize;
	}

	public void setStringsMappedMemorySize(String stringsMappedMemorySize)
	{
		this.stringsMappedMemorySize = stringsMappedMemorySize;
	}

	public String getArraysMappedMemorySize()
	{
		return arraysMappedMemorySize;
	}

	public void setArraysMappedMemorySize(String arraysMappedMemorySize)
	{
		this.arraysMappedMemorySize = arraysMappedMemorySize;
	}

	public String getKeepLogicalLogs()
	{
		return keepLogicalLogs;
	}

	public void setKeepLogicalLogs(String keepLogicalLogs)
	{
		this.keepLogicalLogs = keepLogicalLogs;
	}

	public String getDumpConfiguration()
	{
		return dumpConfiguration;
	}

	public void setDumpConfiguration(String dumpConfiguration)
	{
		this.dumpConfiguration = dumpConfiguration;
	}

	public String getTrafficSignalPenaltyDefault()
	{
		return trafficSignalPenaltyDefault;
	}

	public void setTrafficSignalPenaltyDefault(String trafficSignalPenaltyDefault)
	{
		this.trafficSignalPenaltyDefault = trafficSignalPenaltyDefault;
	}

	public String getDeleteCsvFile()
	{
		return deleteCsvFile;
	}

	public void setDeleteCsvFile(String deleteCsvFile)
	{
		this.deleteCsvFile = deleteCsvFile;
	}

	public String getCacheType()
	{
		return cacheType;
	}

	public void setCacheType(String cacheType)
	{
		this.cacheType = cacheType;
	}
}