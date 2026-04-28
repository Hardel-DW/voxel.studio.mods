package fr.hardel.asset_editor.client.mcdoc.ast;

import java.util.Map;
import java.util.Optional;
import java.util.List;

public record Attribute(String name, Optional<AttributeValue> value) {

    public sealed interface AttributeValue permits TypeValue, TreeValue {}

    public record TypeValue(McdocType type) implements AttributeValue {}

    public record TreeValue(
        Map<String, AttributeValue> named,
        List<AttributeValue> positional
    ) implements AttributeValue {

        public TreeValue {
            named = Map.copyOf(named);
            positional = List.copyOf(positional);
        }

        public Optional<AttributeValue> get(String key) {
            return Optional.ofNullable(named.get(key));
        }
    }
}
