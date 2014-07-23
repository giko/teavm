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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AnnotationNode;

/**
 *
 * @author Alexey Andreev
 */
class MetadataKeeper {
    private Map<String, ClassMetadata> classes = new HashMap<>();
    private ClassLoader classLoader;

    public MetadataKeeper(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public synchronized ClassMetadata getClassMetadata(String name) {
        ClassMetadata classMetadata = classes.get(name);
        if (classMetadata == null) {
            classMetadata = new ClassMetadata();
            if (name.equals("org/teavm/jso/JSObject")) {
                classMetadata.javaScriptObject = true;
            }
            try (InputStream input = classLoader.getResourceAsStream(name + ".class")) {
                if (input != null) {
                    new ClassReader(input).accept(new AnnotationGatheringVisitor(classMetadata), 0);
                }
            } catch (IOException e) {
                throw new RuntimeException("Error reading class " + name);
            }
            classes.put(name, classMetadata);
        }
        return classMetadata;
    }

    private class AnnotationGatheringVisitor extends ClassVisitor {
        private ClassMetadata classMetadata;

        public AnnotationGatheringVisitor(ClassMetadata annotationMap) {
            super(Opcodes.ASM4);
            this.classMetadata = annotationMap;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                String[] interfaces) {
            if (!classMetadata.javaScriptObject && (access & Opcodes.ACC_INTERFACE) != 0) {
                for (String iface : interfaces) {
                    if (getClassMetadata(iface).javaScriptObject) {
                        classMetadata.javaScriptObject = true;
                        break;
                    }
                }
            }
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationNode node = new AnnotationNode(Opcodes.ASM4, desc);
            classMetadata.annotations.put(desc, node);
            return node;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            return new MethodAnnotationGatheringVisitor(classMetadata, name + desc);
        }
    }

    private static class MethodAnnotationGatheringVisitor extends MethodVisitor {
        private List<AnnotationNode> annotations = new ArrayList<>();
        private ClassMetadata classMetadata;
        private String key;

        public MethodAnnotationGatheringVisitor(ClassMetadata annotationMap, String key) {
            super(Opcodes.ASM4);
            this.classMetadata = annotationMap;
            this.key = key;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationNode node = new AnnotationNode(Opcodes.ASM4, desc);
            annotations.add(node);
            return node;
        }

        @Override
        public void visitEnd() {
            classMetadata.methodsAnnotations.put(key, annotations.toArray(new AnnotationNode[0]));
        }
    }
}
