package com.nmqtt.handlers;

import com.nmqtt.NettyMqttClient;
import com.nmqtt.utils.MqttConnectResult;
import com.nmqtt.utils.MqttPendingSubscription;
import com.nmqtt.utils.MqttPendingUnsubscription;
import com.nmqtt.utils.MqttSubScription;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.concurrent.Promise;

public class MqttChannelHandler extends SimpleChannelInboundHandler<MqttMessage> {

    private final NettyMqttClient client;
    private final Promise<MqttConnectResult> connectFuture;

    public MqttChannelHandler(NettyMqttClient client, Promise<MqttConnectResult> connectFuture) {
        this.client = client;
        this.connectFuture = connectFuture;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, MqttMessage mqttMessage) throws Exception {
        switch (mqttMessage.fixedHeader().messageType()) {
            case CONNACK:
                handleConack(channelHandlerContext.channel(), (MqttConnAckMessage) mqttMessage);
                break;
            case SUBACK:
                handleSubAck( (MqttSubAckMessage) mqttMessage);
                break;
            case UNSUBACK:
                handleUnsuback((MqttUnsubAckMessage) mqttMessage);
                break;
            default:
                break;
        }
    }

    private void handleConack(Channel channel, MqttConnAckMessage message) {
        switch (message.variableHeader().connectReturnCode()) {
            case CONNECTION_ACCEPTED:
                this.connectFuture.setSuccess(new MqttConnectResult(true, MqttConnectReturnCode.CONNECTION_ACCEPTED, channel.closeFuture()));

                this.client.getMqttPendingSubscription().entrySet().stream().filter((e) -> !e.getValue().isSent()).forEach((e) -> {
                    channel.write(e.getValue().getSubscribeMessage());
                    e.getValue().setSent(true);
                });
                channel.flush();
                if (this.client.getMqttConfigProperties().isReconnect()) {
                    this.client.getCallback().afterReconnect();
                }
                break;
            case CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD:
            case CONNECTION_REFUSED_IDENTIFIER_REJECTED:
            case CONNECTION_REFUSED_NOT_AUTHORIZED:
            case CONNECTION_REFUSED_SERVER_UNAVAILABLE:
            case CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION:
                this.connectFuture.setSuccess(new MqttConnectResult(false, message.variableHeader().connectReturnCode(), channel.closeFuture()));
                channel.close();
                break;
        }
    }

    private void handleSubAck(MqttSubAckMessage message) {
        MqttPendingSubscription mqttPendingSubscription = this.client.getMqttPendingSubscription().remove(message.variableHeader().messageId());
        if (mqttPendingSubscription == null) { return; }
        mqttPendingSubscription.onSubackReceived();
        for (MqttPendingSubscription.MqttPendingHandler handler : mqttPendingSubscription.getHandlers()) {
            MqttSubScription subscription = new MqttSubScription(mqttPendingSubscription.getTopic(), handler.getHandler(), handler.isOnce());
            this.client.getSubscriptions().put(mqttPendingSubscription.getTopic(), subscription);
            this.client.getHandlerToSubscribtion().put(handler.getHandler(), subscription);
        }
        this.client.getPendingSubscribeTopics().remove(mqttPendingSubscription.getTopic());
        this.client.getServerSubscriptions().add(mqttPendingSubscription.getTopic());
        if (!mqttPendingSubscription.getFuture().isDone()) {
            mqttPendingSubscription.getFuture().setSuccess(null);
        }
    }

    private void handleUnsuback(MqttUnsubAckMessage message) {
        MqttPendingUnsubscription mqttPendingUnsubscription = this.client.getPendingServerUnsubscribes().get(message.variableHeader().messageId());
        if (mqttPendingUnsubscription == null) { return; }
        mqttPendingUnsubscription.onUnsubackReceived();
        this.client.getServerSubscriptions().remove(mqttPendingUnsubscription.getTopic());
        mqttPendingUnsubscription.getFuture().setSuccess(null);
        this.client.getPendingServerUnsubscribes().remove(message.variableHeader().messageId());
    }
}
