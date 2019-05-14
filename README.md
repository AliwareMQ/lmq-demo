# 微消息队列 MQTT 示例说明
本代码提供阿里云微消息队列 MQTT 的运行示例，根据开发语言进行第一级分类，根据功能和场景进行二级分类。
由于部分语言的示例代码更新节奏不一，所以部分场景的示例会有缺失。后续会持续更新。因为 MQTT 是一个标准协议，因此所有开发语言的功能都是一致的。

**注意：示例代码仅提供一个简单的方法和运行参考，其中的参数都已经脱敏，实际业务使用时需要替换成实际申请的资源。**

## SDK 依赖安装
微消息队列 MQTT 支持标准的 MQTT 3.1.1 协议，理论上能够适配所有的 MQTT 客户端，因此阿里云官方并不提供 SDK 目前。
接入时可以选择以下推荐的第三方 SDK。不排除部分客户端存在细节上的兼容性问题。针对 MQTT 用户常用的平台，推荐对应的三方包如下：

| 开发语言和平台       | 推荐的第三方 SDK              | 相关链接                                     |
| ---------- | ----------------------- | ---------------------------------------- |
| Java       | Eclipse Paho SDK        | [http://www.eclipse.org/paho/clients/java/](http://www.eclipse.org/paho/clients/java/) |
| iOS        | MQTT-Client-Framework   | [https://github.com/ckrey/MQTT-Client-Framework](https://github.com/ckrey/MQTT-Client-Framework) |
| Android    | Eclipse Paho SDK        | [https://github.com/eclipse/paho.mqtt.android](https://github.com/eclipse/paho.mqtt.android) |
| JavaScript | Eclipse Paho JavaScript | [http://www.eclipse.org/paho/clients/js/](http://www.eclipse.org/paho/clients/js/) |
| Python | Eclipse Paho Python SDK | [https://pypi.python.org/pypi/paho-mqtt/](https://pypi.python.org/pypi/paho-mqtt/) |
|C| Eclipse Paho C SDK|https://eclipse.org/paho/clients/c/|
|C#| Eclipse Paho C# SDK|https://github.com/eclipse/paho.mqtt.m2mqtt|
|Go|Eclipse Paho Go SDK|https://github.com/eclipse/paho.mqtt.golang|
|Nodejs|MQTT JS|https://github.com/mqttjs/MQTT.js|
其他语言的客户端 SDK 如 PHP 等暂时没有提供测试。如有需要可以访问 https://github.com/mqtt/mqtt.github.io/wiki/libraries 进行下载。

本代码仓库仅提供示例方法和 Demo 片段，部分开发语言需要的工程框架和 IDE 配置不会提供，因此使用时请自行移植。



