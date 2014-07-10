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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.websocket.ClientEndpoint;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
@ClientEndpoint
public class JSRemoteEndpoint extends JSMessageExchange {
    Session session;

    @OnOpen
    public void open(Session session) {
        this.session = session;
    }

    @OnMessage
    @Override
    public void receive(InputStream inputStream) throws IOException {
        super.receive(inputStream);
    }

    @Override
    public void send(byte[] bytes) {
        session.getAsyncRemote().sendBinary(ByteBuffer.wrap(bytes));
    }
}
