package main

import (
	"crypto/hmac"
	"crypto/sha1"
	"encoding/base64"
	"fmt"
	"log"
	"os"
	"time"

	mqtt "github.com/eclipse/paho.mqtt.golang"
)

const (
	topic      = "test"     //测试收发消息的 Topic
	instanceID = "XXXX"     //实例 ID，购买后从控制台获取
	accessKey  = "XXXX"     //账号 AccessKey，从账号控制台获取
	secretKey  = "XXXX"     //账号 SecretKey，从账号控制台获取
	groupID    = "GID-XXXX" //客户端使用的 GroupID，从控制台申请
	deviceID   = "XXXX"     //客户端 ClientID 的后缀，由业务自行指定，只需要保证全局唯一即可
)

func PublishHandler(client mqtt.Client, msg mqtt.Message) {
	log.Printf("TOPIC: %s\n", msg.Topic())
	log.Printf("MSG: %s\n", msg.Payload())
}

func OnConnectHandler(client mqtt.Client) {
	log.Println("connect success")

	if token := client.Subscribe(topic, 0, nil); token.Wait() && token.Error() != nil {
		fmt.Println(token.Error())
		os.Exit(1)
	}

	time.AfterFunc(time.Millisecond*100, func() {
		text := fmt.Sprintf("hello mqtt demo")
		token := client.Publish(topic, 0, false, text)
		token.Wait()
	})
}

func main() {
	//ClientID要求使用 GroupId 和 DeviceId 拼接而成，长度不得超过64个字符
	clientID := fmt.Sprintf("%s@@@%s", groupID, deviceID)

	host := fmt.Sprintf("tcp://%s.mqtt.aliyuncs.com:1883", instanceID)
	opts := mqtt.NewClientOptions().AddBroker(host)

	opts.SetClientID(clientID)
	opts.SetOnConnectHandler(OnConnectHandler)
	opts.SetDefaultPublishHandler(PublishHandler)
	opts.SetCleanSession(true)

	//username和 Password 签名模式下的设置方法，参考文档 https://help.aliyun.com/document_detail/48271.html?spm=a2c4g.11186623.6.553.217831c3BSFry7
	username := fmt.Sprintf("Signature|%s|%s", accessKey, instanceID)
	opts.SetUsername(username)

	mac := hmac.New(sha1.New, []byte(secretKey))
	mac.Write([]byte(clientID))
	password := base64.StdEncoding.EncodeToString(mac.Sum(nil))
	opts.SetPassword(password)

	c := mqtt.NewClient(opts)
	if token := c.Connect(); token.Wait() && token.Error() != nil {
		panic(token.Error())
	}

	time.Sleep(time.Second * 1)

	if token := c.Unsubscribe(topic); token.Wait() && token.Error() != nil {
		fmt.Println(token.Error())
		os.Exit(1)
	}

	c.Disconnect(250)
}
