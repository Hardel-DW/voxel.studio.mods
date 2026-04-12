# Project Overview
Fabric 1.21.11 "Voxel Studio" mod in Java 21.
ALWAYS USE MOJMAP! Not Yarn. There is client and main folder.
The documentation in the docs\SUMMARY.md for TOC, It's important to always read this documentation, it contains the rules and important files you need to know about.

# Decompiled Code
Important: if you need knowledge, search in the directory: 
- "\<mod_name>\decompiled" this contains decompiled Minecraft source code.
- net (e.g asset_editor\decompiled\net\minecraft\SharedConstants.java)
- com
- studio (Original TSX code)
- packages Source voxel dependencies (voxelio e.g breeze, diff).
- client (Old JavaFX code)

# Global Rules:
- Gradle is accessible with "gradle\wrapper\gradle-wrapper.jar" path.
- No redundancy.
- No function/variable with a single line/reference. Except Getter/Setter...
- Avoid over engineering.
- No support of Legacy/Deprecated
- A class should have a signle dominant responsibility.
- Avoid dirty code / temporary code.
- It's better to tell me what you have in mind before doing it.
- Must use Translation Key instead Literal.
- Don't just write code that fixes a problem immediately, think long term and consider all possible future scenarios.
- Don't lie, prefer to tell the truth even when it's negative, don't please me just to please me, we must work factually.
- Try to criticize my choices which can sometimes go in the wrong direction.
- Don't just create full static files all the time; it's useless, unreadable, and counterproductive.
- Prioritize the OOP approach. Don't make everything in a static class. Use a correct Pattern. (Static is good but not for everything)
- Avoid unchecked, UNCHECKED_CAST find good architectural solutions that avoid them as much as possible.
- We must avoid duplicating truth sources.

# Studio Project :
- Unlike Studio Web, we don't have to manage all versions of Minecraft, just the current e.g 1.21.11
- You can use Minecraft/Mixins registries and codecs and everything.
- All colors, breakpoints, fonts, defined at root.
- No text in components either, use string params, translation in routes.
- So just “trying to get closer” is not enough; you have to do it exactly the same. If there are any issues, let me know, such as CSS or Tailwind styles that cannot be reproduced.
- We work by component; each Kotlin File constitutes a component.
- SVGs/Asset should not be rewritten. Copy from tsx.
- Avoid adding too many unnecessary comments that serve no purpose.
- AssetEditorClient and AssetEditor must contains "register" methods.
- AssetEditor Contains MOD_ID, use it instead of hardcoding string.