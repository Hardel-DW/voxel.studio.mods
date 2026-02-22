package fr.hardel.asset_editor.client.javafx.components.ui.tree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class TreeNodeModel {

    private final LinkedHashMap<String, TreeNodeModel> children = new LinkedHashMap<>();
    private final ArrayList<String> identifiers = new ArrayList<>();
    private int count;
    private String elementId;
    private String iconPath;
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

    public String iconPath() {
        return iconPath;
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

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }

    public boolean hasActiveDescendant(String activeId) {
        if (activeId == null || activeId.isBlank()) return false;
        if (activeId.equals(elementId)) return true;
        for (TreeNodeModel child : children.values()) {
            if (child.hasActiveDescendant(activeId)) return true;
        }
        return false;
    }

    public int recalculateCount() {
        if (elementId != null && !elementId.isBlank()) return 1;
        int value = 0;
        for (TreeNodeModel child : children.values()) {
            value += child.recalculateCount();
        }
        count = value;
        return value;
    }
}
