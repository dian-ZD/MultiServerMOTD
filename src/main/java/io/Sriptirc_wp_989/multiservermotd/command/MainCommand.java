package io.Sriptirc_wp_989.multiservermotd.command;

import io.Sriptirc_wp_989.multiservermotd.config.ConfigManager;
import io.Sriptirc_wp_989.multiservermotd.manager.ServerStatusManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MainCommand implements CommandExecutor, TabCompleter {
    
    private final ConfigManager configManager;
    private final ServerStatusManager statusManager;
    
    public MainCommand(ConfigManager configManager, ServerStatusManager statusManager) {
        this.configManager = configManager;
        this.statusManager = statusManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("multiservers")) {
            return handleStatusCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("multiserversreload")) {
            return handleReloadCommand(sender);
        }
        return false;
    }
    
    private boolean handleStatusCommand(CommandSender sender, String[] args) {
        // 检查权限
        if (!sender.hasPermission("multiservers.use")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令。");
            return true;
        }
        
        // 获取服务器状态
        int totalPlayers = statusManager.getTotalPlayers();
        int localPlayers = statusManager.getLocalPlayers();
        Map<String, ServerStatusManager.ServerStatus> statusMap = statusManager.getServerStatusMap();
        
        sender.sendMessage(ChatColor.GOLD + "========== " + ChatColor.GREEN + "多服务器状态" + ChatColor.GOLD + " ==========");
        sender.sendMessage(ChatColor.YELLOW + "本服务器在线人数: " + ChatColor.GREEN + localPlayers);
        sender.sendMessage(ChatColor.YELLOW + "所有服务器总人数: " + ChatColor.GREEN + totalPlayers);
        
        if (!statusMap.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "远程服务器状态:");
            for (Map.Entry<String, ServerStatusManager.ServerStatus> entry : statusMap.entrySet()) {
                ServerStatusManager.ServerStatus status = entry.getValue();
                ChatColor color = status.isOnline() ? ChatColor.GREEN : ChatColor.RED;
                String statusText = status.isOnline() ? "在线" : "离线";
                sender.sendMessage(ChatColor.GRAY + "  " + entry.getKey() + " - " + 
                                 color + statusText + ChatColor.GRAY + " (" + 
                                 color + status.getOnlinePlayers() + " 人" + ChatColor.GRAY + ")");
            }
        } else {
            sender.sendMessage(ChatColor.YELLOW + "远程服务器: " + ChatColor.GRAY + "未配置或未更新");
        }
        
        // 显示当前MOTD配置
        sender.sendMessage(ChatColor.YELLOW + "当前MOTD配置:");
        sender.sendMessage(ChatColor.GRAY + "  第一行: " + ChatColor.WHITE + 
                          ChatColor.translateAlternateColorCodes('&', configManager.getFirstLine()));
        sender.sendMessage(ChatColor.GRAY + "  第二行: " + ChatColor.WHITE + 
                          ChatColor.translateAlternateColorCodes('&', configManager.getSecondLine()));
        
        sender.sendMessage(ChatColor.GOLD + "====================================");
        
        // 如果包含update参数，手动触发更新
        if (args.length > 0 && args[0].equalsIgnoreCase("update")) {
            sender.sendMessage(ChatColor.GRAY + "正在手动更新服务器状态...");
            statusManager.updateNow();
            sender.sendMessage(ChatColor.GREEN + "服务器状态更新已触发！");
        }
        
        return true;
    }
    
    private boolean handleReloadCommand(CommandSender sender) {
        // 检查权限
        if (!sender.hasPermission("multiservers.reload")) {
            sender.sendMessage(ChatColor.RED + "你没有权限重载插件配置。");
            return true;
        }
        
        sender.sendMessage(ChatColor.GRAY + "正在重载多服务器MOTD插件配置...");
        
        try {
            // 重载配置
            configManager.reloadConfig();
            
            // 重启状态更新任务
            statusManager.stop();
            statusManager.start();
            
            sender.sendMessage(ChatColor.GREEN + "配置重载成功！");
            sender.sendMessage(ChatColor.GRAY + "已重新加载 " + configManager.getServers().size() + " 个服务器配置。");
            
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "配置重载失败: " + e.getMessage());
            configManager.getLogger().severe("重载配置时出错: " + e.getMessage());
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (command.getName().equalsIgnoreCase("multiservers")) {
            if (args.length == 1) {
                // 补全子命令
                if (sender.hasPermission("multiservers.use")) {
                    completions.add("update");
                }
            }
        }
        // multiserversreload 没有参数需要补全
        
        return completions;
    }
}