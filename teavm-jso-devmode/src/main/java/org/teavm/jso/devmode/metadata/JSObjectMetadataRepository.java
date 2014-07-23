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
package org.teavm.jso.devmode.metadata;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.teavm.jso.JSObject;
import org.teavm.jso.devmode.JSDataMessageSender;
import org.teavm.jso.devmode.JSMessageExchange;
import org.teavm.jso.devmode.JSMessageSender;

/**
 *
 * @author Alexey Andreev
 */
public class JSObjectMetadataRepository {
    private ConcurrentMap<Class<?>, Integer> map = new ConcurrentHashMap<>();
    private ConcurrentMap<Integer, JSObjectMetadata> idMap = new ConcurrentHashMap<>();
    private AtomicInteger idGenerator = new AtomicInteger();
    private JSMessageSender sender;

    public JSObjectMetadataRepository(JSMessageSender sender) {
        this.sender = sender;
    }

    public JSObjectMetadata get(Class<? extends JSObject> cls) {
        int id = getId(cls);
        JSObjectMetadata metadata = idMap.get(id);
        if (metadata == null) {
            metadata = new JSObjectMetadata(cls);
            metadata.id = id;
            JSObjectMetadata existing = idMap.putIfAbsent(id, metadata);
            if (existing != null) {
                metadata = existing;
            } else {
                sendJavaObjectInfo(metadata);
            }
        }
        return metadata;
    }

    public JSObjectMetadata get(int id) {
        return idMap.get(id);
    }

    private int getId(Class<?> cls) {
        Integer index = map.get(cls);
        if (index == null) {
            index = idGenerator.incrementAndGet();
            Integer existing = map.putIfAbsent(cls, index);
            if (existing != null) {
                index = existing;
            }
        }
        return index;
    }

    private void sendJavaObjectInfo(JSObjectMetadata metadata) {
        try {
            JSDataMessageSender sender = new JSDataMessageSender(this.sender);
            sender.out().write(JSMessageExchange.RECEIVE_JAVA_CLASS_INFO);
            sender.out().writeInt(metadata.id);

            List<JSObjectProperty> properties = metadata.getProperties();
            List<JSObjectMethod> methods = metadata.getMethods();

            sender.out().writeShort((short)properties.size());
            for (JSObjectProperty property : properties) {
                sender.out().writeUTF(property.getName());
                byte flags = 0;
                if (property.getGetter() != null) {
                    flags |= JSMessageExchange.READABLE_PROPERTY;
                }
                if (property.getSetter() != null) {
                    flags |= JSMessageExchange.WRITABLE_PROPERTY;
                }
                sender.out().writeByte(flags);
            }

            sender.out().writeShort((short)methods.size());
            for (JSObjectMethod method : methods) {
                sender.out().writeUTF(method.getName());
            }
        } catch (IOException e) {
            throw new RuntimeException("Error sending class metadata to server", e);
        }
    }
}
