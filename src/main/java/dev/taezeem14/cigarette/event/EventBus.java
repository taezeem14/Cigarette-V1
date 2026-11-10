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

    private final Map<Class<? extends Event>, List<RegisteredListener>> listenerMap = new ConcurrentHashMap<>();

    public void register(Object subscriber) {
        if (subscriber == null) return;
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        for (Method method : subscriber.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(EventTarget.class)) continue;
            if (method.getParameterCount() != 1) continue;

            Class<?> paramType = method.getParameterTypes()[0];
            if (!Event.class.isAssignableFrom(paramType)) continue;

            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) paramType;
            method.setAccessible(true);

            try {
                MethodHandle handle = lookup.unreflect(method);
                EventTarget target = method.getAnnotation(EventTarget.class);
                RegisteredListener listener = new RegisteredListener(subscriber, handle, target.priority());

                listenerMap.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>()).add(listener);
                // Sort by priority ascending (lower value = higher priority)
                listenerMap.get(eventClass).sort(Comparator.comparingInt(RegisteredListener::getPriority));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void unregister(Object subscriber) {
        if (subscriber == null) return;
        for (List<RegisteredListener> listeners : listenerMap.values()) {
            listeners.removeIf(listener -> listener.getSubscriber() == subscriber);
        }
    }

    public void clear() {
        listenerMap.clear();
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> T post(T event) {
        if (event == null) return null;

        List<RegisteredListener> listeners = listenerMap.get(event.getClass());
        if (listeners != null && !listeners.isEmpty()) {
            for (RegisteredListener listener : listeners) {
                try {
                    listener.getMethodHandle().invoke(listener.getSubscriber(), event);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
        return event;
    }

    private static final class RegisteredListener {
        private final Object subscriber;
        private final MethodHandle methodHandle;
        private final byte priority;

        public RegisteredListener(Object subscriber, MethodHandle methodHandle, byte priority) {
            this.subscriber = subscriber;
            this.methodHandle = methodHandle;
            this.priority = priority;
        }

        public Object getSubscriber() {
            return subscriber;
        }

        public MethodHandle getMethodHandle() {
            return methodHandle;
        }

        public byte getPriority() {
            return priority;
        }
    }
}
