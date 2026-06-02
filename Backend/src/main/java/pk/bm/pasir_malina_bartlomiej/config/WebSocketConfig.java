package pk.bm.pasir_malina_bartlomiej.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket  // czysty WebSocket, bez STOMP/SockJS
public class WebSocketConfig implements WebSocketConfigurer {

    private final GroupNotificationHandler groupNotificationHandler;

    public WebSocketConfig(GroupNotificationHandler groupNotificationHandler) {
        this.groupNotificationHandler = groupNotificationHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(groupNotificationHandler, "/ws/group-notifications")
                .setAllowedOriginPatterns("*");
        // Bez .withSockJS() — frontend używa surowego new WebSocket()
    }
}
