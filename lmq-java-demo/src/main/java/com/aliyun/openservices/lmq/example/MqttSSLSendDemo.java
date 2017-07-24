package com.aliyun.openservices.lmq.example;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Properties;

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
        //use ssl socket
        SocketFactory socketFactory = initSSLSocket("intermedia.crt");
        connOpts.setSocketFactory(socketFactory);
        mqttClient.setCallback(new MqttCallback() {
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

    private static SSLSocketFactory initSSLSocket(String certFileName) throws Exception {
        InputStream caInput = new BufferedInputStream(ClassLoader.getSystemResourceAsStream(certFileName));
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate ca = null;
        try {
            ca = cf.generateCertificate(caInput);
        } catch (CertificateException e) {
            e.printStackTrace();
        } finally {
            caInput.close();
        }
        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);
        SSLContext context = SSLContext.getInstance("TLSV1.2");
        context.init(null, tmf.getTrustManagers(), null);
        SSLSocketFactory socketFactory = context.getSocketFactory();
        return socketFactory;
    }
}
