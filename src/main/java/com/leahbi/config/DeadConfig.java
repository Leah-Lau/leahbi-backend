package com.leahbi.config;

import com.leahbi.constant.BiConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class DeadConfig {
    /**
     * 正常BI交换机
     * @return
     */
    @Bean
    DirectExchange biExchange(){
        return new DirectExchange(BiConstant.BI_EXCHANGE_NAME, false, false);
    }

    /**
     * 设置TTL队列
     * @return
     */
    @Bean
    public Queue biTTLQueue(){
        Map<String, Object> args = new HashMap<>();
        // 设置队列超时时间60s
        args.put("x-message-ttl", 60 * 1000);
        args.put("x-dead-letter-exchange", BiConstant.BI_DLX_EXCHANGE_NAME);
        args.put("x-dead-letter-routing-key", BiConstant.BI_DLX_ROUTING_KEY);
        System.out.println("hi");
        return new Queue(BiConstant.BI_QUEUE_NAME, false, false,false,args);
    }

    /**
     * 绑定routingKey
     * @return
     */
    @Bean
    public Binding biTTLRouteBinding(){
        return BindingBuilder.bind(biTTLQueue()).to(biExchange()).with(BiConstant.BI_ROUTING_KEY);
    }

    /**
     * 配置死信交换机
     * @return
     */
    @Bean
    public DirectExchange deadExchange(){
        return new DirectExchange(BiConstant.BI_DLX_EXCHANGE_NAME, false, false);
    }

    /**
     * 配置死信队列
     * @return
     */
    @Bean
    public Queue deadQueue(){
        return new Queue(BiConstant.BI_DLX_QUEUE_NAME, false, false, false, null);
    }

    /**
     * 死信队列绑定routingKey
     * @return
     */
    @Bean
    public Binding deadRouteBinding(){
        return BindingBuilder.bind(deadQueue()).to(deadExchange()).with(BiConstant.BI_DLX_ROUTING_KEY);
    }



}
