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
    private AtomicInteger indexGenerator = new AtomicInteger();
    private ConcurrentMap<JSObject, Integer> idMap = new ConcurrentHashMap<>();

    public int put(JSObject object) {
        int id = indexGenerator.incrementAndGet();
        idMap.put(object, id);
        return id;
    }

    public Integer getId(JSObject object) {
        return idMap.get(object);
    }

    public boolean contains(JSObject object) {
        return idMap.containsKey(object);
    }
}
