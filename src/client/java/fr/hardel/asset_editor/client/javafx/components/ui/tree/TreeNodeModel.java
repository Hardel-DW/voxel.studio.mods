package fr.hardel.asset_editor.client.javafx.components.ui.tree;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class TreeNodeModel {

    private final LinkedHashMap<String, TreeNodeModel> children = new LinkedHashMap<>();
    private final ArrayList<String> identifiers = new ArrayList<>();
    private int count;
    private String elementId;
    private Identifier icon;
    private boolean folder;

    public LinkedHashMap<String, TreeNodeModel> children() {
        return children;
    }

    public List<String> identifiers() {
        return identifiers;
    }

    public int count() {
        return count;
    }

    public String elementId() {
        return elementId;
    }

    public Identifier icon() {
        return icon;
    }

    public boolean folder() {
        return folder;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    public void setIcon(Identifier icon) {
        this.icon = icon;
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }

}
