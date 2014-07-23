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

import java.io.DataOutput;
import java.io.IOException;
import org.teavm.jso.JSObject;
import org.teavm.jso.devmode.metadata.JSObjectMetadataRepository;
import org.teavm.jso.devmode.values.*;

/**
 *
 * @author Alexey Andreev
 */
public class JSRemoteValueSender implements JSRemoteValueVisitor {
    private DataOutput out;
    private JavaObjectRepository javaObjects;
    private JSObjectMetadataRepository javaClasses;

    public JSRemoteValueSender(DataOutput out, JavaObjectRepository javaObjects,
            JSObjectMetadataRepository javaClasses) {
        this.out = out;
        this.javaObjects = javaObjects;
        this.javaClasses = javaClasses;
    }

    public void send(JSObject value) throws IOException {
        if (value == null) {
            out.writeByte(JSRemoteValue.NULL);
        } else if (value instanceof JSRemoteValue) {
            try {
                ((JSRemoteValue)value).acceptVisitor(this);
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Error sending remote value", e);
            }
        } else {
            out.writeByte(JSRemoteValue.JAVA_OBJECT);
            out.writeInt(javaObjects.getId(value));
            out.writeInt(javaClasses.get(value.getClass()).getId());
        }
    }

    @Override
    public void visit(JSRemoteString value) throws Exception {
        out.writeByte(JSRemoteValue.STRING);
        out.writeUTF(value.getValue());
    }

    @Override
    public void visit(JSRemoteNumber value) throws Exception {
        if ((int)value.getValue() == value.getValue()) {
            out.writeByte(JSRemoteValue.INTEGER);
            out.writeInt((int)value.getValue());
        } else {
            out.writeByte(JSRemoteValue.NUMBER);
            out.writeDouble(value.getValue());
        }
    }

    @Override
    public void visit(JSRemoteBoolean value) throws Exception {
        out.writeByte(JSRemoteValue.BOOLEAN);
        out.writeBoolean(value.getValue());
    }

    @Override
    public void visit(JSRemoteUndefined value) throws Exception {
        out.writeByte(JSRemoteValue.UNDEFINED);
    }

    @Override
    public void visit(JSGlobalObject value) throws Exception {
        out.writeByte(JSRemoteValue.GLOBAL);
    }

    @Override
    public void visit(JSRemoteObject value) throws Exception {
        out.writeByte(JSRemoteValue.OBJECT);
        out.writeInt(value.getId());
    }
}
