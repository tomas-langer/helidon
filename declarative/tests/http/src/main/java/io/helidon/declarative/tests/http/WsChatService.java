package io.helidon.declarative.tests.http;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import io.helidon.service.inject.api.Injection;
import io.helidon.websocket.WsSession;

/**
 * Helidon implementation of
 * <a
 * href="https://blogs.oracle.com/javamagazine/post/how-to-build-applications-with-the-websocket-api-for-java-ee-and-jakarta-ee
 * ">WebSocket example</a>
 */
@Injection.Singleton
class WsChatService {
    // session id to user
    private final Map<String, String> users = new HashMap<>();
    // session id to session
    private final Map<String, WsChatEndpoint> endpoints = new HashMap<>();

    WsChatService() {
    }

    public void addSession(WsSession session, WsChatEndpoint endpoint, String username) {
        String socketId = session.socketContext().childSocketId();
        users.put(socketId, username);
        endpoints.put(socketId, endpoint);
    }

    public String user(WsSession session) {
        return users.get(session.socketContext().childSocketId());
    }

    public void removeSession(WsSession session) {
        users.remove(session.socketContext().childSocketId());
        endpoints.remove(session.socketContext().childSocketId());
    }

    void forEachEndpoint(Consumer<WsChatEndpoint> consumer) {
        endpoints.values().forEach(consumer);
    }

    @Injection.PreDestroy
    void preDestroy() {
        users.clear();
    }
}
