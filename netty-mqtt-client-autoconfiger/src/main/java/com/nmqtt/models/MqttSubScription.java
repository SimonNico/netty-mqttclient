package com.nmqtt.models;

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

    public void setCalled(boolean called) {
        this.called = called;
    }

    @Override
    public int hashCode(){
        int topichashcode=topic.hashCode();
        topichashcode=10*topichashcode+handler.hashCode();
        topichashcode=10*topichashcode + (once ? 1 : 0);
        return  topichashcode;
    }

    @Override
    public boolean equals(Object obj){
        if (this == obj) return true;
        if (obj== null || getClass() != obj.getClass()) return false;
        MqttSubScription that = (MqttSubScription) obj;
        return once == that.once && topic.equals(that.topic) && handler.equals(that.handler);
    }
}
