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
package org.teavm.jso.devmode.values;

import org.teavm.jso.JSObject;

/**
 *
 * @author Alexey Andreev
 */
public class JSRemoteBoolean extends JSRemoteValue implements JSObject {
    public static final JSRemoteBoolean FALSE = new JSRemoteBoolean(false);
    public static final JSRemoteBoolean TRUE = new JSRemoteBoolean(true);
    private boolean value;

    public JSRemoteBoolean(boolean value) {
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    public static JSRemoteBoolean valueOf(boolean value) {
        return value ? TRUE : FALSE;
    }

    @Override
    public void acceptVisitor(JSRemoteValueVisitor visitor) throws Exception {
        visitor.visit(this);
    }
}
