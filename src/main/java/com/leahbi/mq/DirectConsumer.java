package com.leahbi.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;

public class DirectConsumer {
    public static final String EXCHANGE_NAME = "direct-exchange";

    public static void main(String[] args) throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel1 = connection.createChannel();
        Channel channel2 = connection.createChannel();

        channel1.exchangeDeclare(EXCHANGE_NAME, "direct");

        String queueName1 = "leah_queue";
        channel1.queueDeclare(queueName1, false, false, false, null);
        channel1.queueBind(queueName1,EXCHANGE_NAME,"leah");

        String queueName2 = "liu_queue";
        channel2.queueDeclare(queueName2, false, false, false, null);
        channel2.queueBind(queueName2,EXCHANGE_NAME,"liu");

        DeliverCallback deliverCallback1 = (consumerTag, deliver) ->{
            String message = new String(deliver.getBody(), StandardCharsets.UTF_8);
            System.out.println("[leah] Receive "+ deliver.getEnvelope().getRoutingKey() + ": "+message);
        };

        DeliverCallback deliverCallback2 = (consumerTag, deliver) ->{
            String message = new String(deliver.getBody(), StandardCharsets.UTF_8);
            System.out.println("[liu] Receive "+ deliver.getEnvelope().getRoutingKey() + ": "+message);
        };

        channel1.basicConsume(queueName1,true,deliverCallback1, consumerTag -> {});
        channel2.basicConsume(queueName2,true,deliverCallback2, consumerTag -> {});
    }
}
