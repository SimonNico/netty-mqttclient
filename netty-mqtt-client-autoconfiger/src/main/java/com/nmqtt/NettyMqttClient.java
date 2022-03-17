package com.nmqtt;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.nmqtt.handlers.MqttChannelHandler;
import com.nmqtt.handlers.MqttPingHandler;
import com.nmqtt.interfaces.MqttClientCallBack;
import com.nmqtt.interfaces.MqttHandler;
import com.nmqtt.models.MqttConfigProperties;
import com.nmqtt.utils.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


@SuppressWarnings({"WeakerAccess", "unused"})
public class NettyMqttClient   {

    private  String host;

    private  int port;

    private String userName;

    private String passWord;

    private  int qosLevel;

    private String topic;

    private  String clientId;

    private  final Set<String> serverSubStriptions=new HashSet<>();
    private final IntObjectHashMap<MqttPendingSubscription> pendingSubscriptionIntObjectHashMap=new IntObjectHashMap<>();
    private IntObjectHashMap<MqttPendingUnsubscription> unsubscriptionIntObjectHashMap=new IntObjectHashMap<>();
    private final HashMultimap<String, MqttSubScription> subscriptions = HashMultimap.create();
    private final IntObjectHashMap<MqttPendingSubscription> pendingSubscriptions = new IntObjectHashMap<>();
    private final Set<String> pendingSubscribeTopics = new HashSet<>();
    private final HashMultimap<MqttHandler, MqttSubScription> handlerToSubscribtion = HashMultimap.create();
    private final AtomicInteger nextMessageId = new AtomicInteger(1);

    private  MqttHandler defaultHandler;

    private EventLoopGroup eventLoop;
    private volatile Channel channel;
    private volatile boolean disconnected = false;
    private volatile boolean reconnect = false;
    private MqttClientCallBack callback;
    private  int timeoutSeconds;
    private  boolean isReconnect;
    private long retryInterval = 1L;
    private  MqttConfigProperties mqttConfigProperties;
    private MqttHandler handler;


    public  NettyMqttClient(MqttConfigProperties properties){
        this.host=properties.getHost();
        this.port=properties.getPort();
        this.userName=properties.getUserName();
        this.passWord=properties.getPassWord();
        this.qosLevel=properties.getQos();
        this.topic=properties.getTopic();
        this.clientId=properties.getClientId();
        this.timeoutSeconds=properties.getTimeoutSeconds();
        this.isReconnect=properties.isReconnect();
        this.mqttConfigProperties=properties;
    }

    public  MqttConfigProperties getMqttConfigProperties(){return  this.mqttConfigProperties;}


    public Future<MqttConnectResult> connect(String host, int port) {
        return connect(host, port, false);
    }

