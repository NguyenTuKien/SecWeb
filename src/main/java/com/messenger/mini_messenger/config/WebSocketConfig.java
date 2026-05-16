package com.messenger.mini_messenger.config;

import com.messenger.mini_messenger.websocket.JwtHandshakeInterceptor;
import com.messenger.mini_messenger.websocket.RealtimeMessageWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RealtimeMessageWebSocketHandler realtimeMessageWebSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    public WebSocketConfig(
            RealtimeMessageWebSocketHandler realtimeMessageWebSocketHandler,
            JwtHandshakeInterceptor jwtHandshakeInterceptor
    ) {
        this.realtimeMessageWebSocketHandler = realtimeMessageWebSocketHandler;
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(realtimeMessageWebSocketHandler, "/ws/messages")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
