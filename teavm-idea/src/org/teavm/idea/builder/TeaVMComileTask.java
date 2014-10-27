package org.teavm.idea.builder;

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileTask;
import org.teavm.idea.facet.TeaVMFacetConfig;

/**
 * Created by giko on 10/27/2014.
 */
public class TeaVMComileTask implements CompileTask {
    private TeaVMFacetConfig config;

    public TeaVMComileTask(TeaVMFacetConfig config) {
        this.config = config;
    }

    @Override
    public boolean execute(CompileContext context) {
        return false;
    }
}
