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
    private JSMessageExchange exchange;

    public JSRemoteHost() {
        if (instance == null) {
            throw new IllegalStateException("Only one instance can be created");
        }
        instance = this;
    }

    public static JSRemoteHost getInstance() {
        return instance;
    }

    public void setExchange(JSMessageExchange exchange) {
        this.exchange = exchange;
    }

    private JSDataMessageSender createSender() {
        if (exchange == null) {
            throw new IllegalStateException("Exchange not set");
        }
        return new JSDataMessageSender(exchange);
    }

    @Override
    public <T extends JSObject> JSArray<T> createArray(int size) {
        JSDataMessageSender sender = createSender();
        WaitingFuture response = new WaitingFuture();
        int messageId = exchange.addFuture(response);
        try {
            sender.out().writeByte(JSRemoteEndpoint.CREATE_ARRAY);
            sender.out().writeInt(messageId);
            sender.send();
        } catch (IOException e) {
            throw new RuntimeException("Error creating JavaScript array", e);
        }
        @SuppressWarnings("unchecked")
        JSArray<T> safeArray = (JSArray<T>)response.get();
        return safeArray;
    }

    @Override
    public JSObject getTypeName(JSObject obj) {
        if (!(obj instanceof JSRemoteValue)) {
            throw new IllegalArgumentException("This object is not ");
        }
        TypeExtractionVisitor visitor = new TypeExtractionVisitor();
        JSRemoteValue remoteValue = (JSRemoteValue)obj;
        try {
            remoteValue.acceptVisitor(visitor);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new JSRemoteException("Error extracting type of a value", e);
        }
        return visitor.result;
    }

    private class TypeExtractionVisitor implements JSRemoteValueVisitor {
        JSObject result;

        @Override
        public void visit(JSRemoteObject value) throws Exception {
            JSDataMessageSender sender = createSender();
            WaitingFuture response = new WaitingFuture();
            int messageId = exchange.addFuture(response);
            try {
                sender.out().writeByte(JSRemoteEndpoint.CREATE_ARRAY);
                sender.out().writeInt(messageId);
                sender.out().writeInt(value.getId());
            } catch (IOException e) {
                throw new RuntimeException("Error getting JavaScript type", e);
            }
            result = new JSRemoteString((String)response.get());
        }

        @Override public void visit(JSGlobalObject value) throws Exception {
            result = new JSRemoteString("object");
        }
        @Override public void visit(JSRemoteNumber value) throws Exception {
            result = new JSRemoteString("number");
        }
        @Override public void visit(JSRemoteBoolean value) throws Exception {
            result = new JSRemoteString("boolean");
        }
        @Override public void visit(JSRemoteString value) throws Exception {
            result = new JSRemoteString("string");
        }
        @Override public void visit(JSRemoteUndefined value) throws Exception {
            result = new JSRemoteString("undefined");
        }
        @Override public void visit(JSRemoteNull value) throws Exception {
            result = new JSRemoteString("object");
        }
    }

    @Override
    public JSObject getGlobal() {
        return JSGlobalObject.getInstance();
    }

    @Override
    public JSObject wrap(String value) {
        return new JSRemoteString(value);
    }

    @Override
    public JSObject wrap(char value) {
        return new JSRemoteNumber(value);
    }

    @Override
    public JSObject wrap(int value) {
        return new JSRemoteNumber(value);
    }

    @Override
    public JSObject wrap(float value) {
        return new JSRemoteNumber(value);
    }

    @Override
    public JSObject wrap(double value) {
        return new JSRemoteNumber(value);
    }

    @Override
    public JSObject wrap(boolean value) {
        return JSRemoteBoolean.valueOf(value);
    }

    @Override
    public boolean unwrapBoolean(JSObject obj) {
        if (!(obj instanceof JSRemoteBoolean)) {
            throw new IllegalArgumentException("The given JavaScript value is not boolean");
        }
        return ((JSRemoteBoolean)obj).getValue();
    }

    @Override
    public int unwrapInt(JSObject obj) {
        if (!(obj instanceof JSRemoteNumber)) {
            throw new IllegalArgumentException("The given JavaScript value is not numeric");
        }
        double number = ((JSRemoteNumber)obj).getValue();
        if ((int)number != number) {
            throw new RuntimeException("The given JavaScript number is not integer");
        }
        return (int)number;
    }

    @Override
    public float unwrapFloat(JSObject obj) {
        if (!(obj instanceof JSRemoteNumber)) {
            throw new IllegalArgumentException("The given JavaScript value is not numeric");
        }
        return (float)((JSRemoteNumber)obj).getValue();
    }

    @Override
    public double unwrapDouble(JSObject obj) {
        if (!(obj instanceof JSRemoteNumber)) {
            throw new IllegalArgumentException("The given JavaScript value is not numeric");
        }
        return ((JSRemoteNumber)obj).getValue();
    }

    @Override
    public String unwrapString(JSObject obj) {
        if (!(obj instanceof JSRemoteString)) {
            throw new IllegalArgumentException("The given JavaScript value is not a string");
        }
        return ((JSRemoteString)obj).getValue();
    }

    @Override
    public char unwrapCharacter(JSObject obj) {
        if (!(obj instanceof JSRemoteNumber)) {
            throw new IllegalArgumentException("The given JavaScript value is not a character code");
        }
        double number = ((JSRemoteNumber)obj).getValue();
        int integer = (int)number;
        if (integer != number || integer < 0 || integer >= Character.MAX_VALUE) {
            throw new RuntimeException("The given JavaScript number is not a valid character code");
        }
        return (char)integer;
    }

    @Override
    public boolean isUndefined(JSObject obj) {
        return obj instanceof JSRemoteUndefined;
    }

    @Override
    public JSObject invoke(JSObject instance, JSObject method, JSObject[] arguments) {
        if (!(instance instanceof JSRemoteValue)) {
            // TODO: handle invocation of
            return null;
        } else {
            JSDataMessageSender sender = createSender();
            WaitingFuture response = new WaitingFuture();
            int messageId = exchange.addFuture(response);
            JSRemoteValueSender valueSender = new JSRemoteValueSender(sender.out(), exchange.getJavaObjects());
            try {
                sender.out().writeByte(JSMessageExchange.INVOKE_METHOD);
                sender.out().writeInt(messageId);
                valueSender.send(instance);
                valueSender.send(method);
                sender.out().writeInt(arguments.length);
                for (int i = 0; i < arguments.length; ++i) {
                    valueSender.send(arguments[i]);
                }
                sender.send();
            } catch (IOException e) {
                throw new RuntimeException("Error calling remote method");
            }
            return (JSObject)response.get();
        }
    }

    @Override
    public JSObject instantiate(JSObject instance, JSObject method, JSObject[] arguments) {
        if (!(instance instanceof JSRemoteValue)) {
            throw new RuntimeException("Can't call non-JavaScript constructor");
        } else {
            JSDataMessageSender sender = createSender();
            WaitingFuture response = new WaitingFuture();
            int messageId = exchange.addFuture(response);
            JSRemoteValueSender valueSender = new JSRemoteValueSender(sender.out(), exchange.getJavaObjects());
            try {
                sender.out().writeByte(JSMessageExchange.INSTANTIATE_CLASS);
                sender.out().writeInt(messageId);
                valueSender.send(instance);
                valueSender.send(method);
                sender.out().writeInt(arguments.length);
                for (int i = 0; i < arguments.length; ++i) {
                    valueSender.send(arguments[i]);
                }
                sender.send();
            } catch (IOException e) {
                throw new RuntimeException("Error calling remote constructor");
            }
            return (JSObject)response.get();
        }
    }

    @Override
    public JSObject get(JSObject instance, JSObject index) {
        if (!(instance instanceof JSRemoteValue)) {
            // TODO: handle invocation of
            return null;
        } else {
            JSDataMessageSender sender = createSender();
            WaitingFuture response = new WaitingFuture();
            int messageId = exchange.addFuture(response);
            JSRemoteValueSender valueSender = new JSRemoteValueSender(sender.out(), exchange.getJavaObjects());
            try {
                sender.out().writeByte(JSMessageExchange.INVOKE_METHOD);
                sender.out().writeInt(messageId);
                valueSender.send(instance);
                valueSender.send(index);
                sender.send();
            } catch (IOException e) {
                throw new RuntimeException("Error calling remote method");
            }
            return (JSObject)response.get();
        }
    }

    @Override
    public void set(JSObject instance, JSObject index, JSObject obj) {
        if (!(instance instanceof JSRemoteValue)) {
            // TODO: handle invocation of
        } else {
            JSDataMessageSender sender = createSender();
            WaitingFuture response = new WaitingFuture();
            int messageId = exchange.addFuture(response);
            JSRemoteValueSender valueSender = new JSRemoteValueSender(sender.out(), exchange.getJavaObjects());
            try {
                sender.out().writeByte(JSMessageExchange.INVOKE_METHOD);
                sender.out().writeInt(messageId);
                valueSender.send(instance);
                valueSender.send(index);
                sender.send();
            } catch (IOException e) {
                throw new RuntimeException("Error calling remote method");
            }
            response.get();
        }
    }

    @Override
    public JSObject function(JSObject instance, JSObject property) {
        return null;
    }
}
