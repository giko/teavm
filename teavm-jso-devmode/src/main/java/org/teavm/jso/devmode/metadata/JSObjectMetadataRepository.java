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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.teavm.jso.JSObject;

/**
 *
 * @author Alexey Andreev
 */
public class JSObjectMetadataRepository {
    private ConcurrentMap<Class<?>, Integer> map = new ConcurrentHashMap<>();
    private ConcurrentMap<Integer, JSObjectMetadata> indexMap = new ConcurrentHashMap<>();
    private AtomicInteger indexGenerator = new AtomicInteger();

    public JSObjectMetadata get(Class<? extends JSObject> cls) {
        int index = getIndex(cls);
        JSObjectMetadata metadata = indexMap.get(index);
        if (metadata == null) {
            metadata = new JSObjectMetadata(cls);
            metadata.index = index;
            JSObjectMetadata existing = indexMap.putIfAbsent(index, metadata);
            if (existing != null) {
                metadata = existing;
            }
        }
        return metadata;
    }

    public JSObjectMetadata get(int index) {
        return indexMap.get(index);
    }

    private int getIndex(Class<?> cls) {
        Integer index = map.get(cls);
        if (index == null) {
            index = indexGenerator.incrementAndGet();
            Integer existing = map.putIfAbsent(cls, index);
            if (existing != null) {
                index = existing;
            }
        }
        return index;
    }
}
