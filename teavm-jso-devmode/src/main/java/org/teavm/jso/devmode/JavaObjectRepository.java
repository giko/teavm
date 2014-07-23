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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.teavm.jso.JSObject;

/**
 *
 * @author Alexey Andreev
 */
public class JavaObjectRepository {
    private AtomicInteger idGenerator = new AtomicInteger();
    private ConcurrentMap<JSObject, Integer> idMap = new ConcurrentHashMap<>();
    private ConcurrentMap<Integer, JSObject> objectMap = new ConcurrentHashMap<>();

    public Integer getId(JSObject object) {
        Integer id = idMap.get(object);
        if (id == null) {
            id = idGenerator.incrementAndGet();
            Integer oldId = idMap.putIfAbsent(object, id);
            if (oldId != null) {
                id = oldId;
            } else {
                objectMap.put(id, object);
            }
        }
        return id;
    }

    public JSObject get(int id) {
        return objectMap.get(id);
    }

    public boolean contains(JSObject object) {
        return idMap.containsKey(object);
    }
}
