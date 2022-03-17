package com.nmqtt.handlers;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public final class RetransmissionHandler<T extends MqttMessage> {

    private BiConsumer<MqttFixedHeader,T>handler;

    private int timeout=10;

    private T msg;

    private ScheduledFuture<?> timer;

    public void start(EventLoop eventLoop){
        if (eventLoop == null) {
            throw new NullPointerException("EventLoop");

        }
        if(this.handler==null){
            throw  new NullPointerException("FixedHeader");
        }

        this.timeout=10;
        startTimer(eventLoop);
    }

    void startTimer(EventLoop eventLoop){
        this.timer = eventLoop.schedule(() -> {
            this.timeout += 5;
            MqttFixedHeader fixedHeader = new MqttFixedHeader(this.msg.fixedHeader().messageType(), true, this.msg.fixedHeader().qosLevel(), this.msg.fixedHeader().isRetain(), this.msg.fixedHeader().remainingLength());
            handler.accept(fixedHeader, msg);
            startTimer(eventLoop);
        }, timeout, TimeUnit.SECONDS);
    }
    public void stop(){
        if(this.timer!=null){
            this.timer.cancel(true);
        }
    }

    public void setHandle(BiConsumer<MqttFixedHeader, T> runnable) {
        this.handler = runnable;
    }

    public void setOriginalMessage(T Message) {
        this.msg = Message;
    }
}
