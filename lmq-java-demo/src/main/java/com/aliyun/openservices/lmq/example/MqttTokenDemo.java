package com.aliyun.openservices.lmq.example;

import com.alibaba.fastjson.JSONObject;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttTokenDemo {
    public static void main(String[] args) throws MqttException, InterruptedException {
        String broker = "tcp://XXXX:1883";
        String clientId = "GID_XXX@@@XXXX";
        final String topic = "XXXX/XXXXX";
        MemoryPersistence persistence = new MemoryPersistence();
        final MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
        final MqttConnectOptions connOpts = new MqttConnectOptions();
        System.out.println("Connecting to broker: " + broker);
        connOpts.setServerURIs(new String[] {broker});
        connOpts.setCleanSession(true);
        connOpts.setKeepAliveInterval(90);
        sampleClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                //when connect success ,need upload token first
                JSONObject object = new JSONObject();
                object.put("token", "XXXX");//body of token
                object.put("type", "RW");//type of token ,like RW，R，W
                MqttMessage message = new MqttMessage(object.toJSONString().getBytes());
                message.setQos(1);
                try {
                    sampleClient.publish("$SYS/uploadToken", message);
                    sampleClient.subscribe(topic, 0);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void connectionLost(Throwable throwable) {
                System.out.println("mqtt connection lost");
                throwable.printStackTrace();
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                if (topic.equals("$SYS/tokenInvalidNotice")) {
                    //Token illegal
                } else if (topic.equals("$SYS/tokenExpireNotice")) {
                    //Token will expire
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                System.out.println("deliveryComplete:" + iMqttDeliveryToken.getMessageId());
            }
        });
        sampleClient.connect(connOpts);
        JSONObject object = new JSONObject();
        object.put("token", "XXXX");//body of token
        object.put("type", "RW");//type of token ,like RW，R，W
        MqttMessage message = new MqttMessage(object.toJSONString().getBytes());
        message.setQos(1);
        MqttTopic pubTopic = sampleClient.getTopic("$SYS/uploadToken");
        MqttDeliveryToken token = pubTopic.publish(message);
        token.waitForCompletion();//sync wait
        //Token upload ok , do normal sub and pub
        sampleClient.subscribe(topic, 0);
        while (true) {
            sampleClient.publish(topic, message);
            Thread.sleep(5000);
        }
    }
}
