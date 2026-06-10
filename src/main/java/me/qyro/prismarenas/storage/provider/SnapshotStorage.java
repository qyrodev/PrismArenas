package me.qyro.prismarenas.storage.provider;

import me.qyro.prismarenas.snapshot.Snapshot;

import java.util.concurrent.CompletableFuture;

public interface SnapshotStorage {

    void initialize() throws StorageException;

    void shutdown();

    Snapshot load(String arenaName) throws StorageException;

    void save(String arenaName, Snapshot snapshot) throws StorageException;

    void delete(String arenaName) throws StorageException;

    boolean exists(String arenaName);

    CompletableFuture<Void> saveAsync(String arenaName, Snapshot snapshot);

    CompletableFuture<Void> deleteAsync(String arenaName);
}