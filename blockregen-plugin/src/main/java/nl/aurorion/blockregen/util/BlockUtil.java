package nl.aurorion.blockregen.util;

import com.cryptomorin.xseries.XMaterial;
import lombok.experimental.UtilityClass;
import nl.aurorion.blockregen.BlockRegen;
import org.bukkit.block.Block;

@UtilityClass
public class BlockUtil {

    public String blockToString(Block block) {
        return "Block{" + LocationUtil.locationToString(block.getLocation()) + ",type=" + block.getType() + "}";
    }

    public static boolean isMultiblockCrop(BlockRegen plugin, Block block) {
        XMaterial type = plugin.getVersionManager().getMethods().getType(block);
        return isMultiblockCrop(type);
    }

    public static boolean isMultiblockCrop(XMaterial type) {
        return type == XMaterial.CACTUS ||
                type == XMaterial.SUGAR_CANE ||
                type == XMaterial.BAMBOO;
    }
}
