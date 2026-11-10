package dev.taezeem14.cigarette.event;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {

    private final Map<Class<? extends Event>, List<ListenerData>> registry = new ConcurrentHashMap<>();
    private final MethodHandles.Lookup lookup = MethodHandles.lookup();

    public void register(Object listener) {
        if (listener == null) return;

        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(EventTarget.class)) continue;
            if (method.getParameterCount() != 1) continue;

            Class<?> paramType = method.getParameterTypes()[0];
            if (!Event.class.isAssignableFrom(paramType)) continue;

            @SuppressWarnings("unchecked")
            Class<? extends Event> eventType = (Class<? extends Event>) paramType;
            EventTarget annotation = method.getAnnotation(EventTarget.class);

            try {
                method.setAccessible(true);
                MethodHandle handle = lookup.unreflect(method).bindTo(listener);
                List<ListenerData> list = registry.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>());

                for (ListenerData existing : list) {
                    if (existing.listener == listener && existing.method.equals(method)) {
                        return;
                    }
                }

                list.add(new ListenerData(listener, method, handle, annotation.priority()));
                list.sort(Comparator.comparingInt(d -> d.priority.getLevel()));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void unregister(Object listener) {
        if (listener == null) return;

        for (List<ListenerData> list : registry.values()) {
            list.removeIf(data -> data.listener == listener);
        }
    }

    public void clear() {
        registry.clear();
    }

    public <T extends Event> T post(T event) {
        if (event == null) return null;

        List<ListenerData> list = registry.get(event.getClass());
        if (list == null || list.isEmpty()) return event;

        for (ListenerData data : list) {
            try {
                data.handle.invoke(event);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return event;
    }

    private static final class ListenerData {
        final Object listener;
        final Method method;
        final MethodHandle handle;
        final EventPriority priority;

        ListenerData(Object listener, Method method, MethodHandle handle, EventPriority priority) {
            this.listener = listener;
            this.method = method;
            this.handle = handle;
            this.priority = priority;
        }
    }
}
