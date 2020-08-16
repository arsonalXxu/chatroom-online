package org.arsonal.chatroom;

import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Server {
    private static AtomicInteger COUNTER = new AtomicInteger(0);
    private final ServerSocket server;
    private Map<Integer, ClientConnection> clients = new ConcurrentHashMap<>();

    // TCP连接的端口号0~65535
    public Server(int port) throws IOException {
        this.server = new ServerSocket(port);
    }

    public void start() throws IOException {
        while (true) {
            Socket socket = server.accept();
            // 开启线程处理数据的读取，因为数据读取比较耗时
            new ClientConnection(COUNTER.incrementAndGet(), socket, this).start();
        }
    }

    public static void main(String[] args) {
        try {
            new Server(8080).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 注册连接的client
    public void registerClient(ClientConnection clientConnection) {

        clients.put(clientConnection.getClientId(), clientConnection);
        this.clientOnline(clientConnection);
    }

    public void dispatchMessage(ClientConnection client, String src, String target, String message) {
        try {
            client.sendMessage(src + "对" + target + "说" + "：" + message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 发送消息
    public void sendMessage(ClientConnection src, Message message) {
        if (message.getId() == 0) {
            clients.values().forEach(client -> dispatchMessage(client, src.getClientName(), "所有人", message.getMessage()));
        } else {
            int targetUser = message.getId();
            ClientConnection target = clients.get(targetUser);
            if (target == null) {
                System.err.println(targetUser + "用户不存在！");
            } else {
                dispatchMessage(target, src.getClientName(), "你", message.getMessage());
            }
        }
    }

    public void clientOffline(ClientConnection clientConnection) {
        clients.remove(clientConnection.getClientId());
        clients.values().forEach(client ->
                dispatchMessage(client, "系统", "所有人", clientConnection.getClientName() + "下线了！" + getAllClientInfo()));
    }

    public void clientOnline(ClientConnection clientConnection) {
        clients.values().forEach(client ->
                dispatchMessage(client, "系统", "所有人", clientConnection.getClientName() + "上线了！" + getAllClientInfo()));
    }

    public String getAllClientInfo() {
        return clients.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue().getClientName())
                .collect(Collectors.joining(","));
    }
}

class ClientConnection extends Thread {
    private Socket socket;
    private int clientId;
    private String clientName;
    private Server server;

    public ClientConnection(int clientId, Socket socket, Server server) {
        this.clientId = clientId;
        this.socket = socket;
        this.server = server;
    }

    public int getClientId() {
        return clientId;
    }

    public String getClientName() {
        return clientName;
    }

    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (isNotOnlineYet()) {
                    clientName = line;
                    server.registerClient(this);
                } else {
                    Message message = JSON.parseObject(line, Message.class);
                    server.sendMessage(this, message);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            // client端下线
            server.clientOffline(this);
        }
    }

    public boolean isNotOnlineYet() {
        return clientName == null;
    }

    public void sendMessage(String str) throws IOException {
        Utils.writeMessage(socket, str);
    }
}

class Message {
    private Integer id;
    private String message;

    public Message() {
    }

    public Message(Integer id, String message) {
        this.id = id;
        this.message = message;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