    private Future<MqttConnectResult> connect(String host, int port, boolean reconnect) {
        if (this.eventLoop == null) {
            this.eventLoop = new NioEventLoopGroup();
        }
        this.host = host;
        this.port = port;
        Promise<MqttConnectResult> connectFuture = new DefaultPromise<>(this.eventLoop.next());
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(this.eventLoop);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        bootstrap.remoteAddress(host, port);

        bootstrap.handler(new MqttChannelInitializer(connectFuture, host, port));

        ChannelFuture future = bootstrap.connect();
        future.addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                NettyMqttClient.this.channel = f.channel();
                NettyMqttClient.this.channel.closeFuture().addListener((ChannelFutureListener) channelFuture -> {
                    if (isConnected()) { return; }
                    if (callback != null) {
                        MqttConnectResult result = connectFuture.getNow();
                        if (result != null) {
                            if (result.getReturnCode() != MqttConnectReturnCode.CONNECTION_ACCEPTED) {
                                ChannelClosedException e = new ChannelClosedException(result.getReturnCode().name());
                                callback.onLostConnection(e);
                                connectFuture.tryFailure(e);
                            }
                        } else {
                            ChannelClosedException e = new ChannelClosedException("Channel is closed!", channelFuture.cause());
                            callback.onLostConnection(e);
                            connectFuture.tryFailure(e);
                        }
                    }
                    pendingSubscriptions.clear();
                    serverSubStriptions.clear();
                    subscriptions.clear();
                    pendingSubscriptionIntObjectHashMap.clear();
                    pendingSubscribeTopics.clear();
                    handlerToSubscribtion.clear();
                    scheduleConnect(host, port, true);
                });
            } else {
                if (callback != null) {
                    callback.onLostConnection(f.cause());
                }
                connectFuture.tryFailure(f.cause());
                scheduleConnect(host, port, reconnect);
            }
        });
        return connectFuture;
    }

    private void scheduleConnect(String host, int port, boolean reconnect) {
        if (this.isReconnect && !disconnected) {
            if (reconnect) {
                this.reconnect = true;
            }
            eventLoop.schedule((Runnable) () -> connect(host, port, reconnect), this.retryInterval, TimeUnit.SECONDS);
        }
    }

    public Future<MqttConnectResult> reconnect() {
        if (host == null) {
            throw new IllegalStateException("reconnect error. ");
        }
        return connect(host, port);
    }

    public EventLoopGroup getEventLoop() {
        return eventLoop;
    }
    public void setEventLoop(EventLoopGroup eventLoop) {
        this.eventLoop = eventLoop;
    }


    public boolean isConnected() {
        return !disconnected && channel != null && channel.isActive();
    }

    public void setMqttHandler(MqttHandler handler){
        this.defaultHandler=handler;
    }

    public Future<Void> on(String topic, MqttHandler handler, MqttQoS qos) {
        return createSubscription(topic, handler, false, qos);
    }

    public Future<Void> once(String topic, MqttHandler handler, MqttQoS qos) {
        return createSubscription(topic, handler, true, qos);
    }

    public Future<Void> once(String topic, MqttHandler handler) {
        return once(topic, handler, MqttQoS.AT_MOST_ONCE);
    }

    public Future<Void> off(String topic, MqttHandler handler) {
        Promise<Void> future = new DefaultPromise<>(this.eventLoop.next());
        for (MqttSubScription subscription : this.handlerToSubscribtion.get(handler)) {
            this.subscriptions.remove(topic, subscription);
        }
        this.handlerToSubscribtion.removeAll(handler);
        this.checkSubscribtions(topic, future);
        return future;
    }

    public Future<Void> off(String topic) {
        Promise<Void> future = new DefaultPromise<>(this.eventLoop.next());
        ImmutableSet<MqttSubScription> subscriptions = ImmutableSet.copyOf(this.subscriptions.get(topic));
        for (MqttSubScription subscription : subscriptions) {
            for (MqttSubScription handSub : this.handlerToSubscribtion.get(subscription.getHandler())) {
                this.subscriptions.remove(topic, handSub);
            }
            this.handlerToSubscribtion.remove(subscription.getHandler(), subscription);
        }
        this.checkSubscribtions(topic, future);
        return future;
    }

    public IntObjectHashMap<MqttPendingSubscription> getMqttPendingSubscription() {
        return pendingSubscriptionIntObjectHashMap;
    }

    public MqttClientCallBack getCallback(){
        return  this.callback;
    }

    public void setCallback(MqttClientCallBack callBack){
        this.callback=callBack;
    }

   public  HashMultimap<MqttHandler, MqttSubScription> getHandlerToSubscribtion() {
        return handlerToSubscribtion;
    }

    public HashMultimap<String, MqttSubScription> getSubscriptions() {
        return subscriptions;
    }

    public Set<String> getPendingSubscribeTopics() {
        return pendingSubscribeTopics;
    }

    public Set<String> getServerSubscriptions() {
        return  serverSubStriptions;
    }

    public IntObjectHashMap<MqttPendingUnsubscription> getPendingServerUnsubscribes() {
        return unsubscriptionIntObjectHashMap;
    }


    private  ChannelFuture sendAndFlushPacket(Object message) {
        if (this.channel == null) {
            return null;
        }
        if (this.channel.isActive()) {
            return this.channel.writeAndFlush(message);
        }
        return this.channel.newFailedFuture(new ChannelClosedException("Channel is closed!"));
    }

    private MqttMessageIdVariableHeader getNewMessageId() {
        this.nextMessageId.compareAndSet(0xffff, 1);
        return MqttMessageIdVariableHeader.from(this.nextMessageId.getAndIncrement());
    }

    private Future<Void> createSubscription(String topic, MqttHandler handler, boolean once, MqttQoS qos) {
        if (this.pendingSubscribeTopics.contains(topic)) {
            Optional<Map.Entry<Integer, MqttPendingSubscription>> subscriptionEntry = this.pendingSubscriptions.entrySet().stream().filter((e) -> e.getValue().getTopic().equals(topic)).findAny();
            if (subscriptionEntry.isPresent()) {
                subscriptionEntry.get().getValue().addHandler(handler, once);
                return subscriptionEntry.get().getValue().getFuture();
            }
        }
        if (this.serverSubStriptions.contains(topic)) {
            MqttSubScription subscription = new MqttSubScription(topic, handler, once);
            this.subscriptions.put(topic, subscription);
            this.handlerToSubscribtion.put(handler, subscription);
            return this.channel.newSucceededFuture();
        }

        Promise<Void> future = new DefaultPromise<>(this.eventLoop.next());
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
        MqttTopicSubscription subscription = new MqttTopicSubscription(topic, qos);
        MqttMessageIdVariableHeader variableHeader = getNewMessageId();
        MqttSubscribePayload payload = new MqttSubscribePayload(Collections.singletonList(subscription));
        MqttSubscribeMessage message = new MqttSubscribeMessage(fixedHeader, variableHeader, payload);
        final MqttPendingSubscription pendingSubscription = new MqttPendingSubscription(future, topic, message);
        pendingSubscription.addHandler(handler, once);
        this.pendingSubscriptions.put(variableHeader.messageId(), pendingSubscription);
        this.pendingSubscribeTopics.add(topic);
        pendingSubscription.setSent(this.sendAndFlushPacket(message) != null); //If not sent, we will send it when the connection is opened
        pendingSubscription.startRetransmitTimer(this.eventLoop.next(), this::sendAndFlushPacket);
        return future;
    }

    private void checkSubscribtions(String topic, Promise<Void> promise) {
        if (!(this.subscriptions.containsKey(topic) && this.subscriptions.get(topic).size() != 0) && this.serverSubStriptions.contains(topic)) {
            MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
            MqttMessageIdVariableHeader variableHeader = getNewMessageId();
            MqttUnsubscribePayload payload = new MqttUnsubscribePayload(Collections.singletonList(topic));
            MqttUnsubscribeMessage message = new MqttUnsubscribeMessage(fixedHeader, variableHeader, payload);
            MqttPendingUnsubscription pendingUnsubscription = new MqttPendingUnsubscription(promise, topic, message);
            this.unsubscriptionIntObjectHashMap.put(variableHeader.messageId(), pendingUnsubscription);
            pendingUnsubscription.startRetransmissionTimer(this.eventLoop.next(), this::sendAndFlushPacket);

            this.sendAndFlushPacket(message);
        } else {
            promise.setSuccess(null);
        }
    }
    public void disconnect() {
        disconnected = true;
        if (this.channel != null) {
            MqttMessage message = new MqttMessage(new MqttFixedHeader(MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0));
            this.sendAndFlushPacket(message).addListener(future1 -> channel.close());
        }
    }

    public MqttConnectResult Run(MqttHandler handler,MqttClientCallBack callback) throws InterruptedException, ExecutionException {
        if(handler!=null) this.defaultHandler =handler;
        if(callback!=null)  this.callback = callback;
        MqttConnectResult result=this.connect(host,port).await().get();
        if(result.getReturnCode() != MqttConnectReturnCode.CONNECTION_ACCEPTED){
            this.disconnect();
        }
        return  result;
    }

    public MqttConnectResult Run(MqttHandler handler) throws InterruptedException, ExecutionException {
        MqttConnectResult result=Run(handler,null);
        return result;
    }

    public MqttConnectResult Run(MqttClientCallBack callback) throws InterruptedException, ExecutionException {
        MqttConnectResult result=Run(null,callback);
        return result;
    }

    public MqttConnectResult Run() throws InterruptedException, ExecutionException {
        MqttConnectResult result=Run(null,null);
        return result;
    }

    private class MqttChannelInitializer extends ChannelInitializer<SocketChannel> {

        private final Promise<MqttConnectResult> connectFuture;
        private final String host;
        private final int port;



        public MqttChannelInitializer(Promise<MqttConnectResult> connectFuture,
                                      String host,
                                      int port) {
            this.connectFuture = connectFuture;
            this.host = host;
            this.port = port;
        }

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline().addLast("mqttDecoder", new MqttDecoder());
            ch.pipeline().addLast("mqttEncoder", MqttEncoder.INSTANCE);
            ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(NettyMqttClient.this.timeoutSeconds, NettyMqttClient.this.timeoutSeconds, 0));
            ch.pipeline().addLast("mqttPingHandler", new MqttPingHandler(NettyMqttClient.this.timeoutSeconds));
            ch.pipeline().addLast("mqttHandler", new MqttChannelHandler(NettyMqttClient.this, connectFuture));
        }
    }

}
