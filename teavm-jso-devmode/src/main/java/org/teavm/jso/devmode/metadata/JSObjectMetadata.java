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

import java.lang.reflect.Method;
import java.util.*;
import org.teavm.jso.*;

/**
 *
 * @author Alexey Andreev
 */
public class JSObjectMetadata {
    private static Map<Class<?>, JSType> jsTypeMap = new HashMap<>();
    private Map<String, JSObjectMember> members = new HashMap<>();
    private List<JSObjectProperty> properties = new ArrayList<>();
    private List<JSObjectProperty> readonlyProperties = Collections.unmodifiableList(properties);
    private List<JSObjectMethod> methods = new ArrayList<>();
    private List<JSObjectMethod> readonlyMethods = Collections.unmodifiableList(methods);
    int index;

    static {
        jsTypeMap.put(void.class, JSType.UNDEFINED);
        jsTypeMap.put(boolean.class, JSType.BOOLEAN);
        jsTypeMap.put(byte.class, JSType.NUMBER);
        jsTypeMap.put(short.class, JSType.NUMBER);
        jsTypeMap.put(int.class, JSType.NUMBER);
        jsTypeMap.put(float.class, JSType.NUMBER);
        jsTypeMap.put(double.class, JSType.NUMBER);
        jsTypeMap.put(String.class, JSType.STRING);
    }

    JSObjectMetadata(Class<? extends JSObject> cls) {
        Set<Class<?>> processedInterfaces = new HashSet<>();
        for (Class<?> iface : cls.getInterfaces()) {
            processInterface(iface, processedInterfaces);
        }
    }

    public int getIndex() {
        return index;
    }

    private void processInterface(Class<?> iface, Set<Class<?>> processedInteraces) {
        if (!processedInteraces.add(iface)) {
            return;
        }
        for (Method method : iface.getDeclaredMethods()) {
            if (method.isAnnotationPresent(JSProperty.class)) {
                if (isProperGetter(method)) {
                    String name = cutPrefix(method.getName(), method.getName().charAt(0) == 'i' ? 2 : 3);
                    JSObjectProperty property = new JSObjectProperty(name);
                    property.getter = method;
                    merge(property);
                } else if (isProperSetter(method)) {
                    String name = cutPrefix(method.getName(), 3);
                    JSObjectProperty property = new JSObjectProperty(name);
                    property.setter = method;
                    merge(property);
                } else {
                    throw new IllegalArgumentException("Method is neither getter nor setter: " + method);
                }
            } else if (method.isAnnotationPresent(JSIndexer.class)) {
                throw new IllegalArgumentException("Java implementations of JavaScript interfaces " +
                        "don't support indexers: " + method);
            } else if (method.isAnnotationPresent(JSConstructor.class)) {
                throw new IllegalArgumentException("Java implementations of JavaScript interfaces " +
                        "don't support constructors: " + method);
            } else {
                if (!method.getReturnType().equals(void.class) && !isSupportedType(method.getReturnType())) {
                    throw new IllegalArgumentException("Unsupported return type at " + method);
                }
                Class<?>[] args = method.getParameterTypes();
                for (Class<?> arg : args) {
                    if (!isSupportedType(arg)) {
                        throw new IllegalArgumentException("Unsupported parameter type at " + method);
                    }
                }
                JSObjectMethod jsMethod = new JSObjectMethod(method.getName());
                JSType[] jsArgs = new JSType[args.length];
                for (int i = 0; i < args.length; ++i) {
                    jsArgs[i] = asJSType(args[i]);
                }
                JSType returnType = asJSType(method.getReturnType());
                JSMethodSignature signature = new JSMethodSignature(jsArgs, returnType, method);
                jsMethod.getSignatures().add(signature);
                merge(jsMethod);
            }
        }
        for (Class<?> superIface : iface.getInterfaces()) {
            processInterface(superIface, processedInteraces);
        }
    }

