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

import java.util.Iterator;
import org.teavm.dependency.PluggableDependency;
import org.teavm.javascript.ni.GeneratedBy;
import org.teavm.javascript.ni.InjectedBy;

/**
 *
 * @author Alexey Andreev
 */
public final class JS {
    private JS() {
    }

    public static JSType getType(JSObject obj) {
        switch (unwrapString(getTypeName(obj))) {
            case "boolean":
                return JSType.OBJECT;
            case "number":
                return JSType.NUMBER;
            case "string":
                return JSType.STRING;
            case "function":
                return JSType.FUNCTION;
            case "object":
                return JSType.OBJECT;
            case "undefined":
                return JSType.UNDEFINED;
        }
        throw new AssertionError("Unexpected type");
    }

    @InjectedBy(JSNativeGenerator.class)
    public static JSObject getTypeName(JSObject obj) {
        return JSHostAccessor.access().getTypeName(obj);
    }

    @InjectedBy(JSNativeGenerator.class)
    public static JSObject getGlobal() {
        return JSHostAccessor.access().getGlobal();
    }

    @GeneratedBy(JSNativeGenerator.class)
    public static JSObject wrap(String str) {
        return JSHostAccessor.access().wrap(str);
    }

    @InjectedBy(JSNativeGenerator.class)
    public static JSObject wrap(char c) {
        return JSHostAccessor.access().wrap(c);
    }

    @InjectedBy(JSNativeGenerator.class)
    public static JSObject wrap(int num) {
        return JSHostAccessor.access().wrap(num);
    }

    @InjectedBy(JSNativeGenerator.class)
    public static JSObject wrap(float num) {
        return JSHostAccessor.access().wrap(num);
    }

    @InjectedBy(JSNativeGenerator.class)
    public static JSObject wrap(double num) {
        return JSHostAccessor.access().wrap(num);
    }

    @InjectedBy(JSNativeGenerator.class)
    public static JSObject wrap(boolean flag) {
        return JSHostAccessor.access().wrap(flag);
    }

    @InjectedBy(JSNativeGenerator.class)
    public static boolean unwrapBoolean(JSObject obj) {
        return JSHostAccessor.access().unwrapBoolean(obj);
    }

    public static byte unwrapByte(JSObject obj) {
        return (byte)unwrapInt(obj);
    }

    public static short unwrapShort(JSObject obj) {
        return (short)unwrapInt(obj);
    }

    @InjectedBy(JSNativeGenerator.class)
    public static int unwrapInt(JSObject obj) {
        return JSHostAccessor.access().unwrapInt(obj);
    }

    @InjectedBy(JSNativeGenerator.class)
    public static float unwrapFloat(JSObject obj) {
        return JSHostAccessor.access().unwrapFloat(obj);
    }

    @InjectedBy(JSNativeGenerator.class)
    public static double unwrapDouble(JSObject obj) {
        return JSHostAccessor.access().unwrapDouble(obj);
    }

    @GeneratedBy(JSNativeGenerator.class)
    public static String unwrapString(JSObject obj) {
        return JSHostAccessor.access().unwrapString(obj);
    }

    @InjectedBy(JSNativeGenerator.class)
    public static char unwrapCharacter(JSObject obj) {
        return JSHostAccessor.access().unwrapCharacter(obj);
    }

