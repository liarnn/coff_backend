package coffee_backend_4j.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class CoffeeWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Map<String, List<WebSocketSession>> activeConnections = new ConcurrentHashMap<>();

    public CoffeeWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String clientId = resolveClientId(session);
        session.getAttributes().put("client_id", clientId);
        activeConnections.computeIfAbsent(clientId, k -> Collections.synchronizedList(new ArrayList<>()));
        activeConnections.get(clientId).add(session);
        log.info("WebSocket connected: {}, current connections for this client: {}", clientId, activeConnections.get(clientId).size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("type", "pong");
        response.put("message", "心跳响应");
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String clientId = (String) session.getAttributes().get("client_id");
        if (clientId != null) {
            List<WebSocketSession> connections = activeConnections.get(clientId);
            if (connections != null) {
                connections.remove(session);
                if (connections.isEmpty()) {
                    activeConnections.remove(clientId);
                }
            }
            log.info("WebSocket closed: {}, remaining connections: {}", clientId, activeConnections.getOrDefault(clientId, Collections.emptyList()).size());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String clientId = (String) session.getAttributes().get("client_id");
        log.error("WebSocket transport error for client {}: {}", clientId, exception.getMessage());
        removeSession(session, clientId);
    }

    public void broadcast(Map<String, Object> message) {
        for (Map.Entry<String, List<WebSocketSession>> entry : activeConnections.entrySet()) {
            List<WebSocketSession> toRemove = new ArrayList<>();
            for (WebSocketSession session : entry.getValue()) {
                if (!sendToSession(session, message)) {
                    toRemove.add(session);
                }
            }
            for (WebSocketSession session : toRemove) {
                removeSession(session, entry.getKey());
            }
        }
    }

    public void sendToClient(String clientId, Map<String, Object> message) {
        List<WebSocketSession> connections = activeConnections.get(clientId);
        if (connections != null) {
            List<WebSocketSession> toRemove = new ArrayList<>();
            for (WebSocketSession session : connections) {
                if (!sendToSession(session, message)) {
                    toRemove.add(session);
                }
            }
            for (WebSocketSession session : toRemove) {
                removeSession(session, clientId);
            }
        }
    }

    private boolean sendToSession(WebSocketSession session, Map<String, Object> message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
                return true;
            }
        } catch (Exception e) {
            log.warn("WebSocket send failed: {}", e.getMessage());
        }
        return false;
    }

    private void removeSession(WebSocketSession session, String clientId) {
        if (clientId == null) return;
        List<WebSocketSession> connections = activeConnections.get(clientId);
        if (connections != null) {
            connections.remove(session);
            if (connections.isEmpty()) {
                activeConnections.remove(clientId);
            }
        }
    }

    public int getConnectionCount() {
        return activeConnections.values().stream().mapToInt(List::size).sum();
    }

    private String resolveClientId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri != null) {
            String clientId = UriComponentsBuilder.fromUri(uri)
                    .build()
                    .getQueryParams()
                    .getFirst("client_id");
            if (clientId != null && !clientId.isBlank()) {
                return clientId;
            }
        }
        return "client_" + session.getId();
    }
}
