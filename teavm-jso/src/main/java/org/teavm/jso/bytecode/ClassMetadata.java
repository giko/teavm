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
package org.teavm.jso.bytecode;

import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.tree.AnnotationNode;

/**
 *
 * @author Alexey Andreev
 */
class ClassMetadata {
    Map<String, AnnotationNode[]> methodsAnnotations = new HashMap<>();
    boolean javaScriptObject;

    public AnnotationNode[] getMethodAnnotations(String name, String desc) {
        return methodsAnnotations.get(name + desc);
    }

    public boolean isJavaScriptObject() {
        return javaScriptObject;
    }
}