    @InjectedBy(JSNativeGenerator.class)
    public static boolean isUndefined(JSObject obj) {
        return JSHostAccessor.access().isUndefined(obj);
    }

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static JSObject invoke(JSObject instance, JSObject method) {
        return JSHostAccessor.access().invoke(instance, method, new JSObject[0]);
    }

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static JSObject invoke(JSObject instance, JSObject method, JSObject a) {
        return JSHostAccessor.access().invoke(instance, method, new JSObject[] { a });
    }

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b) {
        return JSHostAccessor.access().invoke(instance, method, new JSObject[] { a, b });
    }

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c) {
        return JSHostAccessor.access().invoke(instance, method, new JSObject[] { a, b, c });
    }

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c, JSObject d) {
        return JSHostAccessor.access().invoke(instance, method, new JSObject[] { a, b, c, d });
    }

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c, JSObject d,
            JSObject e) {
        return JSHostAccessor.access().invoke(instance, method, new JSObject[] { a, b, c, d, e });
    }

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c, JSObject d,
            JSObject e, JSObject f) {
        return JSHostAccessor.access().invoke(instance, method, new JSObject[] { a, b, c, d, e, f });
    }

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c, JSObject d,
            JSObject e, JSObject f, JSObject g) {
        return JSHostAccessor.access().invoke(instance, method, new JSObject[] { a, b, c, d, e, f, g });
    }

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c, JSObject d,
            JSObject e, JSObject f, JSObject g, JSObject h) {
        return JSHostAccessor.access().invoke(instance, method, new JSObject[] { a, b, c, d, e, f, g, h });
    }

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static JSObject instantiate(JSObject instance, JSObject constructor) {
        return JSHostAccessor.access().instantiate(instance, constructor, new JSObject[] {});
    }

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static JSObject instantiate(JSObject instance, JSObject constructor, JSObject a) {
        return JSHostAccessor.access().instantiate(instance, constructor, new JSObject[] { a });
    }

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static JSObject instantiate(JSObject instance, JSObject constructor, JSObject a, JSObject b) {
        return JSHostAccessor.access().instantiate(instance, constructor, new JSObject[] { a, b });
    }

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static JSObject instantiate(JSObject instance, JSObject constructor, JSObject a, JSObject b, JSObject c) {
        return JSHostAccessor.access().instantiate(instance, constructor, new JSObject[] { a, b, c });
    }

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static JSObject instantiate(JSObject instance, JSObject constructor, JSObject a, JSObject b, JSObject c,
            JSObject d) {
        return JSHostAccessor.access().instantiate(instance, constructor, new JSObject[] { a, b, c, d });
    }

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static JSObject instantiate(JSObject instance, JSObject constructor, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e) {
        return JSHostAccessor.access().instantiate(instance, constructor, new JSObject[] { a, b, c, d, e });
    }

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static JSObject instantiate(JSObject instance, JSObject constructor, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f) {
        return JSHostAccessor.access().instantiate(instance, constructor, new JSObject[] { a, b, c, d, e, f });
    }

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static JSObject instantiate(JSObject instance, JSObject constructor, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g) {
        return JSHostAccessor.access().instantiate(instance, constructor, new JSObject[] { a, b, c, d, e, f, g });
    }

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static JSObject instantiate(JSObject instance, JSObject constructor, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g, JSObject h) {
        return JSHostAccessor.access().instantiate(instance, constructor, new JSObject[] { a, b, c, d, e, f, g, h });
    }

    public static <T extends JSObject> Iterable<T> iterate(final JSArray<T> array) {
        return new Iterable<T>() {
            @Override public Iterator<T> iterator() {
                return new Iterator<T>() {
                    int index = 0;
                    @Override public boolean hasNext() {
                        return index < array.getLength();
                    }
                    @Override public T next() {
                        return array.get(index++);
                    }
                    @Override public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    @InjectedBy(JSNativeGenerator.class)
    public static JSObject get(JSObject instance, JSObject index) {
        return JSHostAccessor.access().get(instance, index);
    }

    @InjectedBy(JSNativeGenerator.class)
    public static void set(JSObject instance, JSObject index, JSObject obj) {
        JSHostAccessor.access().set(instance, index, obj);
    }

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static JSObject function(JSObject instance, JSObject property) {
        return JSHostAccessor.access().function(instance, property);
    }

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static <T extends JSObject> T cast(JSObject obj, Class<T> type) {
        return JSHostAccessor.access().cast(obj, type);
    }

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static JSObject uncast(JSObject obj) {
        return JSHostAccessor.access().uncast(obj);
    }
}
