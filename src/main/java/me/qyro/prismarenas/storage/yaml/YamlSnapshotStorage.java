package me.qyro.prismarenas.storage.yaml;

import me.qyro.prismarenas.PrismArenas;
import me.qyro.prismarenas.arena.ArenaBounds;
import me.qyro.prismarenas.manager.ConfigManager;
import me.qyro.prismarenas.snapshot.Snapshot;
import me.qyro.prismarenas.storage.provider.SnapshotStorage;
import me.qyro.prismarenas.storage.provider.StorageException;
import me.qyro.prismarenas.util.VarIntUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class YamlSnapshotStorage implements SnapshotStorage {

    public static final int MAGIC = 0x5052534D;
    public static final int VERSION_COMPRESSED = 2;
    public static final int VERSION_LEGACY = 1;

    private final PrismArenas plugin;
    private final ConfigManager configManager;
    private File snapshotsFolder;

    public YamlSnapshotStorage(PrismArenas plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public void initialize() throws StorageException {
        snapshotsFolder = new File(plugin.getDataFolder(), configManager.getSnapshotsDirectory());
        if (!snapshotsFolder.exists() && !snapshotsFolder.mkdirs()) {
            throw new StorageException("Failed to create snapshots folder: " + snapshotsFolder.getPath());
        }
        configManager.verboseStorage("Snapshot directory: " + snapshotsFolder.getPath());
    }

    @Override
    public void shutdown() {
    }

    @Override
    public Snapshot load(String arenaName) throws StorageException {
        File file = getSnapshotFile(arenaName);
        if (!file.exists()) {
            return null;
        }

        long start = System.currentTimeMillis();
        try (DataInputStream in = openInput(file)) {
            int magic = in.readInt();
            if (magic != MAGIC) {
                throw new StorageException("Invalid snapshot magic for arena: " + arenaName);
            }

            int version = in.readInt();
            Snapshot snapshot = switch (version) {
                case VERSION_COMPRESSED -> readCompressed(in);
                case VERSION_LEGACY -> readLegacy(in);
                default -> throw new StorageException("Unsupported snapshot version " + version
                        + " for arena: " + arenaName);
            };
            configManager.verboseStorage("Loaded snapshot '" + arenaName + "' ("
                    + snapshot.getBlockCount() + " blocks) in "
                    + (System.currentTimeMillis() - start) + "ms");
            return snapshot;
        } catch (EOFException e) {
            throw new StorageException("Corrupt snapshot file for arena: " + arenaName, e);
        } catch (IOException e) {
            throw new StorageException("Failed to load snapshot for arena: " + arenaName, e);
        }
    }

    @Override
    public void save(String arenaName, Snapshot snapshot) throws StorageException {
        File file = getSnapshotFile(arenaName);
        File temp = new File(snapshotsFolder, getSafeName(arenaName) + ".snapshot.tmp");

        long start = System.currentTimeMillis();
        try (DataOutputStream out = openOutput(temp)) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION_COMPRESSED);
            writeCompressed(out, snapshot);
        } catch (IOException e) {
            throw new StorageException("Failed to save snapshot for arena: " + arenaName, e);
        }

        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Failed to replace existing snapshot for arena: " + arenaName);
        }
        if (!temp.renameTo(file)) {
            throw new StorageException("Failed to finalize snapshot file for arena: " + arenaName);
        }
        configManager.verboseStorage("Saved snapshot '" + arenaName + "' ("
                + snapshot.getBlockCount() + " blocks) in "
                + (System.currentTimeMillis() - start) + "ms");
    }

    @Override
    public void delete(String arenaName) throws StorageException {
        File file = getSnapshotFile(arenaName);
        if (file.exists() && !file.delete()) {
            throw new StorageException("Failed to delete snapshot for arena: " + arenaName);
        }
    }

    @Override
    public boolean exists(String arenaName) {
        return getSnapshotFile(arenaName).exists();
    }

    @Override
    public CompletableFuture<Void> saveAsync(String arenaName, Snapshot snapshot) {
        if (configManager.isSnapshotAsyncSaving()) {
            return CompletableFuture.runAsync(() -> {
                try {
                    save(arenaName, snapshot);
                } catch (StorageException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to save snapshot asynchronously: " + arenaName, e);
                }
            });
        }
        try {
            save(arenaName, snapshot);
            return CompletableFuture.completedFuture(null);
        } catch (StorageException e) {
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    @Override
    public CompletableFuture<Void> deleteAsync(String arenaName) {
        return CompletableFuture.runAsync(() -> {
            try {
                delete(arenaName);
            } catch (StorageException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete snapshot asynchronously: " + arenaName, e);
            }
        });
    }

    private DataOutputStream openOutput(File file) throws IOException {
        OutputStream base = new BufferedOutputStream(new FileOutputStream(file));
        if (configManager.isSnapshotCompression()) {
            GZIPOutputStream gzip = new GZIPOutputStream(base) {
                {
                    def.setLevel(configManager.getSnapshotCompressionLevel());
                }
            };
            return new DataOutputStream(gzip);
        }
        return new DataOutputStream(base);
    }

    private DataInputStream openInput(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        fis.mark(2);
        int first = fis.read();
        int second = fis.read();
        fis.reset();

        if (first == 0x1F && second == (byte) 0x8B) {
            return new DataInputStream(new BufferedInputStream(new GZIPInputStream(fis)));
        }
        return new DataInputStream(new BufferedInputStream(fis));
    }

    private void writeCompressed(DataOutputStream out, Snapshot snapshot) throws IOException {
        ArenaBounds bounds = snapshot.getBounds();
        out.writeUTF(bounds.getWorldName());
        out.writeInt(bounds.getMinX());
        out.writeInt(bounds.getMinY());
        out.writeInt(bounds.getMinZ());
        out.writeInt(bounds.getMaxX());
        out.writeInt(bounds.getMaxY());
        out.writeInt(bounds.getMaxZ());

        String[] palette = snapshot.getPalette();
        VarIntUtil.write(out, palette.length);
        for (String entry : palette) {
            out.writeUTF(entry);
        }

        int blockCount = snapshot.getBlockCount();
        VarIntUtil.write(out, blockCount);

        int[] relX = snapshot.getRelativeX();
        int[] relY = snapshot.getRelativeY();
        int[] relZ = snapshot.getRelativeZ();
        int[] paletteIndex = snapshot.getPaletteIndex();

        for (int i = 0; i < blockCount; i++) {
            VarIntUtil.write(out, relX[i]);
            VarIntUtil.write(out, relY[i]);
            VarIntUtil.write(out, relZ[i]);
            VarIntUtil.write(out, paletteIndex[i]);
        }
    }

    private Snapshot readCompressed(DataInputStream in) throws IOException {
        String worldName = in.readUTF();
        int minX = in.readInt();
        int minY = in.readInt();
        int minZ = in.readInt();
        int maxX = in.readInt();
        int maxY = in.readInt();
        int maxZ = in.readInt();

        ArenaBounds bounds = new ArenaBounds(worldName, minX, minY, minZ, maxX, maxY, maxZ);

        int paletteSize = VarIntUtil.read(in);
        String[] palette = new String[paletteSize];
        for (int i = 0; i < paletteSize; i++) {
            palette[i] = in.readUTF();
        }

        int blockCount = VarIntUtil.read(in);
        int[] relX = new int[blockCount];
        int[] relY = new int[blockCount];
        int[] relZ = new int[blockCount];
        int[] paletteIndex = new int[blockCount];

        for (int i = 0; i < blockCount; i++) {
            relX[i] = VarIntUtil.read(in);
            relY[i] = VarIntUtil.read(in);
            relZ[i] = VarIntUtil.read(in);
            paletteIndex[i] = VarIntUtil.read(in);
        }

        return Snapshot.create(bounds, palette, relX, relY, relZ, paletteIndex);
    }

    private Snapshot readLegacy(DataInputStream in) throws IOException {
        String worldName = in.readUTF();
        int minX = in.readInt();
        int minY = in.readInt();
        int minZ = in.readInt();
        int maxX = in.readInt();
        int maxY = in.readInt();
        int maxZ = in.readInt();

        ArenaBounds bounds = new ArenaBounds(worldName, minX, minY, minZ, maxX, maxY, maxZ);

        int paletteSize = in.readInt();
        String[] palette = new String[paletteSize];
        for (int i = 0; i < paletteSize; i++) {
            palette[i] = in.readUTF();
        }

        int blockCount = in.readInt();
        int[] relX = new int[blockCount];
        int[] relY = new int[blockCount];
        int[] relZ = new int[blockCount];
        int[] paletteIndex = new int[blockCount];

        for (int i = 0; i < blockCount; i++) {
            relX[i] = in.readInt();
            relY[i] = in.readInt();
            relZ[i] = in.readInt();
            paletteIndex[i] = in.readInt();
        }

        return Snapshot.create(bounds, palette, relX, relY, relZ, paletteIndex);
    }

    private File getSnapshotFile(String arenaName) {
        return new File(snapshotsFolder, getSafeName(arenaName) + ".snapshot");
    }

    private String getSafeName(String arenaName) {
        return arenaName.toLowerCase().replaceAll("[^a-z0-9_-]", "_");
    }
}
