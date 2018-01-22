package com.aliyun.openservices.lmq.example;

import com.aliyun.openservices.ons.api.*;

import java.util.Properties;

/**
 * Created by alvin on 17-7-24.
 * This is simple example for mq java client send mqtt msg
 */
public class MqSendMqttDemo {
    public static void main(String[] args) throws Exception {
        Properties properties = Tools.loadProperties();
        final String groupId = properties.getProperty("groupId");
        final String producerId = properties.getProperty("producerId");
        final String topic = properties.getProperty("topic");
        final int qosLevel = Integer.parseInt(properties.getProperty("qos"));
        final Boolean cleanSession = Boolean.parseBoolean(properties.getProperty("cleanSession"));
        String targetClientId = groupId + "@@@RECV0001";
        String accessKey = properties.getProperty("accessKey");
        String secretKey = properties.getProperty("secretKey");
        Properties initProperties = new Properties();
        initProperties.put(PropertyKeyConst.ProducerId, producerId);
        initProperties.put(PropertyKeyConst.AccessKey, accessKey);
        initProperties.put(PropertyKeyConst.SecretKey, secretKey);
        Producer producer = ONSFactory.createProducer(initProperties);
        producer.start();
        final Message msg = new Message(
                topic,//the topic is mqtt parent topic
                "MQ2MQTT",//MQ Tag,must set MQ2MQTT
                "hello mq send mqtt msg".getBytes());//mqtt msg body
        //send mormal mqtt msg ,set the property "mqttSecondTopic={{your mqtt subTopic}}"
        msg.putUserProperties("mqttSecondTopic", "/qos" + qosLevel);
        //mq send mqtt msg ,the qos default =1
        msg.putUserProperties("qoslevel", String.valueOf(qosLevel));
        //mq send mqtt msg ,the cleansession default set true
        msg.putUserProperties("cleansessionflag", String.valueOf(cleanSession));
        SendResult result = producer.send(msg);
        System.out.println(result);

        final Message msg2 = new Message(
                topic,//the topic is mqtt parent topic
                "MQ2MQTT",//MQ Tag,must set MQ2MQTT
                "hello mq send mqtt msg".getBytes());//mqtt msg body
        //send p2p mqtt msg ,also set the mqttSecondTopic property to subTopic
        msg2.putUserProperties("mqttSecondTopic", "/p2p/" + targetClientId);
        SendResult sendResult = producer.send(msg2);
        System.out.println(sendResult);
        producer.shutdown();
        System.exit(0);
    }

}
