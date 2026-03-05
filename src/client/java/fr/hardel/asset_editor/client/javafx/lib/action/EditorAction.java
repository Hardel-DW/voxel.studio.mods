package fr.hardel.asset_editor.client.javafx.lib.action;

import net.minecraft.resources.Identifier;

import java.util.function.UnaryOperator;

public record EditorAction<T>(String registry, Identifier target, Class<T> type, UnaryOperator<T> transform) {}
