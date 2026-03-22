package io.Sriptirc_wp_989.multiservermotd;

import io.Sriptirc_wp_989.multiservermotd.command.MainCommand;
import io.Sriptirc_wp_989.multiservermotd.config.ConfigManager;
import io.Sriptirc_wp_989.multiservermotd.listener.MOTDListener;
import io.Sriptirc_wp_989.multiservermotd.manager.ServerStatusManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Multiservermotd extends JavaPlugin {
    
    private ConfigManager configManager;
    private ServerStatusManager statusManager;
    private MOTDListener motdListener;
    
    @Override
    public void onEnable() {
        try {
            getLogger().info("正在启用多服务器MOTD插件...");
            
            // 保存默认配置文件
            saveDefaultConfig();
            getLogger().info("默认配置文件已保存");
            
            // 初始化配置管理器
            configManager = new ConfigManager(this);
            getLogger().info("配置管理器已初始化");
            
            // 初始化服务器状态管理器
            statusManager = new ServerStatusManager(this, configManager);
            getLogger().info("服务器状态管理器已初始化");
            
            // 初始化MOTD监听器
            motdListener = new MOTDListener(configManager, statusManager);
            getLogger().info("MOTD监听器已初始化");
            
            // 注册事件监听器
            getServer().getPluginManager().registerEvents(motdListener, this);
            getLogger().info("事件监听器已注册");
            
            // 注册命令
            MainCommand mainCommand = new MainCommand(configManager, statusManager);
            getCommand("multiservers").setExecutor(mainCommand);
            getCommand("multiservers").setTabCompleter(mainCommand);
            getCommand("multiserversreload").setExecutor(mainCommand);
            getCommand("multiserversreload").setTabCompleter(mainCommand);
            getLogger().info("命令已注册");
            
            // 启动服务器状态更新任务
            statusManager.start();
            getLogger().info("服务器状态更新任务已启动");
            
            getLogger().info("多服务器MOTD插件已启用！");
            getLogger().info("配置了 " + configManager.getServers().size() + " 个远程服务器");
            getLogger().info("MOTD第一行: " + configManager.getFirstLine());
            getLogger().info("MOTD第二行: " + configManager.getSecondLine());
            
        } catch (Exception e) {
            getLogger().severe("启用插件时发生严重错误！");
            getLogger().severe("错误信息: " + e.getMessage());
            getLogger().severe("堆栈跟踪:");
            e.printStackTrace();
            // 禁用插件，避免进一步错误
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        // 停止更新任务
        if (statusManager != null) {
            statusManager.stop();
        }
        
        getLogger().info("多服务器MOTD插件已禁用！");
    }
    
    /**
     * 获取配置管理器实例
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * 获取服务器状态管理器实例
     */
    public ServerStatusManager getStatusManager() {
        return statusManager;
    }
}
