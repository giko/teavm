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

import java.io.DataInput;
import java.io.IOException;
import org.teavm.jso.JSObject;
import org.teavm.jso.devmode.metadata.JSObjectMetadata;
import org.teavm.jso.devmode.metadata.JSObjectMetadataRepository;
import org.teavm.jso.devmode.values.*;

/**
 *
 * @author Alexey Andreev
 */
public class JSRemoteValueReceiver {
    private DataInput input;
    private JavaObjectRepository javaObjects;
    private JSObjectMetadataRepository javaClasses;

    public JSRemoteValueReceiver(DataInput input, JavaObjectRepository javaObjects,
            JSObjectMetadataRepository javaClasses) {
        this.input = input;
        this.javaObjects = javaObjects;
        this.javaClasses = javaClasses;
    }

    public JSObject receive() throws IOException {
        byte kind = input.readByte();
        switch (kind) {
            case JSRemoteValue.BOOLEAN:
                return new JSRemoteBoolean(input.readBoolean());
            case JSRemoteValue.GLOBAL:
                return JSGlobalObject.getInstance();
            case JSRemoteValue.INTEGER:
                return new JSRemoteNumber(input.readInt());
            case JSRemoteValue.JAVA_OBJECT: {
                int objectId = input.readInt();
                int classId = input.readInt();
                JSObject obj = javaObjects.get(objectId);
                if (obj == null) {
                    throw new IllegalStateException("Java object not found: " + objectId);
                }
                JSObjectMetadata cls = javaClasses.get(classId);
                if (cls == null) {
                    throw new IllegalStateException("Java class not found: " + objectId);
                }
                if (javaClasses.get(obj.getClass()).getId() != cls.getId()) {
                    throw new IllegalStateException("Java object and class markers don't match each other");
                }
                return obj;
            }
            case JSRemoteValue.NULL:
                return null;
            case JSRemoteValue.NUMBER:
                return new JSRemoteNumber(input.readDouble());
            case JSRemoteValue.OBJECT:
                return new JSRemoteObject(input.readInt());
            case JSRemoteValue.STRING:
                return new JSRemoteString(input.readUTF());
            case JSRemoteValue.UNDEFINED:
                return JSRemoteUndefined.getInstance();
            default:
                throw new IllegalStateException("Unexpected value type: " + kind);
        }
    }
}
