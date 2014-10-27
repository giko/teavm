package org.teavm.idea.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by giko on 10/27/2014.
 */
public class TeaVMFacetType extends FacetType<TeaVMFacet, TeaVMFacetConfig> {
    private static final String TYPE_STRING_ID = "TeaVMFacet";

    public static final FacetTypeId<TeaVMFacet> TYPE_ID = new FacetTypeId<TeaVMFacet>(TYPE_STRING_ID);
    private static final String JAVA__MODULE_TYPE = "JAVA_MODULE";

    public TeaVMFacetType() {
        super(TYPE_ID, TYPE_STRING_ID, "TeaVM");
    }

    @Override
    public TeaVMFacetConfig createDefaultConfiguration() {
        return new TeaVMFacetConfig();
    }

    @Override
    public TeaVMFacet createFacet(@NotNull Module module,
                                  String name,
                                  @NotNull TeaVMFacetConfig configuration,
                                  @Nullable Facet underlyingFacet) {
        return new TeaVMFacet(this, module, name, configuration, underlyingFacet);
    }

    @Override
    public boolean isSuitableModuleType(ModuleType moduleType) {
        ModuleType javaModuleType = ModuleTypeManager.getInstance().findByID(JAVA__MODULE_TYPE);
        return javaModuleType != null && moduleType.equals(javaModuleType);
    }

}
