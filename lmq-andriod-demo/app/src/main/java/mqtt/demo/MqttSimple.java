package mqtt.demo;

import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttSimple {

    private MqttAndroidClient mqttAndroidClient;

    public MqttSimple(MqttAndroidClient mqttAndroidClient) {
        this.mqttAndroidClient = mqttAndroidClient;
    }

    public void test() {
        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.e("close", "connectionLost", cause);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String body = new String(message.getPayload());
                Log.w("messageArrived", body);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setConnectionTimeout(3000);
        mqttConnectOptions.setKeepAliveInterval(90);
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(true);

        try {
            // 参考 https://help.aliyun.com/document_detail/54225.html

            // Signature 方式
            mqttConnectOptions.setUserName("Signature|" + Config.accessKey + "|" + Config.instanceId);
            mqttConnectOptions.setPassword(Tool.macSignature(Config.clientId, Config.secretKey).toCharArray());

            /**
             * Token方式
             *  mqttConnectOptions.setUserName("Token|" + Config.accessKey + "|" + Config.instanceId);
             *  mqttConnectOptions.setPassword("RW|xxx");
             */
        } catch (Exception e) {
            Log.e("exception", "setPassword", e);
        }

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w("connect", "onSuccess");
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e("connect", "onFailure", exception);
                }
            });
        } catch (MqttException e) {
            Log.e("connect", "exception", e);
        }
    }

    public void subscribeToTopic() {
        try {
            final String topicFilter[] = {Config.topic};
            final int[] qos = {1};
            mqttAndroidClient.subscribe(topicFilter, qos, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w("subscribe", "success");
                    publishMessage();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e("subscribe", "failed", exception);
                }
            });

        } catch (MqttException ex) {
            Log.e("subscribe", "exception", ex);
        }
    }


    public void publishMessage() {
        try {
            MqttMessage message = new MqttMessage();
            final String msg = "msg_test";
            message.setPayload(msg.getBytes());
            mqttAndroidClient.publish(Config.topic, message, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w("publish", "success:" + msg);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w("publish", "failed:" + msg);
                }
            });
        } catch (MqttException e) {
            Log.e("publish", "exception", e);
        }
    }

}
