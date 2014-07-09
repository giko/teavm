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
package org.teavm.jso;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.apache.commons.io.IOUtils;
import org.teavm.jso.spi.JSHost;

/**
 *
 * @author Alexey Andreev
 */
final class JSHostAccessor {
    private static JSHost host;

    private JSHostAccessor() {
    }

    static {
        ClassLoader classLoader = JSHostAccessor.class.getClassLoader();
        String className;
        try (Reader reader = new InputStreamReader(classLoader.getResourceAsStream("META-INF/teavm/jshost"),
                "UTF-8")) {
            className = IOUtils.toString(reader).trim();
        } catch (IOException e) {
            throw new RuntimeException("Error finding JavaScript host", e);
        }
        Class<?> cls;
        try {
            cls = Class.forName(className, true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Error finding JavaScript host", e);
        }
        if (!JSHost.class.isAssignableFrom(cls)) {
            throw new RuntimeException("JavaScript host " + className + " does not implement " +
                    JSHost.class.getName());
        }
        Constructor<?> cons;
        try {
            cons = cls.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("JavaScript host " + className + " does not have " +
                    "a public zero-argument constructor");
        }
        try {
            host = (JSHost)cons.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Error creating JavaScript host of type " + className, e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Error creating JavaScript host of type " + className, e.getTargetException());
        }
    }

    public static JSHost access() {
        return host;
    }
}
