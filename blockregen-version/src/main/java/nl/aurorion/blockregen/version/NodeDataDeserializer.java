package nl.aurorion.blockregen.version;

import lombok.extern.java.Log;
import nl.aurorion.blockregen.version.api.NodeData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log
public class NodeDataDeserializer<T extends NodeData> {

    public interface PropertyDeserializer<T extends NodeData> {
        void deserialize(T nodeData, String value);
    }

    private static final Pattern dataPattern = Pattern.compile("\\[(.*)]");

    private final Map<String, PropertyDeserializer<T>> properties = new HashMap<>();

    public NodeDataDeserializer<T> property(String key, PropertyDeserializer<T> deserializer) {
        properties.put(key, deserializer);
        return this;
    }

    private Pattern propertyEquals = null;

    private Pattern generatePropertyEqualsPattern() {
        Set<String> properties = this.properties.keySet();
        return Pattern.compile(String.format("(?<=%s)=", String.join("|", properties)));
    }

    public void deserialize(T nodeData, String input) throws IllegalArgumentException {
        log.fine("Deserializing " + input);

        if (propertyEquals == null) {
            this.propertyEquals = generatePropertyEqualsPattern();
        }

        // Split out the [] section with actual data
        Matcher matcher = dataPattern.matcher(input);

        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid node data format");
        }

        String dataString = matcher.group(1);

        String[] dataParts = dataString.split(",");

        for (String dataPart : dataParts) {
            String[] entryParts = propertyEquals.split(dataPart);
            String key = entryParts[0];
            String value = entryParts[1];

            PropertyDeserializer<T> deserializer = properties.get(key);

            if (deserializer == null) {
                log.warning(String.format("Unknown node data property %s", key));
                continue;
            }

            deserializer.deserialize(nodeData, value);
        }
    }
}
