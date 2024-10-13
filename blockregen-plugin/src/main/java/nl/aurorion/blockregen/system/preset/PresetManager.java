package nl.aurorion.blockregen.system.preset;

import com.cryptomorin.xseries.XSound;
import com.google.common.base.Strings;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.system.event.struct.PresetEvent;
import nl.aurorion.blockregen.system.material.parser.MaterialParser;
import nl.aurorion.blockregen.system.preset.struct.Amount;
import nl.aurorion.blockregen.system.preset.struct.BlockPreset;
import nl.aurorion.blockregen.system.preset.struct.PresetConditions;
import nl.aurorion.blockregen.system.preset.struct.PresetRewards;
import nl.aurorion.blockregen.system.preset.struct.material.DynamicMaterial;
import nl.aurorion.blockregen.system.preset.struct.material.TargetMaterial;
import nl.aurorion.blockregen.system.region.struct.RegenerationRegion;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Log
public class PresetManager {

    private final BlockRegen plugin;

    private final Map<String, BlockPreset> presets = new HashMap<>();

    public PresetManager(BlockRegen plugin) {
        this.plugin = plugin;
    }

    public BlockPreset getPreset(@Nullable String name) {
        return presets.get(name);
    }

    @Nullable
    public BlockPreset getPreset(@NotNull Block block) {
        for (BlockPreset preset : this.presets.values()) {
            if (preset.getTargetMaterial().check(preset, block)) {
                return preset;
            }
        }
        return null;
    }

    @Nullable
    public BlockPreset getPreset(@NotNull Block block, @NotNull RegenerationRegion region) {
        for (BlockPreset preset : this.presets.values()) {
            if (preset.getTargetMaterial().check(preset, block) && region.hasPreset(preset.getName())) {
                return preset;
            }
        }
        return null;
    }

    public Map<String, BlockPreset> getPresets() {
        return Collections.unmodifiableMap(presets);
    }

    public void loadAll() {
        presets.clear();

        // Clear all events before loading.
        plugin.getEventManager().clearEvents();

        ConfigurationSection blocks = plugin.getFiles().getBlockList().getFileConfiguration()
                .getConfigurationSection("Blocks");

        if (blocks == null) {
            return;
        }

        for (String key : blocks.getKeys(false)) {
            load(key);
        }

        log.info("Loaded " + presets.size() + " block preset(s)...");
        log.info("Added " + plugin.getEventManager().getLoadedEvents().size() + " event(s)...");
    }

