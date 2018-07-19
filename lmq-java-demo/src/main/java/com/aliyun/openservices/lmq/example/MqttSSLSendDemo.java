package com.aliyun.openservices.lmq.example;

import java.util.Properties;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import static org.eclipse.paho.client.mqttv3.MqttConnectOptions.MQTT_VERSION_3_1_1;

/**
 * Created by alvin on 17-7-24.
 * This is simple example for mqtt java client send mqtt msg
 */
public class MqttSSLSendDemo {
    public static void main(String[] args) throws Exception {
        Properties properties = Tools.loadProperties();
        final String brokerUrl = properties.getProperty("sslBrokerUrl");
        final String groupId = properties.getProperty("groupId");
        final String topic = properties.getProperty("topic");
        final int qosLevel = Integer.parseInt(properties.getProperty("qos"));
        final Boolean cleanSession = Boolean.parseBoolean(properties.getProperty("cleanSession"));
        String clientId = groupId + "@@@SEND0001";
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
        connOpts.setMqttVersion(MQTT_VERSION_3_1_1);
        mqttClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                System.out.println("connect success");
            }

            @Override
            public void connectionLost(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                System.out.println("receive msg from topic " + s + " , body is " + new String(mqttMessage.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                //this notice make sense when qos >0
                System.out.println("send msg succeed");
            }
        });
        mqttClient.connect(connOpts);
        while (true) {
            try {
                //async send normal pub sub msg
                final String mqttSendTopic = topic + "/qos" + qosLevel;
                MqttMessage message = new MqttMessage("hello lmq pub sub msg".getBytes());
                message.setQos(qosLevel);
                mqttClient.publish(mqttSendTopic, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Thread.sleep(1000);
        }
    }
}
