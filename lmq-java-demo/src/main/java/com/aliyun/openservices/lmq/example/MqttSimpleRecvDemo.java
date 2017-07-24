package com.aliyun.openservices.lmq.example;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by alvin on 17-7-24.
 * This is simple example for mqtt java client receive mqtt msg
 */
public class MqttSimpleRecvDemo {
    public static void main(String[] args) throws Exception {
        final AtomicBoolean loseConnection = new AtomicBoolean(false);
        Properties properties = Tools.loadProperties();
        final String brokerUrl = properties.getProperty("brokerUrl");
        final String groupId = properties.getProperty("groupId");
        final String topic = properties.getProperty("topic");
        final int qosLevel = Integer.parseInt(properties.getProperty("qos"));
        final Boolean cleanSession = Boolean.parseBoolean(properties.getProperty("cleanSession"));
        String clientId = groupId + "@@@RECV0001";
        String accessKey = properties.getProperty("accessKey");
        String secretKey = properties.getProperty("secretKey");
        final MemoryPersistence memoryPersistence = new MemoryPersistence();
        final MqttClient mqttClient = new MqttClient(brokerUrl, clientId, memoryPersistence);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        //cal the sign as password,sign=BASE64(MAC.SHA1(groupId,secretKey))
        String sign = Tools.macSignature(clientId.split("@@@")[0], secretKey);
        connOpts.setUserName(accessKey);
        connOpts.setPassword(sign.toCharArray());
        connOpts.setCleanSession(cleanSession);
        connOpts.setKeepAliveInterval(90);
        connOpts.setAutomaticReconnect(true);
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {
                loseConnection.set(true);
                throwable.printStackTrace();
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                System.out.println("receive msg from topic " + s + " , body is " + new String(mqttMessage.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                System.out.println("send msg succeed");
            }
        });
        mqttClient.connect(connOpts);
        final String topicFilter[] = {topic + "/qos" + qosLevel};
        final int[] qos = {qosLevel};
        mqttClient.subscribe(topicFilter, qos);
        while (true) {
            if (loseConnection.get()) {
                //when reconnect success ,should manual do sub again
                if (mqttClient.isConnected()) {
                    mqttClient.subscribe(topicFilter, qos);
                    loseConnection.set(false);
                }
            }
            Thread.sleep(1000);
        }
    }
}
