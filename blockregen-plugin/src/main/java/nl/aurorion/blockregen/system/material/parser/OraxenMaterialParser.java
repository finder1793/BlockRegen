package nl.aurorion.blockregen.system.material.parser;

import io.th0rgal.oraxen.api.OraxenBlocks;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.system.preset.struct.material.OraxenMaterial;
import nl.aurorion.blockregen.system.preset.struct.material.TargetMaterial;
import org.jetbrains.annotations.NotNull;

public class OraxenMaterialParser implements MaterialParser {

    private final BlockRegen plugin;

    public OraxenMaterialParser(BlockRegen plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull TargetMaterial parseMaterial(String input) throws IllegalArgumentException {
        if (!OraxenBlocks.isOraxenBlock(input)) {
            throw new IllegalArgumentException(String.format("'%s' is not an Oraxen block.", input));
        }

        return new OraxenMaterial(this.plugin, input);
    }
}
