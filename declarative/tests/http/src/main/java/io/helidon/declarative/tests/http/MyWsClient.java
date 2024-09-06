package io.helidon.declarative.tests.http;

import java.util.concurrent.CountDownLatch;

import io.helidon.webclient.websocket.WsClient;
import io.helidon.websocket.WsCloseCodes;
import io.helidon.websocket.WsListener;
import io.helidon.websocket.WsSession;

public class MyWsClient {
    public static void main(String[] args) throws InterruptedException {
        WsClient wsClient = WsClient.builder()
                .baseUri("http://localhost:8080")
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        wsClient.connect("/ws/Tomas", new WsListener() {
            @Override
            public void onMessage(WsSession session, String text, boolean last) {
                System.out.println("Received: " + text);
                session.close(WsCloseCodes.NORMAL_CLOSE, "Done");
                latch.countDown();
            }
        });

        latch.await();
    }
}
