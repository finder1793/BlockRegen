package nl.aurorion.blockregen.system.material.parser;

import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.system.preset.struct.material.MMOIItemsMaterial;
import nl.aurorion.blockregen.system.preset.struct.material.TargetMaterial;

public class MMOItemsMaterialParser implements MaterialParser{

    private final BlockRegen plugin;

    public MMOItemsMaterialParser(BlockRegen plugin) {
        this.plugin = plugin;
    }

    @Override
    public TargetMaterial parseMaterial(String input) throws IllegalArgumentException {
        int id;
        try {
            id = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid MMOItem block id: " + input);
        }

        return new MMOIItemsMaterial(plugin, id);
    }
}
