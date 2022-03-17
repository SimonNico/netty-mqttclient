package com.nmqtt.models;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mqtt")
public class MqttConfigProperties {

    /**
     * mqttserver's host
     */
    private  String host;

    /**
     * mqttserver's port
     */
    private  int port;

    /**
     * acount
     */
    private String userName;

    private String passWord;

    /**
     * 0 retry 0 times when miss message
     * 1 retry times until get message
     * 2 retry times until get messagte & remove duplicates
     */
    private  int qos;

    private String topic;

    private  String clientId;

    private boolean retain;

    private int timeoutSeconds;

    private boolean isReconnect;



    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassWord() {
        return passWord;
    }

    public void setPassWord(String passWord) {
        this.passWord = passWord;
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public boolean isRetain() {
        return retain;
    }

    public void setRetain(boolean retain) {
        this.retain = retain;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean isReconnect() {
        return isReconnect;
    }

    public void setReconnect(boolean reconnect) {
        isReconnect = reconnect;
    }
}
