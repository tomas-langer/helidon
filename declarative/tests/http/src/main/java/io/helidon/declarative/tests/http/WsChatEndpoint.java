package io.helidon.declarative.tests.http;

import java.util.concurrent.locks.ReentrantLock;

import io.helidon.http.Http;
import io.helidon.service.inject.api.Injection;
import io.helidon.webserver.websocket.WebSocketServer;
import io.helidon.websocket.WebSocket;
import io.helidon.websocket.WsSession;

@WebSocketServer.Endpoint
@Injection.Singleton
@Http.Path("/ws/{user}")
class WsChatEndpoint {
    private static final System.Logger LOGGER = System.getLogger(WsChatEndpoint.class.getName());

    private final WsChatService chatService;
    private final ReentrantLock sendLock = new ReentrantLock();

    private volatile WsSession session;

    @Injection.Inject
    WsChatEndpoint(WsChatService chatService) {
        this.chatService = chatService;
    }

    @WebSocket.OnOpen
    void onOpen(WsSession session,
                @Http.PathParam("username") String username) {

        this.session = session;
        this.chatService.addSession(session, this, username);

        broadcast(username, new Message("Welcome " + username));
    }

    @WebSocket.MaxMessageSize(1024)
    @WebSocket.OnMessage
    void onMessage(WsSession session,
                   Message message) {
        broadcast(chatService.user(session), message);
    }

    @WebSocket.OnError
    void onError(WsSession session, Throwable throwable) {
        LOGGER.log(System.Logger.Level.WARNING,
                   "There was a problem with session: " + session.socketContext().childSocketId(),
                   throwable);
    }

    @WebSocket.OnClose
    void onClose(WsSession session, WebSocket.CloseReason closeReason) {
        String user = chatService.user(session);
        chatService.removeSession(session);

        broadcast(user, new Message("Disconnected from the server"));
    }

    private void send(String user, Message message) {
        sendLock.lock();
        try {
            session.send(user + ": " + message.message(), true);
        } finally {
            sendLock.unlock();
        }
    }

    private void broadcast(String username,
                           Message message) {
        chatService.forEachEndpoint(it -> it.send(username, message));
    }

    record Message(String message) {

    }
}
