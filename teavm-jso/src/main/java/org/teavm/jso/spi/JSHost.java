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
package org.teavm.jso.spi;

import org.teavm.jso.JSArray;
import org.teavm.jso.JSObject;

/**
 *
 * @author Alexey Andreev
 */
public interface JSHost {
    <T extends JSObject> JSArray<T> createArray(int size);

    JSObject getTypeName(JSObject obj);

    JSObject getGlobal();

    JSObject wrap(String value);

    JSObject wrap(char value);

    JSObject wrap(int value);

    JSObject wrap(float value);

    JSObject wrap(double value);

    JSObject wrap(boolean value);

    boolean unwrapBoolean(JSObject obj);

    int unwrapInt(JSObject obj);

    float unwrapFloat(JSObject obj);

    double unwrapDouble(JSObject obj);

    String unwrapString(JSObject obj);

    char unwrapCharacter(JSObject obj);

    boolean isUndefined(JSObject obj);

    JSObject invoke(JSObject instance, JSObject method, JSObject[] arguments);

    JSObject instantiate(JSObject instance, JSObject method, JSObject[] arguments);

    JSObject get(JSObject instance, JSObject index);

    void set(JSObject instance, JSObject index, JSObject obj);

    JSObject function(JSObject instance, JSObject property);
}
