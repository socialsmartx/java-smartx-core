/**
 Copyright (c) 2017-2018 The Smartx Developers
 <p>
 Distributed under the MIT software license, see the accompanying file
 LICENSE or https://opensource.org/licenses/mit-license.php
 */
package com.smartx.db;

import static org.fusesource.leveldbjni.JniDBFactory.factory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;
import org.iq80.leveldb.*;

import com.smartx.util.ClosableIterator;
import com.smartx.util.FileUtil;
import com.smartx.util.SystemUtil;

public class LeveldbDatabase implements Database {
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(LeveldbDatabase.class);
    private final File file;
    private DB db;
    private boolean isOpened;
    /**
     * Creates an LevelDB instance and opens it.
     *
     * @param file
     */
    public LeveldbDatabase(File file) {
        this.file = file;
        File dir = file.getParentFile();
        if (!dir.exists() && !dir.mkdirs()) {
            logger.error("Failed to create directory: {}" + dir);
        }
        open(createOptions());
    }
    /**
     * Creates the default options.
     *
     * @return
     */
    protected Options createOptions() {
        Options options = new Options();
        options.createIfMissing(true);
        options.compressionType(CompressionType.NONE);
        options.blockSize(4 * 1024 * 1024);
        options.writeBufferSize(8 * 1024 * 1024);
        options.cacheSize(64L * 1024L * 1024L);
        options.paranoidChecks(true);
        options.verifyChecksums(true);
        options.maxOpenFiles(128);
        return options;
    }
    /**
     * Open the database.
     *
     * @param options
     */
    protected void open(Options options) {
        try {
            db = factory.open(file, options);
            isOpened = true;
        } catch (IOException e) {
            if (e.getMessage().contains("Corruption")) {
                // recover
                recover(options);
                // reopen
                try {
                    db = factory.open(file, options);
                    isOpened = true;
                } catch (IOException ex) {
                    logger.error("Failed to open database", e);
                    SystemUtil.exitAsync(SystemUtil.Code.FAILED_TO_OPEN_DB);
                }
            } else {
                logger.error("Failed to open database", e);
                SystemUtil.exitAsync(SystemUtil.Code.FAILED_TO_OPEN_DB);
            }
        }
    }
    /**
     * Tries to recover the database in case of corruption.
     *
     * @param options
     */
    protected void recover(Options options) {
        try {
            logger.info("Trying to repair the database: {}" + file.getName());
            factory.repair(file, options);
            logger.info("Repair done!");
        } catch (IOException ex) {
            logger.error("Failed to repair the database", ex);
            SystemUtil.exitAsync(SystemUtil.Code.FAILED_TO_REPAIR_DB);
        }
    }
    @Override
    public byte[] get(byte[] key) {
        return db.get(key);
    }
    @Override
    public void put(byte[] key, byte[] value) {
        db.put(key, value);
    }
    @Override
    public void delete(byte[] key) {
        db.delete(key);
    }
    @Override
    public void updateBatch(List<Pair<byte[], byte[]>> pairs) {
        try (WriteBatch batch = db.createWriteBatch()) {
            for (Pair<byte[], byte[]> p : pairs) {
                if (p.getValue() == null) {
                    batch.delete(p.getLeft());
                } else {
                    batch.put(p.getLeft(), p.getRight());
                }
            }
            db.write(batch);
        } catch (IOException e) {
            logger.error("Failed to update batch", e);
            SystemUtil.exitAsync(SystemUtil.Code.FAILED_TO_WRITE_BATCH_TO_DB);
        }
    }
    @Override
    public void close() {
        try {
            if (isOpened) {
                db.close();
                isOpened = false;
            }
        } catch (IOException e) {
            logger.error("Failed to close database: {}" + file, e);
        }
    }
    @Override
    public void destroy() {
        close();
        FileUtil.recursiveDelete(file);
    }
    @Override
    public Path getDataDir() {
        return file.toPath();
    }
    @Override
    public ClosableIterator<Entry<byte[], byte[]>> iterator() {
        return iterator(null);
    }
    @Override
    public ClosableIterator<Entry<byte[], byte[]>> iterator(byte[] prefix) {
        return new ClosableIterator<Entry<byte[], byte[]>>() {
            final DBIterator itr = db.iterator();
            private ClosableIterator<Entry<byte[], byte[]>> initialize() {
                if (prefix != null) {
                    itr.seek(prefix);
                } else {
                    itr.seekToFirst();
                }
                return this;
            }
            @Override
            public boolean hasNext() {
                return itr.hasNext();
            }
            @Override
            public Entry<byte[], byte[]> next() {
                return itr.next();
            }
            @Override
            public void close() {
                try {
                    itr.close();
                } catch (IOException e) {
                    throw new DatabaseException(e);
                }
            }
        }.initialize();
    }
    public static class LeveldbFactory implements DatabaseFactory {
        private final EnumMap<DatabaseName, Database> databases = new EnumMap<>(DatabaseName.class);
        private final File dataDir;
        public LeveldbFactory(File dataDir) {
            this.dataDir = dataDir;
        }
        @Override
        public Database getDB(DatabaseName name) {
            return databases.computeIfAbsent(name, k -> {
                File file = new File(dataDir.getAbsolutePath(), k.toString().toLowerCase(Locale.ROOT));
                return new LeveldbDatabase(file);
            });
        }
        @Override
        public void close() {
            for (Database db : databases.values()) {
                db.close();
            }
            databases.clear();
        }
        @Override
        public Path getDataDir() {
            return dataDir.toPath();
        }
    }
}
