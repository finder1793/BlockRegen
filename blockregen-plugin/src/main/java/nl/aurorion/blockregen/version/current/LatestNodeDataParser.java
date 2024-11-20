package nl.aurorion.blockregen.version.current;

import lombok.NoArgsConstructor;
import nl.aurorion.blockregen.ParseUtil;
import nl.aurorion.blockregen.version.NodeDataDeserializer;
import nl.aurorion.blockregen.version.api.NodeData;
import nl.aurorion.blockregen.version.api.NodeDataParser;
import org.bukkit.Axis;
import org.bukkit.Instrument;
import org.bukkit.Note;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Stairs;

@NoArgsConstructor
public class LatestNodeDataParser implements NodeDataParser {

    private final NodeDataDeserializer<LatestNodeData> nodeDataDeserializer = new NodeDataDeserializer<LatestNodeData>()
            .property("age", (nodeData, value) -> {
                int age = Integer.parseInt(value);
                nodeData.setAge(age);
            })
            .property("facing", (nodeData, value) -> {
                int id = Integer.parseInt(value);
                BlockFace facing = BlockFace.values()[id];
                nodeData.setFacing(facing);
            })
            .property("rotation", (nodeData, value) -> {
                int id = Integer.parseInt(value);
                BlockFace facing = BlockFace.values()[id];
                nodeData.setRotation(facing);
            })
            .property("axis", (nodeData, value) -> {
                int id = Integer.parseInt(value);
                Axis axis = Axis.values()[id];
                nodeData.setAxis(axis);
            })
            .property("stairShape", (nodeData, value) -> {
                int id = Integer.parseInt(value);
                Stairs.Shape species = Stairs.Shape.values()[id];
                nodeData.setStairShape(species);
            })
            .property("noteId", (nodeData, value) -> {
                byte id = Byte.parseByte(value);
                nodeData.setNoteId(id);
            })
            .property("instrument", (nodeData, value) -> {
                nodeData.setInstrument(ParseUtil.parseEnum(value, Instrument.class));
            })
            .property("octave", (nodeData, value) -> {
                int octave = Integer.parseInt(value);
                nodeData.setOctave(octave);
            })
            .property("sharped", (nodeData, value) -> {
                nodeData.setSharped(Boolean.parseBoolean(value));
            })
            .property("tone", (nodeData, value) -> {
                nodeData.setTone(ParseUtil.parseEnum(value, Note.Tone.class));
            })
            .property("powered", (nodeData, value) -> {
                nodeData.setPowered(Boolean.parseBoolean(value));
            })
            .property("east", (nodeData, value) -> {
                if ("true".equalsIgnoreCase(value)) {
                    nodeData.addFace(BlockFace.EAST);
                }
            })
            .property("north", (nodeData, value) -> {
                if ("true".equalsIgnoreCase(value)) {
                    nodeData.addFace(BlockFace.NORTH);
                }
            })
            .property("south", (nodeData, value) -> {
                if ("true".equalsIgnoreCase(value)) {
                    nodeData.addFace(BlockFace.SOUTH);
                }
            })
            .property("west", (nodeData, value) -> {
                if ("true".equalsIgnoreCase(value)) {
                    nodeData.addFace(BlockFace.WEST);
                }
            })
            .property("up", (nodeData, value) -> {
                if ("true".equalsIgnoreCase(value)) {
                    nodeData.addFace(BlockFace.UP);
                }
            })
            .property("down", (nodeData, value) -> {
                if ("true".equalsIgnoreCase(value)) {
                    nodeData.addFace(BlockFace.DOWN);
                }
            })
            .property("skull", (LatestNodeData::setSkull));

    @Override
    public NodeData parse(String input) throws IllegalArgumentException {
        LatestNodeData nodeData = new LatestNodeData();
        nodeDataDeserializer.deserialize(nodeData, input);
        return nodeData;
    }
}
