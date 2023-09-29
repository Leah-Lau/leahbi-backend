package com.leahbi.bizmq;

import com.leahbi.constant.BiConstant;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.HashMap;
import java.util.Map;

public class MqInitMain {
    public static void main(String[] args){
        try{
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            // 声明Bi交换机
            channel.exchangeDeclare(BiConstant.BI_EXCHANGE_NAME, "direct");
            // 声明死信交换机
            channel.exchangeDeclare(BiConstant.BI_DLX_EXCHANGE_NAME, "direct");
            // 绑定队列
            Map<String, Object> arg = new HashMap<>();
            arg.put("x-dead-letter-exchange", BiConstant.BI_DLX_EXCHANGE_NAME);
            arg.put("x-dead-letter-routingKey",BiConstant.BI_ROUTING_KEY);
            channel.queueDeclare(BiConstant.BI_QUEUE_NAME, false, false, false, arg);
            // 声明死信队列
            channel.queueDeclare(BiConstant.BI_DLX_QUEUE_NAME, false, false, false, null);
            channel.queueBind(BiConstant.BI_QUEUE_NAME, BiConstant.BI_EXCHANGE_NAME, BiConstant.BI_ROUTING_KEY);
            channel.queueBind(BiConstant.BI_DLX_QUEUE_NAME, BiConstant.BI_DLX_EXCHANGE_NAME, BiConstant.BI_ROUTING_KEY);
        }catch (Exception e){

        }
    }
}
