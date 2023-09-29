package com.leahbi.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class DirectProducer {
    public static final String EXCHANGE_NAME = "direct-exchange";

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");

            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNext()) {
                String strings = scanner.nextLine();
                String[] split = strings.split(" ");
                if(split.length < 1){
                    continue;
                }
                String message = split[0];
                String routingKey = split[1];

                channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes(StandardCharsets.UTF_8));
                System.out.println("[x] Sent : " + message + " with routing key: " + routingKey);
            }
        }
    }
}
