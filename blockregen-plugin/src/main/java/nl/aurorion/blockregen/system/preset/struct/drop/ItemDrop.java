package nl.aurorion.blockregen.system.preset.struct.drop;

import com.cryptomorin.xseries.XMaterial;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.StringUtil;
import nl.aurorion.blockregen.system.preset.struct.Amount;
import nl.aurorion.blockregen.system.preset.struct.BlockPreset;
import nl.aurorion.blockregen.util.ParseUtil;
import nl.aurorion.blockregen.util.TextUtil;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Log
public class ItemDrop {

    private final XMaterial material;

    @Setter
    private Amount amount = new Amount(1);

    @Setter
    private String displayName;

    @Setter
    private List<String> lore = new ArrayList<>();

    @Setter
    private Set<Enchant> enchants = new HashSet<>();

    @Setter
    private Set<ItemFlag> itemFlags = new HashSet<>();

    @Setter
    private boolean dropNaturally = false;

    @Setter
    private ExperienceDrop experienceDrop;

    @Setter
    private Amount chance;

    @Setter
    private Integer customModelData;

    public ItemDrop(XMaterial material) {
        this.material = material;
    }


    /**
     * Compose this Drop into an item stack.
     *
     * @param player Player to give the rewards to.
     * @return Created item stack.
     */
    @Nullable
    public ItemStack toItemStack(Player player, Function<String, String> parser) {

        // x/100% chance to drop
        if (chance != null) {
            double threshold = chance.getDouble();
            double roll = BlockRegen.getInstance().getRandom().nextDouble() * 100;

            if (roll > threshold) {
                log.fine(String.format("Drop %s failed chance roll, %.2f > %.2f", this, roll, threshold));
                return null;
            }
        }

        int amount = this.amount.getInt();

        if (amount <= 0) {
            return null;
        }

        ItemStack itemStack = material.parseItem();

        if (itemStack == null) {
            return null;
        }

        itemStack.setAmount(amount);

        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta == null) {
            return null;
        }

        if (displayName != null) {
            itemMeta.setDisplayName(StringUtil.color(parser.apply(displayName)));
        }

        if (lore != null) {
            List<String> lore = new ArrayList<>(this.lore);

            lore.replaceAll(o -> StringUtil.color(parser.apply(o)));

            itemMeta.setLore(lore);
        }

        enchants.forEach(enchant -> enchant.apply(itemMeta));
        itemMeta.addItemFlags(itemFlags.toArray(new ItemFlag[0]));

        // On 1.14+, apply custom model data
        if (BlockRegen.getInstance().getVersionManager().useCustomModelData()) {
            // Add PDC with custom model data
            if (customModelData != null) {
                itemMeta.setCustomModelData(customModelData);
                log.fine(String.format("Setting custom model data of %d", customModelData));
            }
        }

        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

    @Nullable
    public static ItemDrop load(ConfigurationSection section, BlockPreset preset) {

        if (section == null) {
            return null;
        }

        XMaterial material = ParseUtil.parseMaterial(section.getString("material"));

        if (material == null) {
            log.warning("Could not load item drop at " + section.getCurrentPath() + ", material is invalid.");
            return null;
        }

        ItemDrop drop = new ItemDrop(material);

        drop.setAmount(Amount.load(section, "amount", 1));
        drop.setDisplayName(section.getString("name"));
        drop.setLore(section.getStringList("lores"));

        drop.setEnchants(Enchant.load(section.getStringList("enchants")));
        drop.setItemFlags(section.getStringList("flags").stream()
                .map(str -> ParseUtil.parseEnum(str, ItemFlag.class,
                        e -> log.warning("Could not parse ItemFlag from " + str)))
                .collect(Collectors.toSet()));

        drop.setDropNaturally(section.getBoolean("drop-naturally", preset.isDropNaturally()));

        drop.setExperienceDrop(ExperienceDrop.load(section.getConfigurationSection("exp"), drop));
        drop.setChance(Amount.load(section, "chance", 100));

        drop.setCustomModelData(ParseUtil.parseInteger(section.getString("custom-model-data")));

        return drop;
    }
}