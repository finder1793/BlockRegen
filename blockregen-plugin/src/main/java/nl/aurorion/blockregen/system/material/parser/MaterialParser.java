package nl.aurorion.blockregen.system.material.parser;

import nl.aurorion.blockregen.system.preset.struct.material.TargetMaterial;

public interface MaterialParser {

    /**
     * Parse a TargetMaterial from an input string.
     *
     * @param input String to parse from with the material prefix already removed. (ex.: 'oraxen:caveblock', input = 'caveblock').
     * @return Parsed TargetMaterial
     *
     * @throws IllegalArgumentException if the provided {@code input} is not a valid oraxen block id
     */
    TargetMaterial parseMaterial(String input) throws IllegalArgumentException;

    // Return true if the material syntax contains colons.
    default boolean containsColon() {
        return false;
    }
}