    private void merge(JSObjectMember member) {
        JSObjectMember existing = members.get(member.getName());
        if (existing == null) {
            members.put(member.getName(), member);
            return;
        }
        if (existing instanceof JSObjectProperty && member instanceof JSObjectMethod) {
            reportConflict((JSObjectProperty)existing, (JSObjectMethod)member);
        } else if (existing instanceof JSObjectMethod && member instanceof JSObjectProperty) {
            reportConflict((JSObjectProperty)member, (JSObjectMethod)existing);
        } else if (member instanceof JSObjectProperty) {
            mergeProperty((JSObjectProperty)existing, (JSObjectProperty)member);
        } else {
            mergeMethod((JSObjectMethod)existing, (JSObjectMethod)member);
        }
    }

    private void mergeProperty(JSObjectProperty existing, JSObjectProperty property) {
        if (property.getter != null) {
            if (existing.getter != null && !existing.getter.getReturnType().equals(
                    property.getter.getReturnType())) {
                throw new IllegalArgumentException("Conflict found between " + existing.getter + " and " +
                        property.getter);
            }
            existing.getter = property.getter;
        }
        if (property.setter != null) {
            if (existing.setter != null && !existing.setter.getParameterTypes()[0].equals(
                    property.setter.getParameterTypes()[0])) {
                throw new IllegalArgumentException("Conflict found between " + existing.setter + " and " +
                        property.setter);
            }
            existing.setter = property.setter;
        }
    }

    private void mergeMethod(JSObjectMethod existing, JSObjectMethod method) {
        for (JSMethodSignature signature : method.getSignatures()) {
            JSMethodSignature existingSignature = existing.getSignature(signature.getArguments());
            if (existingSignature != null) {
                throw new IllegalArgumentException("Conflict found between " + existingSignature.getJavaMethod() +
                        " and " + signature.getJavaMethod());
            }
            existing.addSignature(signature);
        }
    }

    private void reportConflict(JSObjectProperty property, JSObjectMethod method) {
        Method first = property.getGetter() != null ? property.getGetter() : property.getSetter();
        Method second = method.getSignatures().iterator().next().getJavaMethod();
        throw new IllegalArgumentException("Conflict found between " + first + " and " + second);
    }

    public JSObjectMember get(String name) {
        return members.get(name);
    }

    public List<JSObjectProperty> getProperties() {
        return readonlyProperties;
    }

    public List<JSObjectMethod> getMethods() {
        return readonlyMethods;
    }

    boolean isSupportedType(Class<?> cls) {
        if (cls.isPrimitive()) {
            return !cls.equals(long.class) && !cls.equals(void.class);
        } else {
            return JSObject.class.isAssignableFrom(cls);
        }
    }

    private boolean isProperGetter(Method method) {
        Class<?>[] params = method.getParameterTypes();
        if (params.length > 0 || !isSupportedType(method.getReturnType())) {
            return false;
        }
        if (method.getReturnType().equals(boolean.class)) {
            if (isProperPrefix(method.getName(), "is")) {
                return true;
            }
        }
        return isProperPrefix(method.getName(), "get");
    }

    private boolean isProperSetter(Method method) {
        Class<?>[] params = method.getParameterTypes();
        if (params.length != 1 || !isSupportedType(params[0]) || method.getReturnType().equals(void.class)) {
            return false;
        }
        return isProperPrefix(method.getName(), "set");
    }

    private boolean isProperPrefix(String name, String prefix) {
        if (!name.startsWith(prefix) || name.length() == prefix.length()) {
            return false;
        }
        char c = name.charAt(prefix.length());
        return Character.isUpperCase(c);
    }

    private String cutPrefix(String name, int prefixLength) {
        if (name.length() == prefixLength + 1) {
            return name.substring(prefixLength).toLowerCase();
        }
        char c = name.charAt(prefixLength + 1);
        if (Character.isUpperCase(c)) {
            return name.substring(prefixLength);
        }
        return Character.toLowerCase(name.charAt(prefixLength)) + name.substring(prefixLength + 1);
    }

    private JSType asJSType(Class<?> cls) {
        JSType knownType = jsTypeMap.get(cls);
        if (knownType != null) {
            return knownType;
        }
        return JSType.OBJECT;
    }
}
