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
import org.teavm.jso.JSArray;
import org.teavm.jso.JSObject;
import org.teavm.jso.spi.JSHost;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class JSRemoteHost implements JSHost {
    private static JSRemoteHost instance;
    private JSRemoteEndpoint endpoint;

    public JSRemoteHost() {
        if (instance == null) {
            throw new IllegalStateException("Only one instance can be created");
        }
        instance = this;
    }

    public static JSRemoteHost getInstance() {
        return instance;
    }

    public JSRemoteEndpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(JSRemoteEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    private void requireEndpoint() {
        if (endpoint == null) {
            throw new IllegalStateException("Endpoint not set");
        }
    }

    @Override
    public <T extends JSObject> JSArray<T> createArray(int size) {
        requireEndpoint();
        MessageSender sender = new MessageSender(endpoint);
        int messageId = endpoint.generateMessageId();
        try {
            sender.out().writeByte(JSRemoteEndpoint.CREATE_ARRAY);
            sender.out().writeInt(messageId);
            sender.send();
        } catch (IOException e) {
            throw new RuntimeException("Error creating JavaScript array", e);
        }
        WaitingFuture response = new WaitingFuture();
        endpoint.addFuture(messageId, response);
        @SuppressWarnings("unchecked")
        JSArray<T> safeArray = (JSArray<T>)response.get();
        return safeArray;
    }

    @Override
    public JSObject getTypeName(JSObject obj) {
        return null;
    }

    @Override
    public JSObject getGlobal() {
        return null;
    }

    @Override
    public JSObject wrap(String value) {
        return null;
    }

    @Override
    public JSObject wrap(char value) {
        return null;
    }

    @Override
    public JSObject wrap(int value) {
        return null;
    }

    @Override
    public JSObject wrap(float value) {
        return null;
    }

    @Override
    public JSObject wrap(double value) {
        return null;
    }

    @Override
    public JSObject wrap(boolean value) {
        return null;
    }

    @Override
    public boolean unwrapBoolean(JSObject obj) {
        return false;
    }

    @Override
    public int unwrapInt(JSObject obj) {
        return 0;
    }

    @Override
    public float unwrapFloat(JSObject obj) {
        return 0;
    }

    @Override
    public double unwrapDouble(JSObject obj) {
        return 0;
    }

    @Override
    public String unwrapString(JSObject obj) {
        return null;
    }

    @Override
    public char unwrapCharacter(JSObject obj) {
        return 0;
    }

    @Override
    public boolean isUndefined(JSObject obj) {
        return false;
    }

    @Override
    public JSObject invoke(JSObject instance, JSObject method, JSObject[] arguments) {
        return null;
    }

    @Override
    public JSObject instantiate(JSObject instance, JSObject method, JSObject[] arguments) {
        return null;
    }

    @Override
    public JSObject get(JSObject instance, JSObject index) {
        return null;
    }

    @Override
    public void set(JSObject instance, JSObject index, JSObject obj) {
    }

    @Override
    public JSObject function(JSObject instance, JSObject property) {
        return null;
    }
}
