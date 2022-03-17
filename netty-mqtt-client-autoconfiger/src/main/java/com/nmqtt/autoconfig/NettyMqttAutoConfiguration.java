package com.nmqtt.autoconfig;

import com.nmqtt.NettyMqttClient;
import com.nmqtt.models.MqttConfigProperties;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
@EnableConfigurationProperties(MqttConfigProperties.class)
@SuppressWarnings({"WeakerAccess", "unused"})
public class NettyMqttAutoConfiguration {

    @Resource
    private  MqttConfigProperties mqttConfigProperties;


    @ConditionalOnMissingBean(NettyMqttClient.class)
    @Bean
    public NettyMqttClient nettyMqttClient(){
        EventLoopGroup eventLoopGroup=new NioEventLoopGroup();
        NettyMqttClient client=new NettyMqttClient(mqttConfigProperties);
        client.setEventLoop(eventLoopGroup);
        return client;
    }
}
