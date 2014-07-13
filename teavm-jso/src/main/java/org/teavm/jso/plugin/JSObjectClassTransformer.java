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
package org.teavm.jso.plugin;

import org.teavm.javascript.ni.GeneratedBy;
import org.teavm.javascript.ni.InjectedBy;
import org.teavm.jso.JS;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev
 */
class JSObjectClassTransformer implements ClassHolderTransformer {
    private ThreadLocal<JavascriptNativeProcessor> processor = new ThreadLocal<>();

    @Override
    public void transformClass(ClassHolder cls, ClassReaderSource innerSource) {
        if (cls.getName().equals(JS.class.getName())) {
            for (MethodHolder method : cls.getMethods()) {
                if (method.getAnnotations().get(InjectedBy.class.getName()) != null ||
                        method.getAnnotations().get(GeneratedBy.class.getName()) != null) {
                    method.getModifiers().add(ElementModifier.NATIVE);
                    method.setProgram(null);
                }
            }
            return;
        }
        JavascriptNativeProcessor processor = getProcessor(innerSource);
        processor.processClass(cls);
        for (MethodHolder method : cls.getMethods()) {
            if (method.getProgram() != null) {
                processor.processProgram(method.getProgram());
            }
        }
    }

    private JavascriptNativeProcessor getProcessor(ClassReaderSource innerSource) {
        if (processor.get() == null) {
            processor.set(new JavascriptNativeProcessor(innerSource));
        }
        return processor.get();
    }
}
