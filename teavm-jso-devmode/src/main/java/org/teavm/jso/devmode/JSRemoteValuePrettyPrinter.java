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

import org.teavm.jso.JSObject;
import org.teavm.jso.devmode.values.*;

/**
 *
 * @author Alexey Andreev
 */
public class JSRemoteValuePrettyPrinter implements JSRemoteValueVisitor {
    private JavaObjectRepository objects;
    private String text;

    public JSRemoteValuePrettyPrinter(JavaObjectRepository objects) {
        this.objects = objects;
    }

    public String print(JSObject value) {
        if (value == null) {
            return "null";
        } else if (value instanceof JSRemoteValue) {
            try {
                ((JSRemoteValue)value).acceptVisitor(this);
                return getText();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            int id = objects.getId(value);
            return "{proxy #" + id + "}";
        }
    }

    public String getText() {
        return text;
    }

    @Override
    public void visit(JSRemoteString value) throws Exception {
        text = value.getValue();
    }

    @Override
    public void visit(JSRemoteNumber value) throws Exception {
        text = String.valueOf(value.getValue());
    }

    @Override
    public void visit(JSRemoteBoolean value) throws Exception {
        text = String.valueOf(value);
    }

    @Override
    public void visit(JSRemoteUndefined value) throws Exception {
        text = "undefined";
    }

    @Override
    public void visit(JSGlobalObject value) throws Exception {
        text = "{global}";
    }

    @Override
    public void visit(JSRemoteObject value) throws Exception {
        text = "{object #" + value.getId() + "}";
    }
}
