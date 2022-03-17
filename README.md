# netty-mqttclient

this client is base on netty,implement subscription method  only

### Add dependency in your project

    <dependency>
       <groupId>com.nmqtt</groupId>
       <artifactId>spring-boot-nettymqttclient-starter</artifactId>
       <version>1.0.0.1</version>
    </dependency>
 
 ### set config in application.properties
 
mqtt.host=127.0.0.1<br>
mqtt.port=1883<br>
mqtt.userName=test<br>
mqtt.passWord=test123<br>
mqtt.qos=1<br>
mqtt.topic=CBYU_1001<br>
mqtt.clientId=lhsjyfj20211001<br>
mqtt.retain=false<br>
mqtt.timeoutSeconds=5<br>
mqtt.isReconnect=true<br>

```Java
import com.nmqtt.interfaces.MqttClientCallBack;
  
public class ClientCallBack implements MqttClientCallBack {

    public void afterReconnect() {
        System.out.println("reconnect sucessfully");
    }

    public void onLostConnection(Throwable throwable) {
        System.out.println("miss connection");
    }
}
```

```Java
import com.nmqtt.interfaces.MqttHandler;
import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

public class ClientHandler implements MqttHandler {
    public void onMessage(String s, ByteBuf byteBuf) {
        String str=byteBuf.toString(CharsetUtil.UTF_8);
        System.out.println("topic:"+s);
        System.out.println("payload:"+str);
    }
}
```

```Java
 @Autowired
    private NettyMqttClient nettyMqttClient;

    public void  ClientStart() throws ExecutionException, InterruptedException {
//        nettyMqttClient.setMqttHandler(new ClientHandler());
//        nettyMqttClient.setCallback(new ClientCallBack());
//        MqttConnectResult result=nettyMqttClient.Run();

        MqttConnectResult result= nettyMqttClient.Run(new ClientHandler(),new ClientCallBack());

        System.out.print(result.getReturnCode());
    }
 ```
