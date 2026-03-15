# Fabric 1.21.x Networking (Mojmap)

Reference documentation for custom networking in Fabric 1.21.11 using official Mojang mappings.

---

## Architecture Overview

Fabric networking is built on top of Minecraft's `CustomPacketPayload` system. The flow is:

1. **Define** a payload record implementing `CustomPacketPayload`
2. **Register** the payload type + codec via `PayloadTypeRegistry`
3. **Register a handler** via `ServerPlayNetworking` or `ClientPlayNetworking`
4. **Send** the payload from either side

All payloads are serialized via `StreamCodec<RegistryFriendlyByteBuf, T>` — not `Codec<T>` (DFU). Stream codecs are imperative (read/write bytes), not declarative like DFU codecs.

---

## 1. Payload Definition

Every custom packet is a Java `record` implementing `CustomPacketPayload`. Each needs:
- A static `Type<T>` ID (namespaced identifier)
- A static `StreamCodec` for serialization
- An override of `type()` returning the ID

### Payload with fields

```java
public record SpellCast(Identifier spellId) implements CustomPacketPayload {
    public static final Type<SpellCast> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath("mymod", "spell_cast"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpellCast> CODEC =
        StreamCodec.composite(
            Identifier.STREAM_CODEC, SpellCast::spellId,
            SpellCast::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

### Stateless payload (no fields)

```java
public record MultiJump() implements CustomPacketPayload {
    public static final Type<MultiJump> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath("mymod", "multi_jump"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MultiJump> CODEC =
        StreamCodec.unit(new MultiJump());

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

### Multi-field payload

```java
public record MyPayload(Identifier id, int value, boolean flag) implements CustomPacketPayload {
    public static final Type<MyPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath("mymod", "my_payload"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MyPayload> CODEC =
        StreamCodec.composite(
            Identifier.STREAM_CODEC, MyPayload::id,
            ByteBufCodecs.VAR_INT,   MyPayload::value,
            ByteBufCodecs.BOOL,      MyPayload::flag,
            MyPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

### Grouping payloads

Multiple payloads can be grouped in a single container class as inner records:

```java
public class MyModPackets {
    public record SpellCast(Identifier spellId) implements CustomPacketPayload { /* ... */ }
    public record PermissionSync(/* ... */) implements CustomPacketPayload { /* ... */ }
}
```

---

## 2. StreamCodec

`StreamCodec<B, V>` is the serialization system for network payloads. It reads from and writes to a `RegistryFriendlyByteBuf`.

### Key factory methods

| Method | Usage |
|--------|-------|
| `StreamCodec.composite(codec1, getter1, ..., constructor)` | Record-style codec, up to 6 fields |
| `StreamCodec.unit(instance)` | Stateless payload (no data) |
| `StreamCodec.of(encoder, decoder)` | Manual encode/decode lambdas |
| `codec.map(decode, encode)` | Transform an existing codec |

### Built-in codecs (`ByteBufCodecs`)

| Codec | Type |
|-------|------|
| `ByteBufCodecs.BOOL` | `boolean` |
| `ByteBufCodecs.BYTE` | `byte` |
| `ByteBufCodecs.SHORT` | `short` |
| `ByteBufCodecs.INT` | `int` |
| `ByteBufCodecs.VAR_INT` | `int` (variable-length) |
| `ByteBufCodecs.LONG` | `long` |
| `ByteBufCodecs.VAR_LONG` | `long` (variable-length) |
| `ByteBufCodecs.FLOAT` | `float` |
| `ByteBufCodecs.DOUBLE` | `double` |
| `ByteBufCodecs.STRING_UTF8` | `String` |
| `ByteBufCodecs.BYTE_ARRAY` | `byte[]` |
| `Identifier.STREAM_CODEC` | `Identifier` (ResourceLocation) |

### Collections

```java
// List<Identifier>
ByteBufCodecs.collection(ArrayList::new, Identifier.STREAM_CODEC)

// Set<Identifier> (manual approach)
StreamCodec.of(
    (buf, set) -> {
        buf.writeVarInt(set.size());
        set.forEach(id -> Identifier.STREAM_CODEC.encode(buf, id));
    },
    buf -> {
        int size = buf.readVarInt();
        Set<Identifier> set = new HashSet<>(size);
        for (int i = 0; i < size; i++) set.add(Identifier.STREAM_CODEC.decode(buf));
        return set;
    }
)
```

### Enums

```java
// For StringRepresentable enums
StreamCodec.of(
    (buf, role) -> ByteBufCodecs.STRING_UTF8.encode(buf, role.getSerializedName()),
    buf -> StudioRole.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf).toUpperCase())
)
```

---

## 3. Registration

Registration happens during mod initialization. Two steps per payload:
1. Register the type + codec in `PayloadTypeRegistry`
2. Register a handler in `ServerPlayNetworking` or `ClientPlayNetworking`

### Direction registries

| Registry | Direction | Usage |
|----------|-----------|-------|
| `PayloadTypeRegistry.playC2S()` | Client → Server | Client sends, server receives |
| `PayloadTypeRegistry.playS2C()` | Server → Client | Server sends, client receives |

### Server-side (in `ModInitializer.onInitialize()`)

```java
// Register C→S payloads (client sends to server)
PayloadTypeRegistry.playC2S().register(SpellCast.TYPE, SpellCast.CODEC);
ServerPlayNetworking.registerGlobalReceiver(SpellCast.TYPE, SpellCastHandler::handle);

