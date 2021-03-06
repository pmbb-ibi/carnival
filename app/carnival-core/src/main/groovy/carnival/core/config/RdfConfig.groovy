package carnival.core.config


import static java.lang.System.err

import org.yaml.snakeyaml.Yaml

import groovy.util.logging.Slf4j


/**
 * A database configuration.
 *
 */
@Slf4j
class RdfConfig extends DatabaseConfig {


	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

	static public RdfConfig getDatabaseConfigFromFile(String confFilePath, String prefix) {
        def conf = loadYamlFile(confFilePath)
        if (!conf.get('carnival.dataSourcesRdf')) throw new RuntimeException("no dataSourcesRdf configurations in: $confFilePath")
        if (!conf.carnival.dataSourcesRdf.get(prefix)) throw new RuntimeException("no configuration in $confFilePath for $prefix")
        def db = new RdfConfig(conf.carnival.dataSourcesRdf.get(prefix))
		return db
	}

    /**
     *
     *
     */
    static public Map getDatabaseConfigVals(String confFilePath, String tag) {
        log.trace "CarnivalDbConfig.getDatabaseConfigVals"

        File dbConfFile = new File(confFilePath)
        assert dbConfFile.exists()
        String dbConfigText = dbConfFile.text
        log.trace "dbConfigText: $dbConfigText"

        Map dbm = loadYaml(dbConfigText)
        log.trace "dbm: ${dbm.dataSources}"

        return dbm.dataSources.get(tag)        
    }



	///////////////////////////////////////////////////////////////////////////
	// FIELDS
	///////////////////////////////////////////////////////////////////////////
	String url
	String user
	String password
	String repository


	///////////////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	///////////////////////////////////////////////////////////////////////////
	public RdfConfig(Map m) {
		["url", "user", "password", "repository"].each { f ->
			assert m[f]
		}
		url = m.url
		user = m.user
		password = m.password
		repository = m.repository
	}


	///////////////////////////////////////////////////////////////////////////
	// METHODS
	///////////////////////////////////////////////////////////////////////////
	public Map getMap() {
		return [url: url, user: user, password: password, repository: repository]
	}
}





