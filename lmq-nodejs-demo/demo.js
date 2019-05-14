
// 开源sdk地址：https://github.com/mqttjs/MQTT.js
var mqtt = require('mqtt')
var CryptoJS = require("crypto-js");

var accessKey='xxxx'
var secretKey='xxxx'

var clientId = 'xxxx'
var instanceId='xxxx'

// https://help.aliyun.com/document_detail/48271.html
var username = 'Signature|' + accessKey + '|' + instanceId;
var password = CryptoJS.HmacSHA1(clientId, secretKey).toString(CryptoJS.enc.Base64);

var options={
	'username':username,
	'password':password,
	'clientId':clientId,
    	'keepalive':90,
    	'connectTimeout': 3000
}

//tls安全连接："tls://host:8883"
var client  = mqtt.connect('tcp://xxxx:1883',options)

var topic='xxxxx'

client.on('connect', function () {
  client.subscribe(topic, {'qos':1})
})

client.on('message', function (topic, message) {
  console.log('topic:'+topic+' msg:'+message.toString())
})

var i=0
setInterval(function(){
	client.publish(topic, 'Hello mqtt ' + (i++))
	client.publish(topic + '/p2p/' + clientId, 'Hello mqtt ' + (i++))
},1000)



