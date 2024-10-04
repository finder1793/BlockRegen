package nl.aurorion.blockregen.system.preset.struct.material;

import java.util.*;

import lombok.extern.java.Log;
import nl.aurorion.blockregen.util.DiscreteGenerator;
import org.jetbrains.annotations.NotNull;

@Log
public class DynamicMaterial {

    private final DiscreteGenerator<TargetMaterial> generator;

    private final Map<TargetMaterial, Double> valuedMaterials;

    private DynamicMaterial(Map<TargetMaterial, Double> valuedMaterials) {
        this.valuedMaterials = valuedMaterials;
        this.generator = DiscreteGenerator.fromProbabilityFunction(valuedMaterials);
    }

    public static DynamicMaterial withOnlyDefault(TargetMaterial defaultMaterial) {
        Map<TargetMaterial, Double> valued = new HashMap<>();
        valued.put(defaultMaterial, 1.0);
        return new DynamicMaterial(valued);
    }

    public static DynamicMaterial from(Map<TargetMaterial, Double> valuedMaterials) {
        return new DynamicMaterial(valuedMaterials);
    }

    public Map<TargetMaterial, Double> getValuedMaterials() {
        return Collections.unmodifiableMap(valuedMaterials);
    }

    @NotNull
    public TargetMaterial get() {
        return this.generator.next();
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<TargetMaterial, Double> entry : valuedMaterials.entrySet()) {
            builder.append(String.format("%s: %.2f", entry.getKey(), entry.getValue())).append(",");
        }
        return "DynamicMaterial{" +
                "valuedMaterials=" + builder.substring(0, builder.length() - 1) +
                '}';
    }
}
