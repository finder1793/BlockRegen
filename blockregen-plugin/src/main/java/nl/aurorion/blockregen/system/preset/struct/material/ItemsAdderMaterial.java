package nl.aurorion.blockregen.system.preset.struct.material;

import dev.lone.itemsadder.api.CustomBlock;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.system.preset.struct.BlockPreset;
import org.bukkit.block.Block;

@Log
public class ItemsAdderMaterial implements TargetMaterial {

    private final String id;

    public ItemsAdderMaterial(String id) {
        this.id = id;
    }

    @Override
    public boolean check(BlockPreset preset, Block block) {
        CustomBlock customBlock = CustomBlock.byAlreadyPlaced(block);

        if (customBlock == null) {
            return false;
        }

        String placedId = customBlock.getNamespacedID();
        return id.equals(placedId);
    }

    @Override
    public void place(Block block) {
        CustomBlock customBlock = CustomBlock.getInstance(this.id);
        customBlock.place(block.getLocation());
    }

    @Override
    public String toString() {
        return "ItemsAdderMaterial{" +
                "id='" + id + '\'' +
                '}';
    }
}
