<?php

use Mosquitto\Client;

##此处填写阿里云帐号 AccessKey
$accessKey = 'XXXX';
##此处填写阿里云帐号 SecretKey
$secretKey = 'XXXX';
## 接入点地址，购买实例后从控制台获取
$endPoint = 'XXXX.mqtt.aliyuncs.com';
##实例 ID，购买后从控制台获取
$instanceId = 'XXXX';
## MQTT Topic,其中第一级 Topic 需要在 MQTT 控制台提前申请
$topic = 'XXXX';
## MQTT 客户端ID 前缀， GroupID，需要在 MQTT 控制台申请
$groupId = 'GID_XXXX';
## MQTT 客户端ID 后缀，DeviceId，业务方自由指定，需要保证全局唯一，禁止 2 个客户端连接使用同一个 ID
$deviceId = 'XXXX';
##用于设置顺序属性的系统命令 topic
$sysCmdTopic='$SYS/enableOrderMsg';
$qos = 0;
$port = 1883;
$keepalive = 90;
$cleanSession = true;
$clientId = $groupId . '@@@' . $deviceId;
echo $clientId . "\n";

$mid = 0;
## 初始化客户端，需要设置 clientId 和 CleanSession 参数，参考官网文档规范
$mqttClient = new Mosquitto\Client($clientId, $cleanSession);


## 设置鉴权参数，参考 MQTT 客户端鉴权代码计算 username 和 password
$username = 'Signature|' . $accessKey . '|' . $instanceId;
$sigStr = hash_hmac("sha1", $clientId, $secretKey, true);
$password = base64_encode($sigStr);
echo "UserName:" . $username . "  Password:" . $password . "\n";
$mqttClient->setCredentials($username, $password);

## 设置连接成功回调
$mqttClient->onConnect(function ($rc, $message) use ($mqttClient, &$mid, $topic, $qos, $sysCmdTopic) {
    echo "Connnect to Server Code is " . $rc . " message is " . $message . "\n";
    ## 连接成功后需要在发消息之前提前设置顺序属性，然后再发消息，才能保证同一个 clientId 发的消息顺序。
    $orderCmdBody = "{\"order\":\"true\"}";
    $mqttClient->publish($sysCmdTopic, $orderCmdBody, 1);
    $mqttClient->subscribe($topic, $qos);
});


## 设置订阅成功回调
$mqttClient->onSubscribe(function () use ($mqttClient, $topic, $qos) {
    $mqttClient->publish($topic, "Hello MQTT PHP Demo", $qos);
});

## 设置发送成功回调
$mqttClient->onPublish(function ($publishedId) use ($mqttClient, $mid) {
    echo "publish message success " . $mid . "\n";
});


## 设置消息接收回调
$mqttClient->onMessage(function ($message) {
    echo "Receive Message From mqtt, topic is " . $message->topic . "  qos is " . $message->qos . "  messageId is " . $message->mid . "  payload is " . $message->payload . "\n";

});
$mqttClient->connect($endPoint, $port, $keepalive);


$mqttClient->loopForever();

echo "Finished";