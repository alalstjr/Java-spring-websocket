-----------------------------------
# Spring Boot WebSocket 제작 공부 노트
-----------------------------------

# 목차

- [1. iOS 내부에는 어떠한 것들이 들어 있을까?](#iOS-내부에는-어떠한-것들이-들어-있을까?)
    - [1-1. Core OS](#Core-OS)
    - [1-2. Core Services](#Core-Services)
    - [1-3. Media](#Media)
    - [1-3. Cocoa Touch](#Cocoa-Touch)

# 설명

WebSocket을은입니다 양방향 , 전이중 , 영구 연결 웹 브라우저와 서버 사이.  
WebSocket 연결이 설정되면 클라이언트 또는 서버가 연결을 종료하기로 결정할 때까지 연결이 열린 상태로 유지됩니다.  
일반적인 사용 사례는 채팅에서와 같이 앱에 여러 사용자가 서로 통신하는 경우 일 수 있습니다.  
이 예제에서는 간단한 채팅 클라이언트를 만들 것입니다.

# Maven 종속성

~~~
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-websocket</artifactId>
    <version>5.3.4</version>
</dependency>

<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-messaging</artifactId>
    <version>5.3.4</version>
</dependency>
~~~

또한 JSON 을 사용하여 메시지 본문을 작성할 것이므로 Jackson 종속성 을 추가해야합니다. 
이를 통해 Spring은 Java 객체를 JSON 으로 /에서 변환 할 수 있습니다.

~~~
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-core</artifactId>
    <version>2.10.2</version>
</dependency>

<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.10.2</version>
</dependency>
~~~

# Spring에서 WebSocket 활성화

가장 먼저 할 일은 WebSocket 기능을 활성화하는 것입니다.   
이렇게 하려면 애플리케이션에 구성을 추가하고 이 클래스에 @EnableWebSocketMessageBroker 주석을 추가해야합니다.  
이름에서 알 수 있듯이 메시지 브로커가 지원하는 WebSocket 메시지 처리를 활성화합니다.

[AbstractWebSocketMessageBrokerConfigurer 추상 클래스는 5.0 부터 사용하지 않습니다.](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/socket/config/annotation/AbstractWebSocketMessageBrokerConfigurer.html)

이전에는 AbstractWebSocketMessageBrokerConfigurer 추상 클래스를 상속받아 사용했지만  
5.0 버전 이후 부터는 `WebSocketMessageBrokerConfigurer` 인터페이스를 상속받아 사용하도록 변경 되었습니다.   
`WebSocketMessageBrokerConfigurer` 인터페이스는 선택적 메서드에 빈 메서드 구현을 제공하는 구현을 위한 편리한 추상 기본 클래스입니다.

~~~
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/chat");
        registry.addEndpoint("/chat").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
~~~

여기에서 configureMessageBroker 메소드가 메시지 브로커 를 구성하는 데 사용되는 것을 수 있습니다.  
먼저, 메모리 내 메시지 브로커가 `"/topic"`접두사가 붙은 대상에서 클라이언트로 메시지를 다시 전달할 수 있도록 합니다.

> topic : 두 프로그램 사이에 어떤 통신 경로가 있다는 뜻

@MessageMapping 을 통해 응용 프로그램 주석 처리 된 
메서드를 대상으로하는 대상을 필터링하기 위해 "/app" 접두사를 지정하여 간단한 구성을 완료합니다.

registerStompEndpoints 메소드는 "/chat" 엔드 포인트를 등록하여 
Spring 의 `STOMP` 지원을 활성화합니다. 

> STOMP 란?  
> STOMP 는 Simple/Streaming Text Oriented Messaging Protocol 의 약자이다.  
> 텍스트 기반의 메세징 프로토콜 이다. 유사한 프로토콜로는 OASIS 표준으로 선정된  
> AMQP(Advanced Message Queuing protocol)이 있다. 웹 소켓을 지원한다.  
> TCP나 WebSocket 과 같은 신뢰성있는 양방향 streaming network protocol 상에 사용 될 수 있다.  
> HTTP 에 모델링된 frame 기반 프로토콜이다.

탄력성을 위해 SockJS 없이 작동하는 엔드 포인트도 여기에 추가하고 있음을 명심하십시오.
"/app" 접두사가 붙은이 끝점은 `ChatController.send() 메서드가 처리하도록 매핑되는 엔드 포인트`입니다.
또한 SockJS 대체 옵션을 활성화하여 WebSocket 을 사용할 수 없는 경우 대체 메시징 옵션을 사용할 수 있습니다.   
WebSocket 은 아직 `모든 브라우저에서 지원되지 않으며 제한적인 네트워크 프록시로 인해 제외 될 수 있기 때문에 유용`합니다.
폴백을 통해 애플리케이션은 WebSocket API 를 사용할 수 있지만  
런타임에 필요할 때 WebSocket 이 아닌 대안으로 정상적으로 저하됩니다.

# 메시지 모델 만들기

이제 프로젝트를 설정하고 WebSocket 기능을 구성 했으므로 보낼 메시지를 만들어야합니다.  
엔드 포인트 는 보낸 사람 이름과 본문이 JSON 개체 인 STOMP 메시지의 텍스트를 포함하는 메시지를 수락 합니다.  
메시지는 다음과 같습니다.

~~~
{
    "from": "John",
    "text": "Hello!"
}
~~~

텍스트를 전달하는 메시지를 모델링하기 위해 from 및 text 속성을 사용하여 간단한 Java 개체를 만들 수 있습니다 .

~~~
public class Message {

    private String from;
    private String text;

    // getters and setters
}
~~~

기본적으로 Spring은 Jackson 라이브러리를 사용하여 모델 객체를 JSON으로 변환합니다.

# 메시지 처리 컨트롤러 만들기

우리가 본 것처럼 STOMP 메시징을 사용하는 Spring 의 접근 방식은 컨트롤러 메서드로 구성된 엔드 포인트에 연결하는 것입니다.  
이것은 @MessageMapping 주석을 통해 가능합니다.

엔드 포인트와 컨트롤러 간의 연결을 통해 필요한 경우 메시지를 처리 할 수 있습니다.

~~~
@MessageMapping("/chat")
@SendTo("/topic/messages")
public OutputMessage send(Message message) throws Exception {
    String time = new SimpleDateFormat("HH:mm").format(new Date());
    return new OutputMessage(message.getFrom(), message.getText(), time);
}
~~~

전송 된 출력 메시지를 나타내는 OutputMessage 라는 또 다른 모델 개체를 만들 것입니다.  
우리는 개체를 보낸 사람과 들어오는 메시지에서 가져온 메시지 텍스트로 채우고 타임 스탬프로 시간 정보를 체크합니다.  
메시지를 처리 한 후 @SendTo 주석으로 정의 된 적절한 대상으로 메시지를 보냅니다.  
"/topic/messages" 대상에 대한 모든 가입자는 메시지를 받습니다.

# 브라우저 클라이언트 생성

서버 측에서 구성을 만든 후에는 sockjs-client 라이브러리 를 사용하여  
메시징 시스템과 상호 작용하는 간단한 HTML 페이지를 만들 것입니다.  
우선 sockjs 와 stomp 자바 스크립트 클라이언트 라이브러리 를 가져와야합니다.  
다음으로 엔드 포인트와의 통신을 열기 위한 connect() 함수,  
STOMP 메시지를 전송 하는 sendMessage() 함수 및 통신을 종료 하는 disconnect() 함수를 만들 수 있습니다.

> properties

~~~
# resources url setting
spring.mvc.static-path-pattern=/static/**
~~~

~~~
<html>
<head>
  <title>Chat WebSocket</title>
  <script src="/static/js/sockjs-0.3.4.js"></script>
  <script src="/static/js/stomp.js"></script>
  <script type="text/javascript">
    var stompClient = null;

    function setConnected(connected) {
      document.getElementById('connect').disabled = connected;
      document.getElementById('disconnect').disabled = !connected;
      document.getElementById('conversationDiv').style.visibility
          = connected ? 'visible' : 'hidden';
      document.getElementById('response').innerHTML = '';
    }

    function connect() {
      var socket = new SockJS('/chat');
      stompClient = Stomp.over(socket);
      stompClient.connect({}, function(frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/messages', function(messageOutput) {
          showMessageOutput(JSON.parse(messageOutput.body));
        });
      });
    }

    function disconnect() {
      if(stompClient != null) {
        stompClient.disconnect();
      }
      setConnected(false);
      console.log("Disconnected");
    }

    function sendMessage() {
      var from = document.getElementById('from').value;
      var text = document.getElementById('text').value;
      stompClient.send("/app/chat", {},
          JSON.stringify({'from':from, 'text':text}));
    }

    function showMessageOutput(messageOutput) {
      var response = document.getElementById('response');
      var p = document.createElement('p');
      p.style.wordWrap = 'break-word';
      p.appendChild(document.createTextNode(messageOutput.from + ": "
          + messageOutput.text + " (" + messageOutput.time + ")"));
      response.appendChild(p);
    }
  </script>
</head>
<body onload="disconnect()">
<div>
  <div>
    <input type="text" id="from" placeholder="Choose a nickname"/>
  </div>
  <br />
  <div>
    <button id="connect" onclick="connect();">Connect</button>
    <button id="disconnect" disabled="disabled" onclick="disconnect();">
      Disconnect
    </button>
  </div>
  <br />
  <div id="conversationDiv">
    <input type="text" id="text" placeholder="Write a message..."/>
    <button id="sendMessage" onclick="sendMessage();">Send</button>
    <p id="response"></p>
  </div>
</div>

</body>
</html>
~~~
