package com.aliyun.openservices.lmq.example.demo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.ons.api.Action;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Consumer;
import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.MessageListener;
import com.aliyun.openservices.ons.api.ONSFactory;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MQTTClientStatusNoticeProcessDemo {
    public static void main(String[] args) {
        /**
         * 初始化消息队列 for RocketMQ 接收客户端，实际业务中一般部署在服务端应用中。
         */
        Properties properties = new Properties();
        /**
         * 设置 RocketMQ 客户端的 GroupID，注意此处的 groupId 和 MQ4IoT 实例中的 GroupId 是2个概念，请按照各自产品的说明申请填写
         */
        properties.setProperty(PropertyKeyConst.GROUP_ID, "GID_XXXXX");
        /**
         * 账号 accesskey，从账号系统控制台获取
         */
        properties.put(PropertyKeyConst.AccessKey, "XXXX");
        /**
         * 账号 secretKey，从账号系统控制台获取，仅在Signature鉴权模式下需要设置
         */
        properties.put(PropertyKeyConst.SecretKey, "XXXX");
        /**
         * 设置 TCP 接入域名
         */
        properties.put(PropertyKeyConst.NAMESRV_ADDR, "http://XXXXX");
        /**
         * 使用 RocketMQ 消费端来处理 MQTT 客户端的上下线通知时，订阅的 topic 为上下线通知 Topic，请遵循控制台文档提前创建。
         */
        final String parentTopic = "GID_XXXXX_MQTT";
        /**
         * 客户端状态数据，实际生产环境中建议使用数据库或者 Redis等外部持久化存储来保存该信息，避免应用重启丢失状态
         */
        final Map<String /*clientId*/, Map<String /*channelId*/, ClientStatus>> clientStatusMap = new ConcurrentHashMap<String, Map<String, ClientStatus>>();
        Consumer consumer = ONSFactory.createConsumer(properties);
        consumer.subscribe(parentTopic, "*", new MqttClientStatusNoticeListener(clientStatusMap));
        consumer.start();
        ScheduledExecutorService cleanService = new ScheduledThreadPoolExecutor(1);
        cleanService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                /**
                 * 清理掉部分只有很久之前(例如半小时)的 offline 状态的数据，这部分只有 offline 的数据可能由于应用部署或者MQTT 服务端运维导致上线状态异常
                 */
                for (Map<String, ClientStatus> channelMap : clientStatusMap.values()) {
                    Iterator<Map.Entry<String, ClientStatus>> iterator = channelMap.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<String, ClientStatus> entry = iterator.next();
                        if (entry.getValue().getOfflineTime() != null && System.currentTimeMillis() - entry.getValue().getOfflineTime() > 1800 * 1000) {
                            iterator.remove();
                        }
                    }
                }
            }
        }, 10, 10, TimeUnit.MINUTES);
        String clientId = "GID_XXXX@@@XXXXX";
        while (true) {
            System.out.println("ClientStatus :" + checkClientOnline(clientId, clientStatusMap));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 处理上下线通知的逻辑。处理状态机参考文档：https://help.aliyun.com/document_detail/50069.html?spm=a2c4g.11186623.6.554.694f767aUuuUrQ
     * 实际部署过程中，消费上下线通知的应用可能部署多台机器，因此客户端在线状态的数据可以使用数据库或者Redis等外部共享存储来维护。
     * 其次需要单独做消息幂等处理，以免重复接收消息导致状态机判断错误。
     */
    static class MqttClientStatusNoticeListener implements MessageListener {
        private Map<String /*clientId*/, Map<String /*channelId*/, ClientStatus>> clientStatusMap;

        public MqttClientStatusNoticeListener(
            Map<String, Map<String, ClientStatus>> clientStatusMap) {
            this.clientStatusMap = clientStatusMap;
        }

        @Override
        public Action consume(Message message, ConsumeContext context) {
            try {
                JSONObject msgBody = JSON.parseObject(new String(message.getBody()));
                System.out.println(msgBody);
                String eventType = msgBody.getString("eventType");
                String clientId = msgBody.getString("clientId");
                String channelId = msgBody.getString("channelId");
                String clientIp = msgBody.getString("clientIp");
                Long time = msgBody.getLong("time");
                ClientStatus status = new ClientStatus();
                status.setChannelId(channelId);
                status.setClientIp(clientIp);
                if ("connect".equals(eventType)) {
                    status.setOnlineTime(time);
                } else {
                    status.setOfflineTime(time);
                }
                /**
                 * 先检查是否有 channel 表
                 */
                Map<String, ClientStatus> channelMap = new ConcurrentHashMap<>();
                Map<String, ClientStatus> oldChannelMap = clientStatusMap.putIfAbsent(clientId, channelMap);
                if (oldChannelMap != null) {
                    channelMap = oldChannelMap;
                }
                synchronized (channelMap) {
                    /**
                     * 检查是否已经处理过当前 channel 的数据，如果没有，直接 put 数据即可
                     */
                    ClientStatus oldStatus = channelMap.putIfAbsent(channelId, status);
                    if (oldStatus != null) {
                        /**
                         * 如果已经处理过当前 channel 的数据，则需要增量更新
                         * 1.如果onlineTime 为空，说明之前没有收到过 connect 事件，则更新 online 状态
                         * 2.如果offlineTime 为空，说明之前没有收到过 disconnect 或 close 事件，则更新 offline 状态
                         */
                        if (oldStatus.getOnlineTime() == null) {
                            oldStatus.setOnlineTime(status.getOnlineTime());
                        }
                        if (oldStatus.getOfflineTime() == null) {
                            oldStatus.setOfflineTime(status.getOfflineTime());
                        }
                        status = oldStatus;
                    }
                    /**
                     * 对于同一个 channel 已经收集到 offline 和 online，则已经完整。可以清理状态机
                     */
                    if (status.getOfflineTime() != null && status.getOnlineTime() != null) {
                        //close the channel
                        channelMap.remove(channelId);
                    }
                }
                return Action.CommitMessage;
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return Action.ReconsumeLater;
        }
    }

    /**
     * 根据状态表判断一个 clientId 是否有活跃的 tcp 连接
     * 1.如果没有 channel 表，则一定不在线
     * 2.如果 channel 表非空，检查一下 channel 数据中是否有未断开的 channel（offlineTime 为空）,如果有则代表有活跃连接，在线。
     * 如果全部的 channel 都已断开则一定是不在线。
     *
     * @param clientId
     * @param clientStatusMap
     * @return
     */
    public static boolean checkClientOnline(String clientId,
        Map<String /*clientId*/, Map<String /*channelId*/, ClientStatus>> clientStatusMap) {
        Map<String, ClientStatus> channelMap = clientStatusMap.get(clientId);
        if (channelMap == null) {
            return false;
        }
        for (ClientStatus status : channelMap.values()) {
            if (status.getOfflineTime() == null) {
                return true;
            }
        }
        return false;
    }

    static class ClientStatus {
        private String channelId;
        private String clientIp;
        private Long onlineTime;
        private Long offlineTime;

        public String getChannelId() {
            return channelId;
        }

        public void setChannelId(String channelId) {
            this.channelId = channelId;
        }

        public String getClientIp() {
            return clientIp;
        }

        public void setClientIp(String clientIp) {
            this.clientIp = clientIp;
        }

        public Long getOnlineTime() {
            return onlineTime;
        }

        public void setOnlineTime(Long onlineTime) {
            this.onlineTime = onlineTime;
        }

        public Long getOfflineTime() {
            return offlineTime;
        }

        public void setOfflineTime(Long offlineTime) {
            this.offlineTime = offlineTime;
        }
    }
}
