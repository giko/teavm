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

/**
 *
 * @author Alexey Andreev
 */
public abstract class JSMessageExchange implements JSMessageSender {
    public static final byte RECEIVE_VALUE = 0;
    public static final byte RECEIVE_EXCEPTION = 1;
    public static final byte CREATE_ARRAY = 2;
    public static final byte GET_TYPE_NAME = 3;
    public static final byte INVOKE_METHOD = 4;
    public static final byte INSTANTIATE_CLASS = 5;
    public static final byte GET_JAVA_OBJECT_INFO = 6;
    public static final byte GET_PROPERTY = 7;
    public static final byte SET_PROPERTY = 8;
    private final AtomicInteger messageIdGenerator = new AtomicInteger();
    private ConcurrentMap<Integer, WaitingFuture> futures = new ConcurrentHashMap<>();
    private JavaObjectRepository javaObjects = new JavaObjectRepository();

    public void receive(InputStream inputStream) throws IOException {
        DataInput input = new DataInputStream(inputStream);
        switch (input.readByte()) {
            case RECEIVE_VALUE: {
                int messageId = input.readInt();
                int objectId = input.readInt();
                futures.get(messageId).set(new JSRemoteObject(objectId));
                break;
            }
            case RECEIVE_EXCEPTION: {
                int messageId = input.readInt();
                String exceptionMesage = input.readUTF();
                futures.get(messageId).setException(new JSRemoteException(exceptionMesage));
                break;
            }
            case GET_JAVA_OBJECT_INFO: {
                int messageId = input.readInt();
                break;
            }
        }
    }

    @Override
    public abstract void send(byte[] bytes);

    public void addFuture(int id, WaitingFuture future) {
        futures.put(id, future);
    }

    public int addFuture(WaitingFuture future) {
        int id = generateMessageId();
        addFuture(id, future);
        return id;
    }

    public int generateMessageId() {
        return messageIdGenerator.incrementAndGet();
    }

    public JavaObjectRepository getJavaObjects() {
        return javaObjects;
    }
}
