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
public class JSObjectUnwrapper implements JSRemoteValueVisitor {
    private Object result;

    public Object getResult() {
        return result;
    }

    public Object unwrap(JSObject obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof JSRemoteValue) {
            try {
                ((JSRemoteValue)obj).acceptVisitor(this);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return result;
        } else {
            return obj;
        }
    }

    @Override
    public void visit(JSRemoteString value) throws Exception {
        result = value.getValue();
    }

    @Override
    public void visit(JSRemoteNumber value) throws Exception {
        result = value.getValue();
    }

    @Override
    public void visit(JSRemoteBoolean value) throws Exception {
        result = value.getValue();
    }

    @Override
    public void visit(JSRemoteUndefined value) throws Exception {
        result = null;
    }

    @Override
    public void visit(JSGlobalObject value) throws Exception {
        result = null;
    }

    @Override
    public void visit(JSRemoteObject value) throws Exception {
        result = null;
    }
}
