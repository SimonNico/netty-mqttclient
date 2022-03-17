package com.nmqtt.interfaces;

public interface MqttClientCallBack {

    void afterReconnect();

    void onLostConnection(Throwable throwable);
}
