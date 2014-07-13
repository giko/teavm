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
import org.teavm.jso.JSType;

/**
 *
 * @author Alexey Andreev
 */
public class JSMethodSignature {
    private JSType[] arguments;
    private JSType returnType;
    private Method javaMethod;

    JSMethodSignature(JSType[] arguments, JSType returnType, Method javaMethod) {
        this.arguments = arguments;
        this.returnType = returnType;
        this.javaMethod = javaMethod;
    }

    public JSType[] getArguments() {
        return arguments.clone();
    }

    public int size() {
        return arguments.length;
    }

    public JSType getArgument(int index) {
        return arguments[index];
    }

    public JSType getReturnType() {
        return returnType;
    }

    public Method getJavaMethod() {
        return javaMethod;
    }
}
