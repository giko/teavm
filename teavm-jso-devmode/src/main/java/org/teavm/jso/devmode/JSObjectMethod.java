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

import java.util.*;
import org.teavm.jso.JSType;

/**
 *
 * @author Alexey Andreev
 */
public class JSObjectMethod extends JSObjectMember {
    private Map<String, JSMethodSignature> signatureMap = new HashMap<>();
    private List<JSMethodSignature> signatures = new ArrayList<>();
    private List<JSMethodSignature> readonlySignatures = Collections.unmodifiableList(signatures);

    JSObjectMethod(String name) {
        super(name);
    }

    public List<JSMethodSignature> getSignatures() {
        return readonlySignatures;
    }

    void addSignature(JSMethodSignature signature) {
        String key = computeKey(signature.getArguments());
        signatureMap.put(key, signature);
        signatures.add(signature);
    }

    public JSMethodSignature getSignature(JSType[] types) {
        String key = computeKey(types);
        return signatureMap.get(key);
    }

    private String computeKey(JSType[] types) {
        char[] chars = new char[types.length];
        for (int i = 0; i < types.length; ++i) {
            switch (types[i]) {
                case BOOLEAN:
                    chars[i] = 'B';
                    break;
                case NUMBER:
                    chars[i] = 'N';
                    break;
                case OBJECT:
                    chars[i] = 'O';
                    break;
                case FUNCTION:
                    chars[i] = 'F';
                    break;
                case STRING:
                    chars[i] = 'S';
                    break;
                case UNDEFINED:
                    chars[i] = 'U';
                    break;
            }
        }
        return new String(chars);
    }
}
