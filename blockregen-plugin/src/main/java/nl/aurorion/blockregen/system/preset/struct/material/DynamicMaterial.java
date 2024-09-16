package nl.aurorion.blockregen.system.preset.struct.material;

import java.util.*;

import lombok.extern.java.Log;
import org.jetbrains.annotations.NotNull;

import lombok.Getter;
import lombok.Setter;
import nl.aurorion.blockregen.BlockRegen;

@Log
public class DynamicMaterial {

    @Getter
    @Setter
    private TargetMaterial defaultMaterial;

    private final List<TargetMaterial> valuedMaterials = new ArrayList<>();

    public DynamicMaterial(TargetMaterial defaultMaterial) {
        this.defaultMaterial = defaultMaterial;
    }

    public DynamicMaterial(TargetMaterial defaultMaterial, Collection<TargetMaterial> valuedMaterials) {
        this.defaultMaterial = defaultMaterial;
        this.valuedMaterials.addAll(valuedMaterials);
    }

    public List<TargetMaterial> getValuedMaterials() {
        return Collections.unmodifiableList(valuedMaterials);
    }

    private TargetMaterial pickRandom() {
        TargetMaterial pickedMaterial = valuedMaterials.get(BlockRegen.getInstance().getRandom().nextInt(valuedMaterials.size()));
        return pickedMaterial != null ? pickedMaterial : defaultMaterial;
    }

    @NotNull
    public TargetMaterial get() {
        return valuedMaterials.isEmpty() ? defaultMaterial : pickRandom();
    }
}
