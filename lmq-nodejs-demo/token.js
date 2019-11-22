var request = require('request');
var CryptoJS = require("crypto-js");

function applyToken(url, data) {
    request({
        url: url,
        method: "POST",
        formData: data
    }, function (error, response, body) {
        console.log("request success:" + !error + "  httpcode:" + response.statusCode + "  body:" + JSON.stringify(body));
        if (!error && response.statusCode == 200) {
            var res = JSON.parse(body);
            console.log("\r\ntoken:" + res.tokenData);
        }
    });
};
var url = "http://XXXXXX/token/apply";
var actions = "R,W";
var resources = "XXXXX";
var accessKey = "XXXXXX";
var secretKey = "XXXXX";
var expireTime = (new Date()).valueOf() + 3600 * 1000;
var proxyType = "MQTT";
var serviceName = "mq";
var instanceId = "XXXXX";
var content = "actions=" + actions +
    "&expireTime=" + expireTime + "&instanceId=" + instanceId + "&resources=" + resources + "&serviceName=" + serviceName;
var signature = CryptoJS.HmacSHA1(content, secretKey).toString(CryptoJS.enc.Base64);
var requestData = {
    "actions": actions,
    "resources": resources,
    "accessKey": accessKey,
    "expireTime": expireTime,
    "proxyType": proxyType,
    "serviceName": serviceName,
    "instanceId": instanceId,
    "signature": signature
};
console.log("request:" + JSON.stringify(requestData));
applyToken(url, requestData);
