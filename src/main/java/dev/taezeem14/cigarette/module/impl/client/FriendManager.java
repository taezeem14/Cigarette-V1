package dev.taezeem14.cigarette.module.impl.client;

import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class FriendManager extends Module {

    private static final Set<String> friends = new CopyOnWriteArraySet<>();

    public FriendManager() {
        super("Friends", "Manage and prevent targeting friends", Category.CLIENT);
    }

    public static boolean isFriend(String name) {
        if (name == null) return false;
        return friends.contains(name.toLowerCase());
    }

    public static void addFriend(String name) {
        if (name != null) {
            friends.add(name.toLowerCase());
        }
    }

    public static void removeFriend(String name) {
        if (name != null) {
            friends.remove(name.toLowerCase());
        }
    }

    public static void clearFriends() {
        friends.clear();
    }

    public static Set<String> getFriends() {
        return Collections.unmodifiableSet(friends);
    }
}
