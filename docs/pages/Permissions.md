# Permissions
The permission system is independent from JavaFX, concepts, registries, and Minecraft OP.
It defines who can open the editor, who can edit, and who can manage other users.

## Roles
There are three roles:
- **Admin**: full access. Can promote or demote other players with `/studio role set`.
- **Contributor**: can edit all available concepts and registries, but cannot manage permissions.
- **None**: cannot open Voxel Studio. The F8 key is blocked with a chat message.

## Storage
Permissions are stored per player UUID in world at <world>/asset_editor_permissions.json
Minecraft OP has no effect on Studio permissions.

## Singleplayer Rules
In singleplayer, the host player is automatically treated as **Admin**, this is based on the player UUID matching `server.getSingleplayerProfile().id()`.
LAN guests start as **None** until the host promotes them.

## Commands
Commands require an **Admin** player or the server console.
- /studio info <player>
- /studio role <player> set <admin|contributor|none>

## Client Sync
Permissions are pushed by the server to the client on join and when they change, the client only stores the received permission state for UI and access control. The server remains the source of truth.
