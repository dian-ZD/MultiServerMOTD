package io.Sriptirc_wp_989.multiservermotd.manager;

import io.Sriptirc_wp_989.multiservermotd.config.ConfigManager;
import io.Sriptirc_wp_989.multiservermotd.util.ServerPingUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ServerStatusManager {
    
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private BukkitTask updateTask;
    
    // 服务器状态缓存
    private final Map<String, ServerStatus> serverStatusMap = new ConcurrentHashMap<>();
    private int totalPlayers = 0;
    private int localPlayers = 0;
    
    public ServerStatusManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }
    
    public void start() {
        stop(); // 确保先停止之前的任务
        
        int interval = configManager.getRefreshInterval() * 20; // 转换为ticks
        
        // 立即异步更新一次，确保有初始数据（不阻塞主线程）
        updateNow();
        
        // 启动定时任务（使用异步任务，避免阻塞主线程）
        updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateAllServers, interval, interval);
        
        plugin.getLogger().info("服务器状态更新任务已启动，间隔 " + configManager.getRefreshInterval() + " 秒");
    }
    
    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }
    
    /**
     * 更新所有服务器状态
     */
    private void updateAllServers() {
        List<String> servers = configManager.getServers();
        
        // 获取本服在线人数
        // 使用callSyncMethod确保在主线程中安全执行
        if (Bukkit.isPrimaryThread()) {
            localPlayers = Bukkit.getOnlinePlayers().size();
        } else {
            try {
                localPlayers = Bukkit.getScheduler().callSyncMethod(plugin, () -> 
                    Bukkit.getOnlinePlayers().size()
                ).get();
            } catch (Exception e) {
                plugin.getLogger().warning("获取本服在线人数时出错: " + e.getMessage());
                localPlayers = 0; // 出错时使用默认值
            }
        }
        
        if (configManager.isDebug()) {
            plugin.getLogger().info("开始更新服务器状态，本服人数: " + localPlayers + ", 配置了 " + servers.size() + " 个远程服务器");
        }
        
        if (servers.isEmpty()) {
            totalPlayers = localPlayers;
            if (configManager.isDebug()) {
                plugin.getLogger().info("没有配置远程服务器，总人数 = 本服人数: " + totalPlayers);
            }
            return;
        }
        
        int newTotal = localPlayers;
        String offlineHandling = configManager.getOfflineHandling();
        
        for (String serverStr : servers) {
            try {
                // 解析服务器地址和端口
                String[] parts = serverStr.split(":");
                String address = parts[0];
                int port = 25565;
                if (parts.length > 1) {
                    try {
                        port = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("服务器端口格式错误: " + serverStr + "，使用默认端口 25565");
                    }
                }
                
                // Ping服务器
                int online = -1;
                try {
                    online = ServerPingUtil.getOnlinePlayers(address, port, 3000); // 3秒超时
                } catch (Exception e) {
                    if (configManager.isDebug()) {
                        plugin.getLogger().warning("Ping服务器 " + address + ":" + port + " 时出错: " + e.getMessage());
                        plugin.getLogger().warning("详细错误: " + e.getClass().getName() + ": " + e.getMessage());
                        // 打印堆栈跟踪的前几行
                        for (StackTraceElement element : e.getStackTrace()) {
                            if (element.getClassName().contains("multiservermotd")) {
                                plugin.getLogger().warning("    at " + element.getClassName() + "." + element.getMethodName() + 
                                                         "(" + element.getFileName() + ":" + element.getLineNumber() + ")");
                                break;
                            }
                        }
                    }
                }
                
                ServerStatus status = new ServerStatus(address, port);
                status.setLastUpdate(System.currentTimeMillis());
                
                if (online >= 0) {
                    // 服务器在线
                    status.setOnline(true);
                    status.setOnlinePlayers(online);
                    newTotal += online;
                    
                    if (configManager.isDebug()) {
                        plugin.getLogger().info("服务器 " + address + ":" + port + " 在线，人数: " + online);
                    }
                } else {
                    // 服务器离线
                    status.setOnline(false);
                    status.setOnlinePlayers(0);
                    
                    if ("zero".equals(offlineHandling)) {
                        // 视为0人在线，计入总数
                        // 不加任何人数
                        if (configManager.isDebug()) {
                            plugin.getLogger().info("服务器 " + address + ":" + port + " 离线，按0人计入总数");
                        }
                    } else if ("ignore".equals(offlineHandling)) {
                        // 忽略，不计入总数
                        // 不增加人数
                        if (configManager.isDebug()) {
                            plugin.getLogger().info("服务器 " + address + ":" + port + " 离线，被忽略");
                        }
                    } else {
                        // error处理方式：暂时按ignore处理
                        if (configManager.isDebug()) {
                            plugin.getLogger().info("服务器 " + address + ":" + port + " 离线，错误处理方式");
                        }
                    }
                }
                
                serverStatusMap.put(serverStr, status);
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "更新服务器状态时出错: " + serverStr, e);
            }
        }
        
        totalPlayers = newTotal;
        
        if (configManager.isDebug()) {
            plugin.getLogger().info("总人数更新完成: " + totalPlayers + " (本服: " + localPlayers + ")");
        }
    }
    
    /**
     * 获取总在线人数（包括本服和所有远程服务器）
     */
    public int getTotalPlayers() {
        return totalPlayers;
    }
    
    /**
     * 获取本服在线人数
     */
    public int getLocalPlayers() {
        return localPlayers;
    }
    
    /**
     * 获取服务器状态映射
     */
    public Map<String, ServerStatus> getServerStatusMap() {
        return new HashMap<>(serverStatusMap);
    }
    
    /**
     * 手动触发一次更新
     */
    public void updateNow() {
        // 使用延迟1tick确保异步执行，避免立即阻塞主线程
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this::updateAllServers, 1L);
    }
    
    /**
     * 服务器状态内部类
     */
    public static class ServerStatus {
        private final String address;
        private final int port;
        private boolean online;
        private int onlinePlayers;
        private long lastUpdate;
        
        public ServerStatus(String address, int port) {
            this.address = address;
            this.port = port;
        }
        
        public String getAddress() {
            return address;
        }
        
        public int getPort() {
            return port;
        }
        
        public boolean isOnline() {
            return online;
        }
        
        public void setOnline(boolean online) {
            this.online = online;
        }
        
        public int getOnlinePlayers() {
            return onlinePlayers;
        }
        
        public void setOnlinePlayers(int onlinePlayers) {
            this.onlinePlayers = onlinePlayers;
        }
        
        public long getLastUpdate() {
            return lastUpdate;
        }
        
        public void setLastUpdate(long lastUpdate) {
            this.lastUpdate = lastUpdate;
        }
        
        @Override
        public String toString() {
            return address + ":" + port + " - " + (online ? "在线" : "离线") + " (" + onlinePlayers + " 人)";
        }
    }
}