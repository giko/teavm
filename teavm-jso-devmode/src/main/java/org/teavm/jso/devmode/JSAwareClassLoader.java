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

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.teavm.jso.bytecode.JSObjectTransformer;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class JSAwareClassLoader extends ClassLoader {
    private List<String> packagesToInstrument = new ArrayList<>();
    private JSObjectTransformer transformer = new JSObjectTransformer();

    public JSAwareClassLoader(ClassLoader innerClassLoader) {
        super(innerClassLoader);
    }

    public void addPackageToInstrument(String packageName) {
        packagesToInstrument.add(packageName + ".");
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        for (String packageName : packagesToInstrument) {
            if (name.startsWith(packageName)) {
                return findClass(name);
            }
        }
        return super.loadClass(name, resolve);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes;
        try {
            InputStream input = getResourceAsStream(name.replace('.', '/') + ".class");
            bytes = IOUtils.toByteArray(input);
            byte[] transformedBytes = transformer.transform(this, name, null, null, bytes);
            if (transformedBytes != null) {
                /*CheckClassAdapter.verify(new ClassReader(transformedBytes), true, new PrintWriter(System.err));
                new ClassReader(transformedBytes).accept(new TraceClassVisitor(new PrintWriter(System.err)), 0);*/
                bytes = transformedBytes;
            }
        } catch (IOException e) {
            throw new ClassNotFoundException("IO error occured finding class " + name, e);
        } catch (IllegalClassFormatException e) {
            throw new ClassNotFoundException("Error transforming class " + name, e);
        }
        return defineClass(name, bytes, 0, bytes.length);
    }
}
