package com.leahbi.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class SingleProducer {
    public static final String QUEUE_NAME = "hello";

    public static void main(String[] args) throws Exception {
        // 创建连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        // 建立连接
        try(Connection connection = factory.newConnection();
            // 创建Channel（程序与rabbitmq连接的信道）
            Channel channel = connection.createChannel()){
            // 声明消息队列
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            // 发送消息
            String message = "Hello, world!";
            channel.basicPublish("",QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
            System.out.println("[x]Sent '" + message + "'");
        }
    }
}
