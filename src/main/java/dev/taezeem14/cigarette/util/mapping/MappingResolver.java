package dev.taezeem14.cigarette.util.mapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MappingResolver {

    private static final Map<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Field MODIFIERS_FIELD;

    static {
        Field modField = null;
        try {
            modField = Field.class.getDeclaredField("modifiers");
            modField.setAccessible(true);
        } catch (Exception ignored) {
        }
        MODIFIERS_FIELD = modField;
    }

    private MappingResolver() {
    }

    private static Field findField(Class<?> clazz, String... names) {
        if (clazz == null || names == null || names.length == 0) {
            return null;
        }
        String cacheKey = clazz.getName() + "#" + Arrays.toString(names);
        Field cached = FIELD_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (String name : names) {
                try {
                    Field field = current.getDeclaredField(name);
                    field.setAccessible(true);
                    FIELD_CACHE.put(cacheKey, field);
                    return field;
                } catch (NoSuchFieldException ignored) {
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    public static Field getField(Class<?> clazz, String... names) {
        Field field = findField(clazz, names);
        if (field == null) {
            throw new RuntimeException("Could not resolve field from names: " + Arrays.toString(names) + " on class " + (clazz != null ? clazz.getName() : "null"));
        }
        return field;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Object instance, Class<?> clazz, String... names) {
        try {
            Field field = findField(clazz, names);
            if (field == null) {
                return null;
            }
            return (T) field.get(instance);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void setFieldValue(Object instance, Object value, Class<?> clazz, String... names) {
        try {
            Field field = findField(clazz, names);
            if (field == null) {
                return;
            }
            if (Modifier.isFinal(field.getModifiers()) && MODIFIERS_FIELD != null) {
                MODIFIERS_FIELD.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            }
            field.set(instance, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Method getMethod(Class<?> clazz, Class<?>[] parameterTypes, String... names) {
        String cacheKey = clazz.getName() + "#" + Arrays.toString(names) + "(" + Arrays.toString(parameterTypes) + ")";
        Method cached = METHOD_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (String name : names) {
                try {
                    Method method = current.getDeclaredMethod(name, parameterTypes);
                    method.setAccessible(true);
                    METHOD_CACHE.put(cacheKey, method);
                    return method;
                } catch (NoSuchMethodException ignored) {
                }
            }
            current = current.getSuperclass();
        }

        throw new RuntimeException("Could not resolve method from names: " + Arrays.toString(names) + " on class " + clazz.getName());
    }

    @SuppressWarnings("unchecked")
    public static <T> T invokeMethod(Object instance, Class<?> clazz, Class<?>[] parameterTypes, Object[] args, String... names) {
        try {
            Method method = getMethod(clazz, parameterTypes, names);
            return (T) method.invoke(instance, args);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Common MCP <-> Searge mapping definitions
    public static final String[] MINECRAFT_RIGHT_CLICK_DELAY_TIMER = {"rightClickDelayTimer", "field_71467_ac"};
    public static final String[] MINECRAFT_LEFT_CLICK_COUNTER = {"leftClickCounter", "field_71429_W"};
    public static final String[] MINECRAFT_TIMER = {"timer", "field_71428_T"};
    public static final String[] TIMER_TIMER_SPEED = {"timerSpeed", "field_74278_d"};
    public static final String[] KEYBINDING_PRESSED = {"pressed", "field_74513_e"};
    public static final String[] KEYBINDING_PRESS_TIME = {"pressTime", "field_151474_i"};
    public static final String[] ENTITYPLAYER_ITEM_IN_USE_COUNT = {"itemInUseCount", "field_71072_f"};
    public static final String[] PLAYERCONTROLLERMP_CUR_BLOCK_DAMAGE = {"curBlockDamageMP", "field_78770_f"};
    public static final String[] PLAYERCONTROLLERMP_BLOCK_HIT_DELAY = {"blockHitDelay", "field_78781_i"};
    public static final String[] MINECRAFT_CLICK_MOUSE = {"clickMouse", "func_147116_af"};
    public static final String[] MINECRAFT_RIGHT_CLICK_MOUSE = {"rightClickMouse", "func_147121_ag"};
}
