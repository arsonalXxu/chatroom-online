package org.arsonal.chatroom;

import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws IOException {
        System.out.println("请输入你的昵称：");
        Scanner scanner = new Scanner(System.in);
        String name = scanner.nextLine();

        Socket socket = new Socket("127.0.0.1", 8080);
        Utils.writeMessage(socket, name);

        System.out.println("连接成功！");

        new Thread(()-> readFromServer(socket)).start();

        while (true) {
            System.out.println("输入你要发送的聊天消息；");
            System.out.println("消息格式为 id:message, 例如：1:hello 表示向用户1发送消息hello");
            System.out.println("id=0表示向所有在线用户发送消息, 例如：0:hello 表示向所有在线用户发送消息hello");
            String line = scanner.nextLine();

            if (!line.contains(":")) {
                System.err.println("输入的格式不对！");
            } else {
                int index = line.indexOf(":");
                int id = Integer.parseInt(line.substring(0, index));
                String message = line.substring(index + 1);

                String json = JSON.toJSONString(new Message(id, message));
                Utils.writeMessage(socket, json);
            }
        }
    }

    private static void readFromServer(Socket socket) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                System.out.println(line);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
