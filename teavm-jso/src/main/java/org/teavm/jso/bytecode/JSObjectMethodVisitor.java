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

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.teavm.jso.JS;
import org.teavm.jso.JSConstructor;
import org.teavm.jso.JSIndexer;
import org.teavm.jso.JSProperty;

/**
 *
 * @author Alexey Andreev
 */
class JSObjectMethodVisitor extends MethodVisitor {
    private static final String JS_CLS = Type.getInternalName(JS.class);
    private static final String JSOBJECT_CLS = Type.getInternalName(JS.class);
    private LocalVariableUsageAnalyzer locals;
    private MetadataKeeper metadata;
    private Type[] arguments;
    private int access;

    public JSObjectMethodVisitor(int api, MethodVisitor mv, Type[] arguments, int access,
            LocalVariableUsageAnalyzer locals, MetadataKeeper metadata) {
        super(api, mv);
        this.arguments = arguments;
        this.locals = locals;
        this.metadata = metadata;
        this.access = access;
    }

    @Override
    public void visitCode() {
        int offset = (access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
        for (int i = 0; i < arguments.length; ++i) {

        }
        super.visitCode();
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        ClassMetadata ownerData = metadata.getClassMetadata(owner);
        if (!ownerData.isJavaScriptObject()) {
            super.visitMethodInsn(opcode, owner, name, desc);
            return;
        }
        AnnotationNode[] nodes = ownerData.getMethodAnnotations(name, desc);
        AnnotationNode propertyAnnot = find(nodes, Type.getInternalName(JSProperty.class));
        AnnotationNode indexerAnnot = find(nodes, Type.getInternalName(JSIndexer.class));
        AnnotationNode consAnnot = find(nodes, Type.getInternalName(JSConstructor.class));
        if (propertyAnnot != null) {
            emitProperty(owner, name, desc);
        } else if (indexerAnnot != null) {
            emitIndexer(desc);
        } else {
            emitInvocation(consAnnot, owner, name, desc);
        }
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if (opcode != Opcodes.CHECKCAST) {
            super.visitTypeInsn(opcode, type);
        }
        ClassMetadata clsMeta = metadata.getClassMetadata(type);
        if (!clsMeta.javaScriptObject) {
            super.visitTypeInsn(opcode, type);
        }
    }

    private void emitProperty(String owner, String name, String desc) {
        if (isProperGetter(name, desc)) {
            String propertyName = name.charAt(0) == 'i' ? cutPrefix(name, 2) : cutPrefix(name, 3);
            mv.visitLdcInsn(propertyName);
            wrap(Type.getType(String.class));
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, JS_CLS, "get", "(L" + JSOBJECT_CLS + ";L" + JSOBJECT_CLS +
                    ";)L" + JSOBJECT_CLS + ";");
            unwrap(Type.getReturnType(desc));
        } else if (isProperSetter(name, desc)) {
            wrap(Type.getArgumentTypes(desc)[0]);
            mv.visitLdcInsn(cutPrefix(name, 3));
            wrap(Type.getType(String.class));
            mv.visitInsn(Opcodes.SWAP);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, JS_CLS, "set", "(L" + JSOBJECT_CLS + ";L" + JSOBJECT_CLS +
                    ";L" + JSOBJECT_CLS + ";)V");
        } else {
            throw new RuntimeException("Method " + owner + "." + name + desc + " is not " +
                    "a proper native JavaScript property declaration");
        }
    }

    private void emitIndexer(String desc) {
        if (isProperGetIndexer(desc)) {
            Type[] params = Type.getArgumentTypes(desc);
            wrap(params[0]);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, JS_CLS, "get", "(L" + JSOBJECT_CLS + ";L" + JSOBJECT_CLS +
                    ";)L" + JSOBJECT_CLS + ";");
            unwrap(Type.getReturnType(desc));
        } else if (isProperSetIndexer(desc)) {
            Type[] params = Type.getArgumentTypes(desc);
            mv.visitInsn(Opcodes.SWAP);
            wrap(params[0]);
            mv.visitInsn(Opcodes.SWAP);
            wrap(params[1]);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, JS_CLS, "set", "(L" + JSOBJECT_CLS + ";L" + JSOBJECT_CLS +
                    ";L" + JSOBJECT_CLS + ";)V");
        }
    }

    private void emitInvocation(AnnotationNode consAnnot, String owner, String name, String desc) {
        boolean isConstructor;
        Type returnType = Type.getReturnType(desc);
        Type[] params = Type.getArgumentTypes(desc);
        if (consAnnot != null) {
            if (!isSupportedType(returnType)) {
                throw new RuntimeException("Method " + owner + "." + name + desc + " is not " +
                        "a proper native JavaScript constructor declaration");
            }
            String consName = (String)find(consAnnot, "value");
            if (consName == null || consName.isEmpty()) {
                if (!consName.startsWith("new") || consName.length() == 3) {
                    throw new RuntimeException("Method " + owner + "." + name + desc + " is not " +
                            "declared as a native JavaScript constructor, but its name does " +
                            "not satisfy conventions");
                }
                consName = name.substring(3);
            }
            name = consName;
            isConstructor = true;
        } else {
            if (returnType.getSort() != Type.VOID && !isSupportedType(returnType)) {
                throw new RuntimeException("Method " + owner + "." + name + desc + " is not " +
                        "a proper native JavaScript method declaration");
            }
            isConstructor = false;
        }
        for (Type param : params) {
            if (!isSupportedType(param)) {
                throw new RuntimeException("Method " + owner + "." + name + desc + " is not " +
                        "a proper native JavaScript method or constructor declaration");
            }
        }
        int minLocal = locals.getMaxLocal(name, desc) + 1;
        int[] paramLocations = new int[params.length];
        int lastParamLoc = minLocal;
        for (int i = params.length - 1; i >= 0; --i) {
            paramLocations[i] = lastParamLoc;
            mv.visitVarInsn(getStoreOpcode(params[i]), lastParamLoc);
            lastParamLoc += params[i].getSize();
        }
        wrap(Type.getObjectType("java/lang/Object"));
        mv.visitLdcInsn(params);
        wrap(Type.getType(String.class));
        StringBuilder invokerDesc = new StringBuilder("(L" + JSOBJECT_CLS + ";L" + JSOBJECT_CLS + ";");
        for (int i = 0; i < params.length; ++i) {
            mv.visitVarInsn(getLoadOpcode(params[i]), paramLocations[i]);
            wrap(params[i]);
            invokerDesc.append("L" + JSOBJECT_CLS + ";");
        }
        invokerDesc.append(")L" + JSOBJECT_CLS + ";");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, JS_CLS, isConstructor ? "instantiate" : "invoke",
                invokerDesc.toString());
        if (returnType.getSort() == Type.VOID) {
            mv.visitInsn(Opcodes.POP);
        } else {
            unwrap(returnType);
        }
    }

    private int getStoreOpcode(Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return Opcodes.ISTORE;
            case Type.LONG:
                return Opcodes.LSTORE;
            case Type.FLOAT:
                return Opcodes.FSTORE;
            case Type.DOUBLE:
                return Opcodes.DSTORE;
            case Type.OBJECT:
            case Type.ARRAY:
                return Opcodes.ASTORE;
            default:
                throw new AssertionError("Unknown type: " + type.getDescriptor());
        }
    }

    private int getLoadOpcode(Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return Opcodes.ILOAD;
            case Type.LONG:
                return Opcodes.LLOAD;
            case Type.FLOAT:
                return Opcodes.FLOAD;
            case Type.DOUBLE:
                return Opcodes.DLOAD;
            case Type.OBJECT:
            case Type.ARRAY:
                return Opcodes.ALOAD;
            default:
                throw new AssertionError("Unknown type: " + type.getDescriptor());
        }
    }

    private void unwrap(Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
                unwrap("unwrapBoolean", Type.BOOLEAN_TYPE);
                return;
            case Type.BYTE:
                unwrap("unwrapByte", Type.BYTE_TYPE);
                return;
            case Type.SHORT:
                unwrap("unwrapShort", Type.SHORT_TYPE);
                return;
            case Type.INT:
                unwrap("unwrapInt", Type.INT_TYPE);
                return;
            case Type.CHAR:
                unwrap("unwrapCharacter", Type.CHAR_TYPE);
                return;
            case Type.DOUBLE:
                unwrap("unwrapDouble", Type.DOUBLE_TYPE);
                return;
            case Type.FLOAT:
                unwrap("unwrapFloat", Type.FLOAT_TYPE);
                return;
            case Type.LONG:
                break;
            case Type.OBJECT: {
                String className = type.getClassName();
                if (className.equals(JSOBJECT_CLS)) {
                    return;
                } else if (className.equals("java/lang/String")) {
                    unwrap("unwrapString", Type.getType(String.class));
                    return;
                } else {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, type.getDescriptor());
                    return;
                }
            }
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    private void unwrap(String methodName, Type resultType) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, JS_CLS, methodName, "(L" + JSOBJECT_CLS + ";)" +
                resultType.getDescriptor());
    }

    private void wrap(Type type) {
        if (type.getSort() == Type.OBJECT && !type.getInternalName().equals("java/lang/String")) {
            mv.visitLdcInsn(type);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, JS_CLS, "cast", "(L" + JSOBJECT_CLS + ";Ljava/lang/Class;)" +
                    "L" + JSOBJECT_CLS + ";");
        }
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, JS_CLS, "wrap", "(" + type.getDescriptor() + ")" +
                "L" + JSOBJECT_CLS + ";");
    }

    private AnnotationNode find(AnnotationNode[] nodes, String annotName) {
        if (nodes == null) {
            return null;
        }
        for (AnnotationNode node : nodes) {
            if (node.desc.equals(annotName)) {
                return node;
            }
        }
        return null;
    }

    private Object find(AnnotationNode node, String valueName) {
        for (int i = 0; i < node.values.size(); i += 2) {
            if (node.values.get(i).equals(valueName)) {
                return node.values.get(i + 1);
            }
        }
        return null;
    }

    private boolean isProperGetter(String name, String desc) {
        Type returnType = Type.getReturnType(desc);
        if (Type.getArgumentTypes(desc).length > 0 || !isSupportedType(returnType)) {
            return false;
        }
        if (returnType.getSort() == Type.BOOLEAN) {
            if (isProperPrefix(name, "is")) {
                return true;
            }
        }
        return isProperPrefix(name, "get");
    }

    private boolean isProperSetter(String name, String desc) {
        Type[] params = Type.getArgumentTypes(desc);
        if (params.length != 1 || !isSupportedType(params[0]) ||
                Type.getReturnType(desc).getSort() != Type.VOID) {
            return false;
        }
        return isProperPrefix(name, "set");
    }

    private boolean isProperGetIndexer(String desc) {
        Type[] params = Type.getArgumentTypes(desc);
        return params.length == 1 && isSupportedType(params[0]) && isSupportedType(Type.getReturnType(desc));
    }

    private boolean isProperSetIndexer(String desc) {
        Type[] params = Type.getArgumentTypes(desc);
        return params.length == 2 && isSupportedType(params[0]) && isSupportedType(params[1]) &&
                Type.getReturnType(desc).getSort() == Type.VOID;
    }


    private boolean isProperPrefix(String name, String prefix) {
        if (!name.startsWith(prefix) || name.length() == prefix.length()) {
            return false;
        }
        char c = name.charAt(prefix.length());
        return Character.isUpperCase(c);
    }

    private String cutPrefix(String name, int prefixLength) {
        if (name.length() == prefixLength + 1) {
            return name.substring(prefixLength).toLowerCase();
        }
        char c = name.charAt(prefixLength + 1);
        if (Character.isUpperCase(c)) {
            return name.substring(prefixLength);
        }
        return Character.toLowerCase(name.charAt(prefixLength)) + name.substring(prefixLength + 1);
    }

    private boolean isSupportedType(Type type) {
        switch (type.getSort()) {
            case Type.VOID:
            case Type.LONG:
                return false;
            case Type.ARRAY:
                return isSupportedType(type.getElementType());
            case Type.OBJECT:
                return type.getClassName().equals("java/lang/String") ||
                        metadata.getClassMetadata(type.getClassName()).isJavaScriptObject();
            default:
                return true;
        }
    }
}
