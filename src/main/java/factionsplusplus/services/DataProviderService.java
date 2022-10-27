package factionsplusplus.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.PrintWriter;
import java.sql.SQLException;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.codec.CodecFactory;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import factionsplusplus.codecs.UUIDCodec;
import java.util.UUID;

@Singleton
public class DataProviderService {
    private final ConfigService configService;
    private final Jdbi persistentData;

    @Inject
    public DataProviderService(ConfigService configService) throws SQLException {
        this.configService = configService;
        this.persistentData = this.initializePersistentData();
    }

    public Jdbi initializePersistentData() throws SQLException {
        HikariConfig configuration = new HikariConfig();
        configuration.setDataSourceClassName("org.mariadb.jdbc.MariaDbDataSource");
        configuration.addDataSourceProperty("url", "jdbc:mariadb://127.0.0.1/factionsplusplus?useServerPrepStmts=true");
        configuration.addDataSourceProperty("user", "root");
        HikariDataSource dataSource = new HikariDataSource(configuration);
        dataSource.setLogWriter(new PrintWriter(System.out));
        Jdbi persistentData = Jdbi.create(dataSource).installPlugin(new SqlObjectPlugin());
        persistentData.registerCodecFactory(CodecFactory.forSingleCodec(QualifiedType.of(UUID.class), new UUIDCodec()));
        return persistentData;
    }

    public Jdbi getPersistentData() {
        return this.persistentData;
    }
}
