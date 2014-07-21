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
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSType;
import org.teavm.jso.devmode.metadata.*;
import org.teavm.jso.devmode.values.*;

/**
 *
 * @author Alexey Andreev
 */
public abstract class JSMessageExchange implements JSMessageSender {
    public static final byte RECEIVE_VALUE = 0;
    public static final byte RECEIVE_EXCEPTION = 1;
    public static final byte RECEIVE_JAVA_CLASS_INFO = 2;
    public static final byte CREATE_ARRAY = 3;
    public static final byte INVOKE_METHOD = 5;
    public static final byte INSTANTIATE_CLASS = 6;
    public static final byte GET_JAVA_CLASS_INFO = 7;
    public static final byte GET_PROPERTY = 8;
    public static final byte SET_PROPERTY = 9;
    public static final byte READABLE_PROPERTY = 1;
    public static final byte WRITABLE_PROPERTY = 2;
    private final AtomicInteger messageIdGenerator = new AtomicInteger();
    private ConcurrentMap<Integer, WaitingFuture> futures = new ConcurrentHashMap<>();
    private JavaObjectRepository javaObjects = new JavaObjectRepository();
    private JSObjectMetadataRepository javaClasses = new JSObjectMetadataRepository();
    private EventQueue eventQueue = new EventQueue();

    public void init() {
        JSRemoteHost.getInstance().setExchange(this);
    }

    public void receive(InputStream inputStream) throws IOException {
        DataInput input = new DataInputStream(inputStream);
        switch (input.readByte()) {
            case RECEIVE_VALUE: {
                int messageId = input.readInt();
                JSRemoteValueReceiver valueReceiver = createValueReceiver(input);
                JSObject value = valueReceiver.receive();
                futures.get(messageId).set(value);
                break;
            }
            case RECEIVE_EXCEPTION: {
                int messageId = input.readInt();
                String exceptionMesage = input.readUTF();
                futures.get(messageId).setException(new JSRemoteException(exceptionMesage));
                break;
            }
            case GET_JAVA_CLASS_INFO: {
                int messageId = input.readInt();
                int objectId = input.readInt();
                JSObject obj = javaObjects.get(objectId);
                sendJavaObjectInfo(messageId, obj);
                break;
            }
            case INVOKE_METHOD: {
                int messageId = input.readInt();
                JSRemoteValueReceiver valueReceiver = createValueReceiver(input);
                JSObject instance = valueReceiver.receive();
                JSObject method = valueReceiver.receive();
                JSObject[] arguments = new JSObject[input.readByte()];
                for (int i = 0; i < arguments.length; ++i) {
                    arguments[i] = valueReceiver.receive();
                }
                invokeJavaMethodAndSendResponse(messageId, instance, method, arguments);
            }
        }
    }

    private void invokeJavaMethodAndSendResponse(final int messageId, final JSObject instance, final JSObject method,
            final JSObject[] arguments) {
        eventQueue.put(new Runnable() {
            @Override public void run() {
                try {
                    JSObject result = invokeJavaMethod(instance, method, arguments);

                    JSDataMessageSender messageSender = new JSDataMessageSender(JSMessageExchange.this);
                    JSRemoteValueSender valueSender = new JSRemoteValueSender(messageSender.out(),
                            javaObjects, javaClasses);
                    messageSender.out().writeByte(RECEIVE_VALUE);
                    messageSender.out().writeInt(messageId);
                    valueSender.send(result);
                    messageSender.send();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public JSObject invokeJavaMethod(JSObject instance, JSObject method, JSObject[] arguments) {
        if (instance == null) {
            throw new IllegalArgumentException("Can't invoke method of null");
        }
        if (instance instanceof JSRemoteValue) {
            throw new IllegalArgumentException("Can't invoke method of primitive value");
        }
        String methodName = ((JSRemoteString)method).getValue();
        JSObjectTypeExtractor typeExtractor = new JSObjectTypeExtractor();
        JSObjectUnwrapper unwrapper = new JSObjectUnwrapper();
        JSType[] argTypes = new JSType[arguments.length];
        Object[] javaAguments = new Object[arguments.length];
        for (int i = 0; i < arguments.length; ++i) {
            argTypes[i] = typeExtractor.extract(arguments[i]);
            javaAguments[i] = unwrapper.unwrap(arguments[i]);
        }
        JSObjectMetadata cls = javaClasses.get(instance.getClass());
        JSObjectMember member = cls.get(methodName);
        if (!(member instanceof JSObjectMethod)) {
            throw new IllegalArgumentException(instance.getClass().getName() + "." + methodName + " is not a method");
        }
        JSObjectMethod methodMember = (JSObjectMethod)member;
        JSMethodSignature signature = methodMember.getSignature(argTypes);
        if (signature == null) {
            throw new IllegalArgumentException("Method variant not found: " + instance.getClass().getName() +
                    "." + methodName);
        }
        Object result;
        try {
            result = signature.getJavaMethod().invoke(instance, javaAguments);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error calling Java method " + signature.getJavaMethod(), e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Error calling Java method " + signature.getJavaMethod(),
                    e.getTargetException());
        }
        return wrap(signature.getReturnType(), result);
    }

    private JSObject wrap(JSType type, Object value) {
        switch (type) {
            case BOOLEAN:
                return new JSRemoteBoolean((Boolean)value);
            case NUMBER: {
                if (value instanceof Byte) {
                    return new JSRemoteNumber((Byte)value);
                } else if (value instanceof Short) {
                    return new JSRemoteNumber((Short)value);
                } else if (value instanceof Integer) {
                    return new JSRemoteNumber((Integer)value);
                } else if (value instanceof Float) {
                    return new JSRemoteNumber((Float)value);
                } else if (value instanceof Double) {
                    return new JSRemoteNumber((Double)value);
                } else {
                    throw new IllegalArgumentException("Can't wrap this value as a number");
                }
            }
            case OBJECT:
                return (JSObject)value;
            case STRING:
                return new JSRemoteString((String)value);
            case UNDEFINED:
                return JSRemoteUndefined.getInstance();
            case FUNCTION:
                return null;
        }
        return null;
    }

    private JSRemoteValueReceiver createValueReceiver(DataInput input) {
        return new JSRemoteValueReceiver(input, javaObjects, javaClasses);
    }

    private void sendJavaObjectInfo(int messageId, JSObject obj) throws IOException {
        JSDataMessageSender sender = new JSDataMessageSender(this);
        sender.out().write(RECEIVE_JAVA_CLASS_INFO);
        sender.out().writeInt(messageId);

        JSObjectMetadata metadata = javaClasses.get(obj.getClass());
        List<JSObjectProperty> properties = metadata.getProperties();
        List<JSObjectMethod> methods = metadata.getMethods();

        sender.out().writeShort((short)properties.size());
        for (JSObjectProperty property : properties) {
            sender.out().writeUTF(property.getName());
            byte flags = 0;
            if (property.getGetter() != null) {
                flags |= READABLE_PROPERTY;
            }
            if (property.getSetter() != null) {
                flags |= WRITABLE_PROPERTY;
            }
            sender.out().writeByte(flags);
        }

        sender.out().writeShort((short)methods.size());
        for (JSObjectMethod method : methods) {
            sender.out().writeUTF(method.getName());
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

    public JSObjectMetadataRepository getJavaClasses() {
        return javaClasses;
    }

    public EventQueue getEventQueue() {
        return eventQueue;
    }
}
