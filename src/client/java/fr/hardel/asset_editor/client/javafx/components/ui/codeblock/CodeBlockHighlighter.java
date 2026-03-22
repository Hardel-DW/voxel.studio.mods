package fr.hardel.asset_editor.client.javafx.components.ui.codeblock;

import fr.hardel.asset_editor.client.highlight.HighlightRegistry;

@FunctionalInterface
public interface CodeBlockHighlighter {

    void apply(String text, HighlightRegistry registry);
}
