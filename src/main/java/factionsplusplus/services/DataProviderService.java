package factionsplusplus.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.io.File;
import java.io.PrintWriter;
import java.sql.SQLException;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.codec.CodecFactory;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.gson2.Gson2Plugin;
import org.jdbi.v3.guava.GuavaPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import factionsplusplus.data.codecs.UUIDCodec;

import java.util.UUID;

@Singleton
public class DataProviderService {
    private final ConfigService configService;
    private final Jdbi persistentData;
    private final String dataPath;
    private HikariDataSource persistentDataSource;

    @Inject
    public DataProviderService(@Named("dataFolder") String dataPath, ConfigService configService) throws SQLException {
        this.configService = configService;
        this.dataPath = dataPath;
        this.persistentData = this.initializePersistentData();
    }

    public Jdbi initializePersistentData() throws SQLException {
        HikariConfig configuration = new HikariConfig();
        if (! this.configService.getBoolean("system.database.flatfile")) {
            final String hostname = this.configService.getString("system.database.host");
            String port = this.configService.getString("system.database.port");
            if (port != null && port.length() > 0) port = ":"+port;
            else port = "";
            final String name = this.configService.getString("system.database.name");
            configuration.setDataSourceClassName("org.mariadb.jdbc.MariaDbDataSource");
            configuration.addDataSourceProperty("url", String.format("jdbc:mariadb://%s%s/%s?useServerPrepStmts=true", hostname, port, name));
            configuration.addDataSourceProperty("user", this.configService.getString("system.database.username"));
            configuration.addDataSourceProperty("password", this.configService.getString("system.database.password"));
        } else {
            final String fileName = this.configService.getString("system.database.name");
            final File path = new File(this.dataPath, fileName);
            configuration.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
            configuration.addDataSourceProperty("url", String.format("jdbc:h2:file:%s;MODE=MariaDB;AUTO_SERVER=TRUE;DATABASE_TO_LOWER=TRUE;INIT=CREATE SCHEMA IF NOT EXISTS factions\\;SET SCHEMA factions", path.toString()));
        }
        this.persistentDataSource = new HikariDataSource(configuration);
        this.persistentDataSource.setLogWriter(new PrintWriter(System.out));
        Jdbi persistentData = Jdbi.create(this.persistentDataSource)
            .installPlugin(new SqlObjectPlugin())
            .installPlugin(new GuavaPlugin())
            .installPlugin(new Gson2Plugin());
        persistentData.registerCodecFactory(CodecFactory.forSingleCodec(QualifiedType.of(UUID.class), new UUIDCodec()));
        return persistentData;
    }

    public void onDisable() {
        this.persistentDataSource.close();
    }

    public Jdbi getPersistentData() {
        return this.persistentData;
    }
}
