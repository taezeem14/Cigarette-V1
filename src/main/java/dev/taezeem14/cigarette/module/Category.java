package dev.taezeem14.cigarette.module;

public enum Category {
    COMBAT("Combat"),
    BLATANT("Blatant"),
    RENDER("Render"),
    UTILITY("Utility"),
    WORLD("World"),
    CLIENT("Client");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