    public void load(String name) {
        FileConfiguration file = plugin.getFiles().getBlockList().getFileConfiguration();

        ConfigurationSection section = file.getConfigurationSection("Blocks." + name);

        if (section == null) {
            return;
        }

        BlockPreset preset = new BlockPreset(name);

        String targetMaterialInput = section.getString("target-material", name);

        // Target material
        TargetMaterial targetMaterial;
        try {
            targetMaterial = this.plugin.getMaterialManager().parseMaterial(targetMaterialInput);
            if (targetMaterial == null) {
                log.warning(String.format("Could not load preset %s, invalid target material %s.", name, targetMaterialInput));
                return;
            }
            preset.setTargetMaterial(targetMaterial);
        } catch (IllegalArgumentException e) {
            log.warning(String.format("Could not load preset %s: %s", name, e.getMessage()));
            return;
        }
        log.fine(String.format("target-material: %s", preset.getTargetMaterial()));

        // Replace material
        String replaceMaterial = section.getString("replace-block");

        if (Strings.isNullOrEmpty(replaceMaterial)) {
            replaceMaterial = "AIR";
        }

        try {
            preset.setReplaceMaterial(this.loadDynamicMaterial(replaceMaterial));
        } catch (IllegalArgumentException e) {
            log.warning("Dynamic material ( " + replaceMaterial + " ) in replace-block material for " + name
                    + " is invalid: " + e.getMessage());
            return;
        }
        log.fine(String.format("replace-material: %s", preset.getReplaceMaterial()));

        // Regenerate into
        String regenerateIntoInput = section.getString("regenerate-into");

        if (Strings.isNullOrEmpty(regenerateIntoInput)) {
            preset.setRegenMaterial(DynamicMaterial.withOnlyDefault(targetMaterial));
        } else {
            try {
                preset.setRegenMaterial(this.loadDynamicMaterial(regenerateIntoInput));
            } catch (IllegalArgumentException e) {
                log.warning("Dynamic material ( " + regenerateIntoInput + " ) in regenerate-into material for " + name
                        + " is invalid: " + e.getMessage());
                return;
            }
        }
        log.fine(String.format("regenerate-into: %s", preset.getRegenMaterial()));

        // Delay
        preset.setDelay(Amount.load(file, "Blocks." + name + ".regen-delay", 3));

        // Natural break
        preset.setNaturalBreak(section.getBoolean("natural-break", true));

        // Apply fortune
        preset.setApplyFortune(section.getBoolean("apply-fortune", true));

        // Drop naturally
        preset.setDropNaturally(section.getBoolean("drop-naturally", true));

        // Block Break Sound
        String sound = section.getString("sound");

        if (!Strings.isNullOrEmpty(sound)) {
            Optional<XSound> xSound = XSound.matchXSound(sound);
            if (xSound.isEmpty()) {
                log.warning("Sound " + sound + " in preset " + name + " is invalid.");
            } else
                preset.setSound(xSound.get());
        }

        // Particle
        String particleName = section.getString("particles");

        if (!Strings.isNullOrEmpty(particleName))
            preset.setParticle(particleName);

        String regenParticle = section.getString("regeneration-particles");

        if (!Strings.isNullOrEmpty(regenParticle))
            preset.setRegenerationParticle(regenParticle);

        // Conditions
        PresetConditions conditions = new PresetConditions();

        // Tools
        String toolsRequired = section.getString("tool-required");
        if (!Strings.isNullOrEmpty(toolsRequired)) {
            conditions.setToolsRequired(toolsRequired);
        }

        // Enchants
        String enchantsRequired = section.getString("enchant-required");
        if (!Strings.isNullOrEmpty(enchantsRequired)) {
            conditions.setEnchantsRequired(enchantsRequired);
        }

        // Jobs
        if (plugin.getJobsProvider() != null) {
            String jobsRequired = section.getString("jobs-check");
            if (!Strings.isNullOrEmpty(jobsRequired)) {
                conditions.setJobsRequired(jobsRequired);
            }
        }

        preset.setConditions(conditions);

        // Rewards
        PresetRewards rewards = PresetRewards.load(section, preset);

        preset.setRewards(rewards);

        PresetEvent event = PresetEvent.load(section.getConfigurationSection("event"), name, preset);

        if (event != null)
            plugin.getEventManager().addEvent(event);

        presets.put(name, preset);
    }

    private DynamicMaterial loadDynamicMaterial(String input) throws IllegalArgumentException {
        if (Strings.isNullOrEmpty(input)) {
            throw new IllegalArgumentException("Input string cannot be null");
        }

        // clear blanks?
        input = input.replace(" ", "").trim();

        List<String> materials;
        Map<TargetMaterial, Double> valuedMaterials = new HashMap<>();

        materials = Arrays.asList(input.split(";"));

        if (materials.isEmpty()) {
            throw new IllegalArgumentException("Dynamic material " + input + " doesn't have the correct syntax");
        }

        if (materials.size() == 1) {
            log.fine(String.format("%s -> single material", input));
            return DynamicMaterial.withOnlyDefault(this.plugin.getMaterialManager().parseMaterial(materials.getFirst()));
        }

        // Materials without a chance.
        List<TargetMaterial> restMaterials = new ArrayList<>();

        for (String material : materials) {

            // Separate parts
            String[] parts = material.split(":");

            // First either prefix or material

            MaterialParser parser = this.plugin.getMaterialManager().getParser(parts[0].toLowerCase());

            if (parser == null) {
                parser = this.plugin.getMaterialManager().getParser(null);

                if (parser == null) {
                    log.fine(String.format("No valid parser found for material %s in input %s", material, input));
                    continue;
                }
            } else {
                // remove parts[0] aka the parser prefix
                parts = Arrays.copyOfRange(parts, 1, parts.length);
            }

            TargetMaterial mat = parser.parseMaterial(parts[0]);

            // chance
            if (parts.length == 2) {
                double chance = Double.parseDouble(parts[1]);
                // Chance in config is in %, go into <0; 1>
                valuedMaterials.put(mat, chance / 100);
                log.fine(String.format("Added material %s at chance %.2f%%", material, chance));
            } else {
                restMaterials.add(mat);
            }
        }

        double rest = 1.0 - valuedMaterials.values().stream().mapToDouble(e -> e).sum();

        if (restMaterials.size() == 1) {
            valuedMaterials.put(restMaterials.getFirst(), rest);
        } else {
            // Split the rest of the chance between the materials.
            double chance = rest / restMaterials.size();
            restMaterials.forEach(mat -> valuedMaterials.put(mat, chance));
        }

        return DynamicMaterial.from(valuedMaterials);
    }
}