package nl.aurorion.blockregen.system.material;

import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.system.material.parser.MaterialParser;
import nl.aurorion.blockregen.system.preset.struct.material.TargetMaterial;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Log
public class MaterialManager {

    private final BlockRegen plugin;

    private final Map<String, MaterialParser> registeredParsers = new HashMap<>();

    public MaterialManager(BlockRegen plugin) {
        this.plugin = plugin;
    }

    /**
     * Register a material parser under a prefix.
     * If there is a parser already registered, overwrite (aka Map#put).
     * <p>
     * A null prefix parser is used for inputs with no prefix.
     * <p>
     * A prefix cannot match a material name, otherwise the parsing screws up.
     * We could use a different separator, but screw it, a colon looks cool.
     */
    public void registerParser(@Nullable String prefix, @NotNull MaterialParser parser) {
        registeredParsers.put((prefix == null ? null : prefix.toLowerCase()), parser);
        log.fine(String.format("Registered material parser with prefix %s", prefix));
    }

    public MaterialParser getParser(@Nullable String prefix) {
        return this.registeredParsers.get((prefix == null ? null : prefix.toLowerCase()));
    }

    /**
     * Parse a material using registered parsers.
     *
     * @param input Input string, format (prefix:?)(material-name[nodedata,...])
     * @return Parsed material or null when no parser was found.
     * @throws IllegalArgumentException When the parser is unable to parse the material.
     */
    public @Nullable TargetMaterial parseMaterial(@NotNull String input) throws IllegalArgumentException {
        // Separate parts
        String[] parts = input.split(":");

        // First either prefix or material

        MaterialParser parser = getParser(parts[0].toLowerCase());

        if (parser == null) {
            parser = getParser(null);

            if (parser == null) {
                log.fine(String.format("No valid parser found for material input %s", input));
                return null;
            }
        } else {
            // remove parts[0] aka the parser prefix
            parts = Arrays.copyOfRange(parts, 1, parts.length);
        }

        return parser.parseMaterial(parts[0]);
    }
}
