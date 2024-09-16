package nl.aurorion.blockregen.system.preset.struct.material;

import com.cryptomorin.xseries.XMaterial;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.system.preset.struct.BlockPreset;
import org.bukkit.block.Block;

public class MinecraftMaterial implements TargetMaterial {

    private final BlockRegen plugin;

    private final XMaterial material;

    public MinecraftMaterial(BlockRegen plugin, XMaterial material) {
        this.plugin = plugin;
        this.material = material;
    }

    @Override
    public boolean check(BlockPreset preset, Block block) {
        return this.plugin.getVersionManager().getMethods().compareType(block, this.material);
    }

    @Override
    public void place(Block block) {
        plugin.getVersionManager().getMethods().setType(block, this.material);
    }

    @Override
    public String toString() {
        return "MinecraftMaterial{" +
                "material=" + material +
                '}';
    }
}
