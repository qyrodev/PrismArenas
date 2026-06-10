package me.qyro.prismarenas.storage.provider;

import me.qyro.prismarenas.arena.Arena;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface ArenaStorage {

    void initialize() throws StorageException;

    void shutdown();

    Map<String, Arena> loadAll() throws StorageException;

    void save(Arena arena) throws StorageException;

    void saveAll(Collection<Arena> arenas) throws StorageException;

    void delete(String arenaName) throws StorageException;

    CompletableFuture<Void> saveAsync(Arena arena);

    CompletableFuture<Void> saveAllAsync(Collection<Arena> arenas);

    CompletableFuture<Void> flushAsync();
}
