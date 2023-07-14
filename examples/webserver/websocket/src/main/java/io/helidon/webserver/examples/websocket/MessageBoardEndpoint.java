/*
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.webserver.examples.websocket;

import java.util.logging.Logger;

import io.helidon.nima.websocket.WsListener;
import io.helidon.nima.websocket.WsSession;

/**
 * Class MessageBoardEndpoint.
 */
public class MessageBoardEndpoint implements WsListener {
    private static final Logger LOGGER = Logger.getLogger(MessageBoardEndpoint.class.getName());

    private final MessageQueue messageQueue = MessageQueue.instance();

    @Override
    public void onMessage(WsSession session, String text, boolean last) {
        // Send all messages in the queue
        if (text.equals("send")) {
            while (!messageQueue.isEmpty()) {
                session.send(messageQueue.pop(), last);
            }
        }
    }
}
