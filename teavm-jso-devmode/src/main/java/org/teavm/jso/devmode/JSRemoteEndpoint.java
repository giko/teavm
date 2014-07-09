/*
 *  Copyright 2014 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.jso.devmode;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.websocket.ClientEndpoint;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
@ClientEndpoint
public class JSRemoteEndpoint {
    public static final byte RECEIVE_VALUE = 0;
    public static final byte CREATE_ARRAY = 1;
    private final AtomicInteger messageIdGenerator = new AtomicInteger();
    private ConcurrentMap<Integer, WaitingFuture> futures = new ConcurrentHashMap<>();
    Session session;

    @OnOpen
    public void open(Session session) {
        this.session = session;
    }

    @OnMessage
    public void receive(InputStream inputStream) throws IOException {
        DataInput input = new DataInputStream(inputStream);
        switch (input.readByte()) {
            case RECEIVE_VALUE: {
                int messageId = input.readInt();
                int objectId = input.readInt();
                futures.get(messageId).set(new JSRemoteObject(objectId));
            }
        }
    }

    public <T> void addFuture(int id, WaitingFuture future) {
        futures.put(id, future);
    }

    public int generateMessageId() {
        return messageIdGenerator.incrementAndGet();
    }
}
