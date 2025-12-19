package me.koopa.ultraspawners.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.koopa.ultraspawners.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private HikariDataSource dataSource;

    public DatabaseManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void initialize() throws Exception {
        String type = configManager.getStorageType().toUpperCase();
        
        if ("MYSQL".equals(type)) {
            initializeMysql();
        } else {
            initializeSqlite();
        }
        
        createTables();
    }

    private void initializeSqlite() throws Exception {
        String filePath = configManager.getSqliteFilePath();
        File file = new File(filePath);
        file.getParentFile().mkdirs();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + filePath);
        config.setMaximumPoolSize(5);
        config.setConnectionTimeout(5000);
        
        dataSource = new HikariDataSource(config);
    }

    private void initializeMysql() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=%b&serverTimezone=UTC",
            configManager.getMysqlHost(),
            configManager.getMysqlPort(),
            configManager.getMysqlDatabase(),
            configManager.isMysqlUseSSL()
        ));
        config.setUsername(configManager.getMysqlUsername());
        config.setPassword(configManager.getMysqlPassword());
        config.setMaximumPoolSize(configManager.getMysqlMaxPoolSize());
        config.setConnectionTimeout(10000);
        
        dataSource = new HikariDataSource(config);
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            String type = configManager.getStorageType().toUpperCase();
            String createTable;
            
            if ("MYSQL".equals(type)) {
                createTable = "CREATE TABLE IF NOT EXISTS ultraspawners_spawners (" +
                    "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
                    "world TEXT NOT NULL," +
                    "x INT NOT NULL," +
                    "y INT NOT NULL," +
                    "z INT NOT NULL," +
                    "type TEXT NOT NULL," +
                    "stack INT NOT NULL DEFAULT 1," +
                    "tier INT NOT NULL DEFAULT 0," +
                    "owner TEXT," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "UNIQUE KEY unique_location (world(255), x, y, z)" +
                    ")";
            } else {
                createTable = "CREATE TABLE IF NOT EXISTS ultraspawners_spawners (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "world TEXT NOT NULL," +
                    "x INTEGER NOT NULL," +
                    "y INTEGER NOT NULL," +
                    "z INTEGER NOT NULL," +
                    "type TEXT NOT NULL," +
                    "stack INTEGER NOT NULL DEFAULT 1," +
                    "tier INTEGER NOT NULL DEFAULT 0," +
                    "owner TEXT," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "UNIQUE(world, x, y, z)" +
                    ")";
            }
            
            stmt.execute(createTable);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public void saveSpawner(StoredSpawner spawner) throws SQLException {
        String type = configManager.getStorageType().toUpperCase();
        String sql;
        
        if ("MYSQL".equals(type)) {
            sql = "INSERT INTO ultraspawners_spawners (world, x, y, z, type, stack, tier, owner) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE type = VALUES(type), stack = VALUES(stack), " +
                "tier = VALUES(tier), owner = VALUES(owner)";
        } else {
            sql = "INSERT INTO ultraspawners_spawners (world, x, y, z, type, stack, tier, owner) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(world, x, y, z) DO UPDATE SET " +
                "type = excluded.type, stack = excluded.stack, tier = excluded.tier, owner = excluded.owner";
        }
        
        try (Connection conn = getConnection();
             var pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, spawner.world);
            pstmt.setInt(2, spawner.x);
            pstmt.setInt(3, spawner.y);
            pstmt.setInt(4, spawner.z);
            pstmt.setString(5, spawner.type);
            pstmt.setInt(6, spawner.stack);
            pstmt.setInt(7, spawner.tier);
            pstmt.setString(8, spawner.owner);
            
            pstmt.executeUpdate();
        }
    }

    public StoredSpawner getSpawner(String world, int x, int y, int z) throws SQLException {
        String sql = "SELECT type, stack, tier, owner FROM ultraspawners_spawners " +
            "WHERE world = ? AND x = ? AND y = ? AND z = ?";
        
        try (Connection conn = getConnection();
             var pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, world);
            pstmt.setInt(2, x);
            pstmt.setInt(3, y);
            pstmt.setInt(4, z);
            
            var rs = pstmt.executeQuery();
            if (rs.next()) {
                return new StoredSpawner(world, x, y, z, rs.getString("type"), 
                    rs.getInt("stack"), rs.getInt("tier"), rs.getString("owner"));
            }
        }
        return null;
    }

    public void deleteSpawner(String world, int x, int y, int z) throws SQLException {
        String sql = "DELETE FROM ultraspawners_spawners WHERE world = ? AND x = ? AND y = ? AND z = ?";
        
        try (Connection conn = getConnection();
             var pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, world);
            pstmt.setInt(2, x);
            pstmt.setInt(3, y);
            pstmt.setInt(4, z);
            
            pstmt.executeUpdate();
        }
    }

    public static class StoredSpawner {
        public final String world;
        public final int x, y, z;
        public final String type;
        public final int stack;
        public final int tier;
        public final String owner;

        public StoredSpawner(String world, int x, int y, int z, String type, int stack, int tier, String owner) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.type = type;
            this.stack = stack;
            this.tier = tier;
            this.owner = owner;
        }
    }
}
