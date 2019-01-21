package com.aliyun.openservices.lmq.example.demo;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.lmq.example.util.Tools;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 本文件提供了 TokenAPI 的操作示例，实际场景中该代码应该由业务方的应用服务器调用，应用服务器负责管理设备的权限，向 MQ4IoT 申请 token 并发放给设备。具体交互参考：
 * https://help.aliyun.com/document_detail/54226.html?spm=a2c4g.11186623.6.559.ca8a695aco5DF7
 */
public class TokenApiDemo {
    private static final String applyTokenUrl = "/token/apply";
    private static final String revokeTokenUrl = "/token/revoke";

    public static void main(String[] args)
        throws UnrecoverableKeyException, NoSuchAlgorithmException, IOException, KeyManagementException,
        KeyStoreException, InvalidKeyException {
        List<String> resource = new ArrayList<String>();
        resource.add("XXXX/AAA");
        String token = applyToken("https://mqauth.aliyuncs.com", "XXXXX", "XXXXX",
            resource, "R,W", 2592000000L, "XXXX");
        System.out.println(token);
    }

    /**
     * 申请 Token 接口，具体参数参考链接https://help.aliyun.com/document_detail/54276.html?spm=a2c4g.11186623.6.562.f12033f5ay6nu5
     *
     * @param apiUrl token 服务器地址，参考文档设置正确的地址
     * @param accessKey 账号 AccessKey，由控制台获取
     * @param secretKey 账号 SecretKey，由控制台获取
     * @param topics 申请的 topic 列表
     * @param action Token类型
     * @param expireTime Token 过期的时间戳
     * @param instanceId MQ4IoT 实例 Id
     * @return 如果申请成功则返回 token 内容
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws KeyStoreException
     * @throws UnrecoverableKeyException
     * @throws KeyManagementException
     */
    public static String applyToken(String apiUrl, String accessKey, String secretKey, List<String> topics,
        String action,
        long expireTime,
        String instanceId) throws InvalidKeyException, NoSuchAlgorithmException, IOException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {
        Map<String, String> paramMap = new HashMap<String, String>();
        Collections.sort(topics);
        StringBuilder builder = new StringBuilder();
        for (String topic : topics) {
            builder.append(topic).append(",");
        }
        if (builder.length() > 0) {
            builder.setLength(builder.length() - 1);
        }
        paramMap.put("resources", builder.toString());
        paramMap.put("actions", action);
        paramMap.put("serviceName", "mq");
        paramMap.put("expireTime", String.valueOf(System.currentTimeMillis() + expireTime));
        paramMap.put("instanceId", instanceId);
        String signature = Tools.doHttpSignature(paramMap, secretKey);
        paramMap.put("proxyType", "MQTT");
        paramMap.put("accessKey", accessKey);
        paramMap.put("signature", signature);
        JSONObject object = Tools.httpsPost(apiUrl + applyTokenUrl, paramMap);
        if (object != null) {
            return (String) object.get("tokenData");
        }
        return null;
    }

    /**
     * 提前注销 token，一般在 token 泄露出现安全问题时，提前禁用特定的客户端
     *
     * @param apiUrl token 服务器地址，参考文档设置正确的地址
     * @param accessKey 账号 AccessKey，由控制台获取
     * @param secretKey 账号 SecretKey，由控制台获取
     * @param token 禁用的 token 内容
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws UnrecoverableKeyException
     * @throws KeyStoreException
     * @throws KeyManagementException
     */
    public static void revokeToken(String apiUrl, String accessKey, String secretKey,
        String token) throws InvalidKeyException, NoSuchAlgorithmException, IOException, UnrecoverableKeyException, KeyStoreException, KeyManagementException {
        Map<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("token", token);
        String signature = Tools.doHttpSignature(paramMap, secretKey);
        paramMap.put("signature", signature);
        paramMap.put("accessKey", accessKey);
        JSONObject object = Tools.httpsPost(apiUrl + revokeTokenUrl, paramMap);
    }
}
