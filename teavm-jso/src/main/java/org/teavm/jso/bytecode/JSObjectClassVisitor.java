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

import org.objectweb.asm.*;

/**
 *
 * @author Alexey Andreev
 */
class JSObjectClassVisitor extends ClassVisitor {
    private MetadataKeeper metadata;
    private LocalVariableUsageAnalyzer locals;

    public JSObjectClassVisitor(int api, ClassVisitor cv, LocalVariableUsageAnalyzer locals, ClassLoader classLoader) {
        super(api, cv);
        this.metadata = new MetadataKeeper(classLoader);
        this.locals = locals;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new JSObjectMethodVisitor(Opcodes.ASM4, cv.visitMethod(access, name, desc, signature, exceptions),
                locals, metadata);
    }
}