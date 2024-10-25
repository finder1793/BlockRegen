package nl.aurorion.blockregen.system.material.parser;

import dev.lone.itemsadder.api.CustomBlock;
import nl.aurorion.blockregen.system.preset.struct.material.ItemsAdderMaterial;
import nl.aurorion.blockregen.system.preset.struct.material.TargetMaterial;

public class ItemsAdderMaterialParser implements MaterialParser {

    @Override
    public TargetMaterial parseMaterial(String input) throws IllegalArgumentException {

        if (!CustomBlock.isInRegistry(input)) {
            throw new IllegalArgumentException(input + " is not a valid ItemsAdder custom block.");
        }

        return new ItemsAdderMaterial(input);
    }

    @Override
    public boolean containsColon() {
        return true;
    }
}
