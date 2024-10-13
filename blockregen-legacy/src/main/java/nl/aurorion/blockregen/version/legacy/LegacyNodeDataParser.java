package nl.aurorion.blockregen.version.legacy;

import lombok.extern.java.Log;
import nl.aurorion.blockregen.version.api.NodeData;
import nl.aurorion.blockregen.version.api.NodeDataParser;
import org.bukkit.CropState;
import org.bukkit.TreeSpecies;
import org.bukkit.block.BlockFace;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log
public class LegacyNodeDataParser implements NodeDataParser {

    private static final Pattern dataPattern = Pattern.compile("\\[(.*)]");

    @Override
    public NodeData parse(String input) {
        // Split out the [] section with actual data
        Matcher matcher = dataPattern.matcher(input);

        if (!matcher.find()) {
            return null;
        }

        String dataString = matcher.group(1);

        LegacyNodeData nodeData = new LegacyNodeData();

        String[] dataParts = dataString.split(",");

        for (String dataPart : dataParts) {
            // age=7
            String[] entryParts = dataPart.split("=");
            String key = entryParts[0];
            String value = entryParts[1];

            switch (key) {
                case "age": {
                    int age = Integer.parseInt(value);
                    CropState state = CropState.values()[age];
                    nodeData.setCropState(state);
                    break;
                }
                case "facing": {
                    int id = Integer.parseInt(value);
                    BlockFace facing = BlockFace.values()[id];
                    nodeData.setFacing(facing);
                    break;
                }
                case "species": {
                    int id = Integer.parseInt(value);
                    TreeSpecies species = TreeSpecies.values()[id];
                    nodeData.setTreeSpecies(species);
                    break;
                }
                case "inverted": {
                    boolean inverted = Boolean.parseBoolean(value);
                    nodeData.setInverted(inverted);
                    break;
                }
                default:
                    log.warning(String.format("Unknown property in block data: %s", key));
                    break;
            }
        }

        return nodeData;
    }
}