// Register S→C payloads (server sends to client) — type only, no handler here
PayloadTypeRegistry.playS2C().register(PermissionSync.TYPE, PermissionSync.CODEC);
```

### Client-side (in `ClientModInitializer.onInitializeClient()`)

```java
// Register handlers for S→C payloads (server sends to client)
ClientPlayNetworking.registerGlobalReceiver(PermissionSync.TYPE, (payload, context) -> {
    context.client().execute(() -> {
        // Handle on main client thread
    });
});
```

### Recommended structure

```
src/main/java/.../network/
    MyModPackets.java       // All payload record definitions
    NetworkHandler.java     // Registration (both C2S types + S2C types)
    impl/
        SpellCastHandler.java   // Server-side handler for C→S packet

src/client/java/.../network/
    ClientNetworkHandler.java   // Client-side handlers for S→C packets
```

---

## 4. Handlers

### Server handler signature

```java
public static void handle(SpellCast packet, ServerPlayNetworking.Context context) {
    context.server().execute(() -> {
        ServerPlayer player = context.player();
        // Game logic here — runs on main server thread
    });
}
```

`ServerPlayNetworking.Context` provides:
- `server()` — `MinecraftServer` instance (use `.execute()` for main thread)
- `player()` — `ServerPlayer` who sent the packet

### Client handler signature

```java
ClientPlayNetworking.registerGlobalReceiver(MyPayload.TYPE, (payload, context) -> {
    context.client().execute(() -> {
        // Game logic here — runs on main client thread
        Minecraft mc = Minecraft.getInstance();
    });
});
```

`ClientPlayNetworking.Context` provides:
- `client()` — `Minecraft` instance (use `.execute()` for main thread)

### Thread safety

Handlers are called on the **network thread**. Always wrap game logic in `context.server().execute()` or `context.client().execute()` to run on the main thread.

---

## 5. Sending Packets

### Server → Client

```java
// Send to a specific player
ServerPlayNetworking.send(serverPlayer, new PermissionSync(permissions));

// Send to all players
for (ServerPlayer player : server.getPlayerList().getPlayers()) {
    ServerPlayNetworking.send(player, new PermissionSync(perms));
}
```

### Client → Server

```java
ClientPlayNetworking.send(new SpellCast(spellId));
```

---

## 6. Lifecycle Events (for sending on join/disconnect)

```java
// In ModInitializer.onInitialize()

// Player joins — send them their data
ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
    ServerPlayer player = handler.getPlayer();
    ServerPlayNetworking.send(player, new PermissionSync(getPerms(player)));
});

// Server starts — load data
ServerLifecycleEvents.SERVER_STARTED.register(server -> {
    // Load permissions, etc.
});

// Server stops — save data
ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
    // Save permissions, etc.
});
```

---

## 7. Payload Size Limits

| Direction | Max size |
|-----------|----------|
| Server → Client (play) | 1,048,576 bytes (1 MB) |
| Client → Server (play) | 32,767 bytes (32 KB) |

---

## 8. Complete Example

A minimal mod that syncs a value from server to client on join.

**Payload:**
```java
public record SyncData(int level) implements CustomPacketPayload {
    public static final Type<SyncData> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath("mymod", "sync_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncData> CODEC =
        StreamCodec.composite(ByteBufCodecs.VAR_INT, SyncData::level, SyncData::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}
```

**Server (main):**
```java
public class MyMod implements ModInitializer {
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(SyncData.TYPE, SyncData.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayNetworking.send(handler.getPlayer(), new SyncData(42));
        });
    }
}
```

**Client:**
```java
public class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SyncData.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                System.out.println("Received level: " + payload.level());
            });
        });
    }
}
```

---

## Imports Reference

```java
// Payload definition
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Registration & sending (server)
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

// Registration & sending (client)
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

// Lifecycle events
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

// Player
import net.minecraft.server.level.ServerPlayer;
```
