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
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
@ServerEndpoint("/")
public class JSRemoteEndpoint extends JSMessageExchange {
    private static Class<?> mainClass;
    Session session;

    public static void setMainClass(Class<?> mainClass) {
        JSRemoteEndpoint.mainClass = mainClass;
    }

    @OnOpen
    public void open(Session session) throws ReflectiveOperationException {
        this.session = session;
        init();
        Method method = mainClass.getMethod("main", String[].class);
        method.invoke(null, new Object[] { new String[0] });
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
