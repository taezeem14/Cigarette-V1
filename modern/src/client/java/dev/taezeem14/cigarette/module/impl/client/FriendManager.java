package dev.taezeem14.cigarette.module.impl.client;

import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FriendManager extends Module {

    private static final Set<String> FRIENDS = ConcurrentHashMap.newKeySet();

    public FriendManager() {
        super("Friends", "Prevents targeting or attacking players in your friends list", Category.CLIENT);
        setState(true);
    }

    public static void addFriend(String name) {
        if (name != null && !name.isEmpty()) {
            FRIENDS.add(name.toLowerCase());
        }
    }

    public static void removeFriend(String name) {
        if (name != null) {
            FRIENDS.remove(name.toLowerCase());
        }
    }

    public static boolean isFriend(String name) {
        return name != null && FRIENDS.contains(name.toLowerCase());
    }

    public static Set<String> getFriends() {
        return Collections.unmodifiableSet(FRIENDS);
    }
}
