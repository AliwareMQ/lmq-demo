package com.aliyun.openservices.lmq.example;

import com.aliyun.openservices.ons.api.*;

import java.util.Properties;

/**
 * Created by alvin on 17-7-24.
 * This is simple example for mq java client recv mqtt msg
 */
public class MqRecvMqttDemo {
    public static void main(String[] args) throws Exception {
        Properties properties = Tools.loadProperties();
        final String consumerId = properties.getProperty("consumerId");
        final String topic = properties.getProperty("topic");
        String accessKey = properties.getProperty("accessKey");
        String secretKey = properties.getProperty("secretKey");
        Properties initProperties = new Properties();
        initProperties.put(PropertyKeyConst.ConsumerId, consumerId);
        initProperties.put(PropertyKeyConst.AccessKey, accessKey);
        initProperties.put(PropertyKeyConst.SecretKey, secretKey);
        Consumer consumer = ONSFactory.createConsumer(initProperties);
        //mq client recv mqtt msg ,just subscribe the parent topic
        consumer.subscribe(topic, "*", new MessageListener() {
            public Action consume(Message message, ConsumeContext consumeContext) {
                System.out.println("recv msg:" + message);
                return Action.CommitMessage;
            }
        });
        consumer.start();
        Thread.sleep(Integer.MAX_VALUE);
        consumer.shutdown();
        System.exit(0);
    }

}
