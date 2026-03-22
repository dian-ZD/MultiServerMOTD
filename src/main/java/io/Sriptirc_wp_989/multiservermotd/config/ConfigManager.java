package io.Sriptirc_wp_989.multiservermotd.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

public class ConfigManager {
    
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;
    
    // 配置项
    private String firstLine;
    private String secondLine;
    private List<String> servers;
    private int refreshInterval;
    private String offlineHandling;
    private boolean debug;
    
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public void loadConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }
        
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // 设置默认值
        config.addDefault("first-line", "&a欢迎来到我们的服务器网络！");
        config.addDefault("second-line", "&e总在线人数：&6%total%");
        config.addDefault("servers", java.util.Arrays.asList("server2.example.com:25565", "server3.example.com:25566"));
        config.addDefault("refresh-interval", 30);
        config.addDefault("offline-handling", "ignore");
        config.addDefault("debug", false);
        
        config.options().copyDefaults(true);
        saveConfig();
        
        // 读取配置
        firstLine = config.getString("first-line", "&a欢迎来到我们的服务器网络！");
        secondLine = config.getString("second-line", "&e总在线人数：&6%total%");
        servers = config.getStringList("servers");
        refreshInterval = config.getInt("refresh-interval", 30);
        offlineHandling = config.getString("offline-handling", "ignore");
        debug = config.getBoolean("debug", false);
        
        // 验证配置
        if (refreshInterval < 5) {
            refreshInterval = 5;
            plugin.getLogger().warning("刷新间隔太短，已自动设置为5秒");
        }
        
        plugin.getLogger().info("配置文件已加载，共配置了 " + servers.size() + " 个服务器");
    }
    
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "保存配置文件时出错", e);
        }
    }
    
    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        loadConfig();
    }
    
    // Getter方法
    public String getFirstLine() {
        return firstLine;
    }
    
    public String getSecondLine() {
        return secondLine;
    }
    
    public List<String> getServers() {
        return servers;
    }
    
    public int getRefreshInterval() {
        return refreshInterval;
    }
    
    public String getOfflineHandling() {
        return offlineHandling;
    }
    
    public boolean isDebug() {
        return debug;
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    /**
     * 获取插件Logger
     */
    public java.util.logging.Logger getLogger() {
        return plugin.getLogger();
    }
    
    /**
     * 获取插件实例
     */
    public JavaPlugin getPlugin() {
        return plugin;
    }
}