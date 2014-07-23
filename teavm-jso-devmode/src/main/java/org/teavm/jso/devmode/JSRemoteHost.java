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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teavm.jso.JSObject;
import org.teavm.jso.devmode.values.*;
import org.teavm.jso.spi.JSHost;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class JSRemoteHost implements JSHost {
    private static final Logger logger = LoggerFactory.getLogger(JSRemoteHost.class);
    private static JSRemoteHost instance;
    private JSMessageExchange exchange;

    public JSRemoteHost() {
        if (instance != null) {
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
    public JSObject getTypeName(JSObject obj) {
        if (obj == null) {
            return new JSRemoteString("null");
        }
        if (!(obj instanceof JSRemoteValue)) {
            throw new IllegalArgumentException("This object is not a JavaScript object");
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
            result = new JSRemoteString("object");
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

    private JSRemoteValueSender createValueSender(JSDataMessageSender sender) {
        return new JSRemoteValueSender(sender.out(), exchange.getJavaObjects(), exchange.getJavaClasses());
    }

    @Override
    public JSObject invoke(JSObject instance, JSObject method, JSObject[] arguments) {
        instance = uncast(instance);
        for (int i = 0; i < arguments.length; ++i) {
            arguments[i] = uncast(arguments[i]);
        }
        if (!(instance instanceof JSRemoteValue)) {
            return exchange.invokeJavaMethod(instance, method, arguments);
        } else {
            JSDataMessageSender sender = createSender();
            WaitingFuture response = new WaitingFuture(exchange.getEventQueue());
            int messageId = exchange.addFuture(response);
            JSRemoteValueSender valueSender = createValueSender(sender);
            try {
                sender.out().writeByte(JSMessageExchange.INVOKE_METHOD);
                sender.out().writeInt(messageId);
                valueSender.send(instance);
                valueSender.send(method);
                sender.out().writeByte((byte)arguments.length);
                for (int i = 0; i < arguments.length; ++i) {
                    valueSender.send(arguments[i]);
                }
                if (logger.isInfoEnabled()) {
                    logger.info("Sending command to call method {}.{} and put result to slot #{}",
                            exchange.printValue(instance), exchange.printValue(method), messageId);
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
        instance = uncast(instance);
        for (int i = 0; i < arguments.length; ++i) {
            arguments[i] = uncast(arguments[i]);
        }
        if (!(instance instanceof JSRemoteValue)) {
            throw new RuntimeException("Can't call non-JavaScript constructor");
        } else {
            JSDataMessageSender sender = createSender();
            WaitingFuture response = new WaitingFuture(exchange.getEventQueue());
            int messageId = exchange.addFuture(response);
            JSRemoteValueSender valueSender = createValueSender(sender);
            try {
                sender.out().writeByte(JSMessageExchange.INSTANTIATE_CLASS);
                sender.out().writeInt(messageId);
                valueSender.send(instance);
                valueSender.send(method);
                sender.out().writeInt(arguments.length);
                for (int i = 0; i < arguments.length; ++i) {
                    valueSender.send(arguments[i]);
                }
                if (logger.isInfoEnabled()) {
                    logger.info("Sending command to call constructor {}.{} and put result to slot #{}",
                            exchange.printValue(instance), exchange.printValue(method), messageId);
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
        instance = uncast(instance);
        index = uncast(index);
        if (!(instance instanceof JSRemoteValue)) {
            // TODO: handle invocation of
            return null;
        } else {
            JSDataMessageSender sender = createSender();
            WaitingFuture response = new WaitingFuture(exchange.getEventQueue());
            int messageId = exchange.addFuture(response);
            JSRemoteValueSender valueSender = createValueSender(sender);
            try {
                sender.out().writeByte(JSMessageExchange.GET_PROPERTY);
                sender.out().writeInt(messageId);
                valueSender.send(instance);
                valueSender.send(index);
                if (logger.isInfoEnabled()) {
                    logger.info("Sending command to get property {}.{} to slot #{}", exchange.printValue(instance),
                            exchange.printValue(index), messageId);
                }
                sender.send();
            } catch (IOException e) {
                throw new RuntimeException("Error calling remote method");
            }
            return (JSObject)response.get();
        }
    }

    @Override
    public void set(JSObject instance, JSObject index, JSObject obj) {
        instance = uncast(instance);
        index = uncast(index);
        obj = uncast(obj);
        if (!(instance instanceof JSRemoteValue)) {
            // TODO: handle invocation of
        } else {
            JSDataMessageSender sender = createSender();
            WaitingFuture response = new WaitingFuture(exchange.getEventQueue());
            int messageId = exchange.addFuture(response);
            JSRemoteValueSender valueSender = createValueSender(sender);
            try {
                sender.out().writeByte(JSMessageExchange.SET_PROPERTY);
                sender.out().writeInt(messageId);
                valueSender.send(instance);
                valueSender.send(index);
                valueSender.send(obj);
                if (logger.isInfoEnabled()) {
                    logger.info("Sending command to set property {}.{} to value {}", exchange.printValue(instance),
                            exchange.printValue(index), exchange.printValue(obj));
                }
                sender.send();
            } catch (IOException e) {
                throw new RuntimeException("Error calling remote method");
            }
            response.get();
        }
    }

    @Override
    public JSObject function(JSObject instance, JSObject property) {
        // TODO: implement function wrapping
        return null;
    }

    @Override
    public <T extends JSObject> T cast(JSObject obj, Class<T> type) {
        obj = uncast(obj);
        @SuppressWarnings("unchecked")
        T safeResult = (T)Proxy.newProxyInstance(JSRemoteHost.class.getClassLoader(), new Class<?>[] { type },
                new JSObjectProxy(obj));
        return safeResult;
    }

    @Override
    public JSObject uncast(JSObject obj) {
        if (!Proxy.isProxyClass(obj.getClass())) {
            return obj;
        }
        InvocationHandler handler = Proxy.getInvocationHandler(obj);
        if (!(handler instanceof JSObjectProxy)) {
            return obj;
        }
        return ((JSObjectProxy)handler).getInnerObject();
    }
}
