package io.Sriptirc_wp_989.multiservermotd.util;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ServerPingUtil {
    
    /**
     * Ping一个Minecraft服务器并获取在线人数
     * @param address 服务器地址
     * @param port 服务器端口
     * @param timeout 超时时间（毫秒）
     * @return 在线人数，如果失败返回-1
     */
    public static int getOnlinePlayers(String address, int port, int timeout) {
        Socket socket = null;
        DataInputStream in = null;
        DataOutputStream out = null;
        
        try {
            // 建立连接
            socket = new Socket();
            socket.connect(new InetSocketAddress(address, port), timeout);
            socket.setSoTimeout(timeout);
            
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            
            // 发送握手包 (Handshake)
            // 包ID: 0x00 (Handshake)
            // 协议版本: -1 (表示请求状态)
            // 服务器地址长度 + 地址
            // 服务器端口
            // 下一步状态: 1 (表示请求状态)
            
            ByteArrayOutputStream handshakeBytes = new ByteArrayOutputStream();
            DataOutputStream handshakeOut = new DataOutputStream(handshakeBytes);
            
            // 包ID: 0x00 (使用VarInt编码)
            writeVarInt(handshakeOut, 0x00);
            
            // 协议版本: -1 (使用VarInt编码)
            writeVarInt(handshakeOut, -1);
            
            // 服务器地址
            byte[] addressBytes = address.getBytes(StandardCharsets.UTF_8);
            writeVarInt(handshakeOut, addressBytes.length);
            handshakeOut.write(addressBytes);
            
            // 服务器端口 (无符号short)
            handshakeOut.writeShort(port);
            
            // 下一步状态: 1 (状态)
            writeVarInt(handshakeOut, 1);
            
            // 发送握手包
            byte[] handshakePacket = handshakeBytes.toByteArray();
            writeVarInt(out, handshakePacket.length);
            out.write(handshakePacket);
            
            // 发送状态请求包 (Request)
            // 包长度: 1 (包ID的长度)
            // 包ID: 0x00 (Request)
            writeVarInt(out, 1); // 包长度
            writeVarInt(out, 0x00); // 包ID
            
            // 接收状态响应
            int length = readVarInt(in);
            int packetId = readVarInt(in);
            
            if (packetId != 0x00) {
                throw new IOException("无效的响应包ID: " + packetId);
            }
            
            // 读取JSON响应长度
            int jsonLength = readVarInt(in);
            if (jsonLength < 0) {
                throw new IOException("无效的JSON长度: " + jsonLength);
            }
            
            // 读取JSON响应
            byte[] jsonData = new byte[jsonLength];
            in.readFully(jsonData);
            String jsonResponse = new String(jsonData, StandardCharsets.UTF_8);
            
            // 调试日志：记录接收到的JSON响应
            if (jsonResponse != null && !jsonResponse.isEmpty()) {
                System.out.println("[ServerPingUtil DEBUG] 接收到JSON响应: " + 
                    (jsonResponse.length() > 200 ? jsonResponse.substring(0, 200) + "..." : jsonResponse));
            }
            
            // 解析JSON获取在线人数
            // 简单解析：查找"players":{"online":的数字
            return parseOnlinePlayersFromJson(jsonResponse);
            
        } catch (Exception e) {
            // 失败时抛出异常，包含更多错误信息
            throw new RuntimeException("无法ping服务器 " + address + ":" + port + ": " + e.getMessage(), e);
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                // 忽略关闭异常
            }
        }
    }
    
    /**
     * 从JSON响应中解析在线人数
     */
    private static int parseOnlinePlayersFromJson(String json) throws IOException {
        // 使用更健壮的解析方法
        // 支持多种字段名：layers, players, Players等
        
        // 调试：记录完整的JSON响应
        System.out.println("[ServerPingUtil DEBUG] 完整JSON响应: " + json);
        
        // 尝试查找可能的字段名变体
        String[] possibleFieldNames = {"\"layers\":{", "\"layers\": {", "\"players\":{", "\"players\": {"};
        int fieldIndex = -1;
        String foundFieldName = null;
        
        for (String fieldName : possibleFieldNames) {
            int index = json.indexOf(fieldName);
            if (index != -1) {
                fieldIndex = index;
                foundFieldName = fieldName;
                break;
            }
        }
        
        // 如果没有找到标准格式，尝试更宽松的查找
        if (fieldIndex == -1) {
            // 尝试查找包含layers或players的字段
            int layersIndex = json.indexOf("\"layers\"");
            int playersIndex = json.indexOf("\"players\"");
            
            if (layersIndex != -1) {
                fieldIndex = layersIndex;
                foundFieldName = "\"layers\"";
            } else if (playersIndex != -1) {
                fieldIndex = playersIndex;
                foundFieldName = "\"players\"";
            }
        }
        
        if (fieldIndex == -1) {
            throw new IOException("JSON响应中未找到'layers'或'players'字段。完整响应: " + 
                (json.length() > 300 ? json.substring(0, 300) + "..." : json));
        }
        
        // 在字段对象中查找"online":
        int onlineIndex = json.indexOf("\"online\":", fieldIndex);
        if (onlineIndex == -1) {
            throw new IOException("JSON响应中未找到'online'字段。" + foundFieldName + "部分: " + 
                json.substring(fieldIndex, Math.min(fieldIndex + 100, json.length())));
        }
        
        // 详细的调试信息
        System.out.println("[ServerPingUtil DEBUG] 找到onlineIndex: " + onlineIndex);
        System.out.println("[ServerPingUtil DEBUG] online字段周围内容: '" + 
            json.substring(Math.max(0, onlineIndex - 10), Math.min(json.length(), onlineIndex + 30)) + "'");
        
        // 跳过"online":（9个字符："o(1)n(2)l(3)i(4)n(5)e(6)"(7):(8)）
        int valueStart = onlineIndex + 9; // "\"online\":".length() = 9个字符
        System.out.println("[ServerPingUtil DEBUG] valueStart位置: " + valueStart + ", 字符串长度: " + json.length());
        
        // 确保valueStart在范围内
        if (valueStart >= json.length()) {
            throw new IOException("valueStart超出字符串范围。onlineIndex: " + onlineIndex + ", 字符串长度: " + json.length());
        }
        
        // 查找数字的开始位置（跳过空格和可能的引号）
        while (valueStart < json.length() && 
               (Character.isWhitespace(json.charAt(valueStart)) || json.charAt(valueStart) == '\"')) {
            valueStart++;
        }
        
        System.out.println("[ServerPingUtil DEBUG] 跳过空格后valueStart: " + valueStart);
        System.out.println("[ServerPingUtil DEBUG] 当前位置字符: '" + json.charAt(valueStart) + "' (ASCII: " + (int)json.charAt(valueStart) + ")");
        System.out.println("[ServerPingUtil DEBUG] 当前位置字符是数字吗? " + Character.isDigit(json.charAt(valueStart)));
        
        // 查找数字的结束位置（逗号、}或非数字字符）
        int valueEnd = valueStart;
        while (valueEnd < json.length() && Character.isDigit(json.charAt(valueEnd))) {
            valueEnd++;
        }
        
        System.out.println("[ServerPingUtil DEBUG] valueEnd: " + valueEnd + ", valueEnd == valueStart? " + (valueEnd == valueStart));
        
        if (valueEnd == valueStart) {
            System.out.println("[ServerPingUtil DEBUG] 未找到数字，检查附近字符:");
            for (int i = Math.max(0, valueStart - 5); i < Math.min(json.length(), valueStart + 5); i++) {
                char c = json.charAt(i);
                System.out.println("[ServerPingUtil DEBUG]  位置 " + i + ": '" + c + "' (ASCII: " + (int)c + "), 是数字? " + Character.isDigit(c));
            }
            
            // 尝试查找可能的负数或浮点数（虽然不太可能）
            if (valueStart < json.length() && json.charAt(valueStart) == '-') {
                valueEnd = valueStart + 1;
                while (valueEnd < json.length() && Character.isDigit(json.charAt(valueEnd))) {
                    valueEnd++;
                }
            }
            
            if (valueEnd == valueStart) {
                throw new IOException("未找到在线人数数字。online字段周围内容: '" + 
                    json.substring(Math.max(0, onlineIndex - 20), Math.min(json.length(), valueStart + 20)) + "'");
            }
        }
        
        String onlineStr = json.substring(valueStart, valueEnd);
        System.out.println("[ServerPingUtil DEBUG] 提取的数字字符串: '" + onlineStr + "'");
        
        try {
            int result = Integer.parseInt(onlineStr);
            System.out.println("[ServerPingUtil DEBUG] 成功解析为数字: " + result);
            return result;
        } catch (NumberFormatException e) {
            throw new IOException("无法解析在线人数: '" + onlineStr + "'。周围内容: '" + 
                json.substring(Math.max(0, valueStart - 10), Math.min(json.length(), valueEnd + 10)) + "'");
        }
    }
    
    /**
     * 写入VarInt
     */
    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while (true) {
            if ((value & ~0x7F) == 0) {
                out.writeByte(value);
                return;
            }
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
    }
    
    /**
     * 读取VarInt
     */
    private static int readVarInt(DataInputStream in) throws IOException {
        int value = 0;
        int length = 0;
        byte currentByte;
        
        while (true) {
            currentByte = in.readByte();
            value |= (currentByte & 0x7F) << (length * 7);
            length++;
            
            if (length > 5) {
                throw new IOException("VarInt太大");
            }
            
            if ((currentByte & 0x80) != 0x80) {
                break;
            }
        }
        
        return value;
    }
}