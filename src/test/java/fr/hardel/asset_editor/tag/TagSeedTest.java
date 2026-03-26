package fr.hardel.asset_editor.tag;

import io.netty.buffer.Unpooled;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TagSeedTest {

    @Test
    void streamCodecRoundTripPreservesFullSeedShape() {
        TagSeed seed = new TagSeed(
            List.of(
                new TagSeedEntry(Identifier.fromNamespaceAndPath("minecraft", "axes"), true, true),
                new TagSeedEntry(Identifier.fromNamespaceAndPath("minecraft", "diamond_sword"), false, false)
            ),
            List.of(new TagSeedEntry(Identifier.fromNamespaceAndPath("minecraft", "wooden_sword"), false, true)),
            true
        );

        var buffer = Unpooled.buffer();
        TagSeed.STREAM_CODEC.encode(buffer, seed);
        TagSeed decoded = TagSeed.STREAM_CODEC.decode(buffer);

        assertEquals(seed, decoded);
        assertEquals(List.of("#minecraft:axes", "minecraft:diamond_sword?"), decoded.toTagFile().entries().stream().map(Object::toString).toList());
        assertEquals(List.of("minecraft:wooden_sword"), decoded.toTagFile().exclude().stream().map(Object::toString).toList());
        assertEquals(true, decoded.toTagFile().replace());
    }
}
