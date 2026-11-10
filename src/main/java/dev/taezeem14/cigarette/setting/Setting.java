package dev.taezeem14.cigarette.setting;

import java.util.function.Supplier;

public abstract class Setting<T> {

    protected final String name;
    protected T value;
    protected Supplier<Boolean> visibility = () -> true;

    public Setting(String name, T defaultValue) {
        this.name = name;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public boolean isVisible() {
        return visibility.get();
    }

    public Setting<T> setVisibility(Supplier<Boolean> visibility) {
        this.visibility = visibility;
        return this;
    }
}
