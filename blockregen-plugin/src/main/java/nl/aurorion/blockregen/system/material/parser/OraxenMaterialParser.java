package nl.aurorion.blockregen.system.material.parser;

import io.th0rgal.oraxen.api.OraxenBlocks;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.system.preset.struct.material.OraxenMaterial;
import nl.aurorion.blockregen.system.preset.struct.material.TargetMaterial;

public class OraxenMaterialParser implements MaterialParser {

    private final BlockRegen plugin;

    public OraxenMaterialParser(BlockRegen plugin) {
        this.plugin = plugin;
    }

    @Override
    public TargetMaterial parseMaterial(String input) throws IllegalArgumentException {
        if (!OraxenBlocks.isOraxenBlock(input)) {
            throw new IllegalArgumentException(input + " is not an Oraxen block");
        }

        return new OraxenMaterial(this.plugin, input);
    }
}
