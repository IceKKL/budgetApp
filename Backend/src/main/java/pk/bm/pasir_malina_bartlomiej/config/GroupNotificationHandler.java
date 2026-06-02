package pk.bm.pasir_malina_bartlomiej.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import pk.bm.pasir_malina_bartlomiej.dto.GroupNotificationDTO;
import pk.bm.pasir_malina_bartlomiej.security.JwtUtil;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Obsługuje połączenia WebSocket od frontendu.
 * Frontend łączy się przez: new WebSocket("ws://localhost:8080/ws/group-notifications?token=JWT")
 * Po połączeniu sesja jest przechowywana w mapie email → sesja.
 * GroupTransactionService wywołuje sendNotification(email, dto) żeby wysłać wiadomość.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupNotificationHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    // Mapa: email użytkownika → jego aktywna sesja WebSocket
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String email = extractEmailFromSession(session);
        if (email == null) {
            log.warn("WebSocket: odmowa połączenia — brak lub nieprawidłowy token");
            try {
                session.close(CloseStatus.NOT_ACCEPTABLE);
            } catch (IOException e) {
                log.error("Błąd zamykania sesji WebSocket", e);
            }
            return;
        }
        sessions.put(email, session);
        log.info("WebSocket: połączono użytkownika {}", email);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String email = extractEmailFromSession(session);
        if (email != null) {
            sessions.remove(email);
            log.info("WebSocket: rozłączono użytkownika {}", email);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Frontend nie wysyła wiadomości — tylko nasłuchuje; ignorujemy przychodzące
    }

    /**
     * Wysyła powiadomienie do konkretnego użytkownika po jego emailu.
     * Wywoływane z GroupTransactionService po dodaniu wydatku grupowego.
     */
    public void sendNotification(String recipientEmail, GroupNotificationDTO notification) {
        WebSocketSession session = sessions.get(recipientEmail);
        if (session == null || !session.isOpen()) {
            log.debug("WebSocket: użytkownik {} nie jest połączony, pomijam powiadomienie", recipientEmail);
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(notification);
            session.sendMessage(new TextMessage(json));
            log.info("WebSocket: wysłano powiadomienie do {}", recipientEmail);
        } catch (IOException e) {
            log.error("WebSocket: błąd wysyłania do {}", recipientEmail, e);
        }
    }

    /**
     * Wyciąga email z JWT przekazanego jako ?token= w URL połączenia.
     */
    private String extractEmailFromSession(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;
        String query = uri.getQuery();
        if (query == null) return null;

        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                String token = param.substring("token=".length());
                try {
                    if (jwtUtil.validateToken(token)) {
                        return jwtUtil.extractUsername(token);
                    }
                } catch (Exception e) {
                    log.warn("WebSocket: nieprawidłowy token JWT");
                }
                return null;
            }
        }
        return null;
    }
}
