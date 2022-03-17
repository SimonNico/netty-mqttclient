package com.nmqtt.utils;

import com.nmqtt.interfaces.MqttHandler;

public final class MqttSubScription {

    private final String  topic;
    private final MqttHandler handler;

    private final boolean once;

    private boolean called;

    public MqttSubScription(String topic, MqttHandler handler, boolean once) {
        if(topic == null){
            throw new NullPointerException("topic");
        }
        if(handler == null){
            throw new NullPointerException("handler");
        }
        this.topic = topic;
        this.handler = handler;
        this.once = once;
    }
   public String getTopic() {
        return topic;
    }

    public MqttHandler getHandler() {
        return handler;
    }

   public  boolean isOnce() {
        return once;
    }

    public boolean isCalled() {
        return called;
    }
}
