package nl.aurorion.blockregen.system.material.parser;

import com.cryptomorin.xseries.XMaterial;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.system.preset.struct.material.MinecraftMaterial;
import nl.aurorion.blockregen.system.preset.struct.material.TargetMaterial;
import nl.aurorion.blockregen.util.ParseUtil;

public class MinecraftMaterialParser implements MaterialParser {

    private final BlockRegen plugin;

    public MinecraftMaterialParser(BlockRegen plugin) {
        this.plugin = plugin;
    }

    @Override
    public TargetMaterial parseMaterial(String input) throws IllegalArgumentException {
        XMaterial xMaterial = ParseUtil.parseMaterial(input, true);

        if (xMaterial == null) {
            throw new IllegalArgumentException("Could not parse minecraft material: " + input);
        }

        return new MinecraftMaterial(plugin, xMaterial);
    }
}
