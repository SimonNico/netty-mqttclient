package com.nmqtt.utils;

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;

public final class MqttConnectResult {
    private final boolean success;
    private final MqttConnectReturnCode returnCode;
    private final ChannelFuture closeFuture;

    public MqttConnectResult(boolean success, MqttConnectReturnCode returnCode, ChannelFuture closeFuture) {
        this.success = success;
        this.returnCode = returnCode;
        this.closeFuture = closeFuture;
    }

    public boolean isSuccess() {
        return success;
    }

    public MqttConnectReturnCode getReturnCode() {
        return returnCode;
    }

    public ChannelFuture getCloseFuture() {
        return closeFuture;
    }
}
