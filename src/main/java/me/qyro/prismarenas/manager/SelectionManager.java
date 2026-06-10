package me.qyro.prismarenas.manager;

import org.bukkit.Location;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SelectionManager {

    public static final class Selection {
        private Location pos1;
        private Location pos2;

        public Location getPos1() {
            return pos1;
        }

        public void setPos1(Location pos1) {
            this.pos1 = pos1;
        }

        public Location getPos2() {
            return pos2;
        }

        public void setPos2(Location pos2) {
            this.pos2 = pos2;
        }

        public boolean isComplete() {
            return pos1 != null && pos2 != null
                    && pos1.getWorld() != null && pos2.getWorld() != null
                    && pos1.getWorld().equals(pos2.getWorld());
        }
    }

    private final Map<UUID, Selection> selections = new ConcurrentHashMap<>();

    public Selection getSelection(UUID playerId) {
        return selections.computeIfAbsent(playerId, id -> new Selection());
    }

    public void clearSelection(UUID playerId) {
        selections.remove(playerId);
    }
}
