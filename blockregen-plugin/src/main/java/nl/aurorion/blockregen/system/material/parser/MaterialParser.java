package nl.aurorion.blockregen.system.material.parser;

import nl.aurorion.blockregen.system.preset.struct.material.TargetMaterial;
import org.jetbrains.annotations.NotNull;

public interface MaterialParser {

    /**
     * Parse a TargetMaterial from an input string.
     *
     * @param input String to parse from with the material prefix already removed. (ex.: 'oraxen:caveblock', input = 'caveblock').
     * @return Parsed TargetMaterial
     * @throws IllegalArgumentException if the provided {@code input} is not a valid oraxen block id
     */
    @NotNull
    TargetMaterial parseMaterial(String input) throws IllegalArgumentException;

    /**
     * @return True if the material syntax contains colons.
     */
    default boolean containsColon() {
        return false;
    }
}
