package org.teavm.idea.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.teavm.idea.builder.TeaVMComileTask;

/**
 * Created by giko on 10/27/2014.
 */
public class TeaVMFacet extends Facet<TeaVMFacetConfig> {
    public TeaVMFacet(@NotNull FacetType facetType, @NotNull Module module, @NotNull String name, @NotNull TeaVMFacetConfig configuration, Facet underlyingFacet) {
        super(facetType, module, name, configuration, underlyingFacet);
    }

    @Override
    public void initFacet() {
        super.initFacet();
        CompilerManager.getInstance(this.getModule().getProject()).addAfterTask(new TeaVMComileTask(this.getConfiguration()));
    }
}
