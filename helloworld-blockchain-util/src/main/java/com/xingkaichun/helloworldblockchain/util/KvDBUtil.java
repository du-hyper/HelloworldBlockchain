package com.xingkaichun.helloworldblockchain.util;

import org.iq80.leveldb.*;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.iq80.leveldb.impl.WriteBatchImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LevelDB工具类
 *
 * @author 邢开春 409060350@qq.com
 */
public class KvDBUtil {

    private static final Logger logger = LoggerFactory.getLogger(KvDBUtil.class);

    private static WriteOptions writeOptions = new WriteOptions();
    private static Map<String,DB> dbMap = new HashMap<>();

    static {
        writeOptions.sync(true);
        writeOptions.snapshot(true);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (DB db:dbMap.values()){
                try {
                    db.close();
                } catch (Exception e) {
                    logger.error("LevelDB database close failed.",e);
                }
            }
        }));
    }

    private static DB getDB(String dbPath) {
        synchronized (KvDBUtil.class){
            DB db = dbMap.get(dbPath);
            if(db == null){
                try {
                    DBFactory factory = new Iq80DBFactory();
                    Options options = new Options();
                    db = factory.open(new File(dbPath), options);
                } catch (IOException e) {
                    logger.error(String.format("create or load LevelDB database failed. LevelDB database file path is %s.",dbPath),e);
                    throw new RuntimeException(e);
                }
                dbMap.put(dbPath,db);
            }
            return db;
        }
    }

    public static void put(String dbPath, byte[] bytesKey, byte[] bytesValue) {
        DB db = getDB(dbPath);
        db.put(bytesKey,bytesValue);
    }
    public static void write(String dbPath, KvWriteBatch kvWriteBatch) {
        DB db = getDB(dbPath);
        WriteBatch levelDBwriteBatch = levelDbWriteBatch(kvWriteBatch);
        db.write(levelDBwriteBatch);
    }
    public static void delete(String dbPath, byte[] bytesKey) {
        DB db = getDB(dbPath);
        db.delete(bytesKey);
    }
    public static byte[] get(String dbPath, byte[] bytesKey) {
        DB db = getDB(dbPath);
        return db.get(bytesKey);
    }
    public static List<byte[]> get(String dbPath, long from, long size) {
        synchronized (KvDBUtil.class){
            List<byte[]> valueList = new ArrayList<>();
            int cunrrentFrom = 0;
            int cunrrentSize = 0;
            DB db = getDB(dbPath);
            for (DBIterator iterator = db.iterator(); iterator.hasNext(); iterator.next()) {
                byte[] byteValue = iterator.peekNext().getValue();
                if(byteValue == null || byteValue.length==0){
                    //注意，用levelDB这里确是continue
                   continue;
                }
                cunrrentFrom++;
                if(cunrrentFrom>=from && cunrrentSize<size){
                    valueList.add(byteValue);
                    cunrrentSize++;
                }
                if(cunrrentSize>=size){
                    break;
                }
            }
            return valueList;
        }
    }

    private static WriteBatch levelDbWriteBatch(KvWriteBatch kvWriteBatch) {
        WriteBatch writeBatch = new WriteBatchImpl();
        if(kvWriteBatch != null){
            for (KvWrite kvWrite : kvWriteBatch.getKvWriteList()){
                if(kvWrite.kvWriteActionEnum == KvWriteActionEnum.ADD){
                    writeBatch.put(kvWrite.key, kvWrite.value);
                }else if(kvWrite.kvWriteActionEnum == KvWriteActionEnum.DELETE){
                    writeBatch.delete(kvWrite.key);
                }else {
                    throw new RuntimeException();
                }
            }
        }
        return writeBatch;
    }



    public static class KvWriteBatch {
        private List<KvWrite> kvWriteList;
        public KvWriteBatch() {
            this.kvWriteList = new ArrayList<>();
        }
        public List<KvWrite> getKvWriteList() {
            return kvWriteList;
        }
        public void setKvWriteList(List<KvWrite> kvWriteList) {
            this.kvWriteList = kvWriteList;
        }
        public void put(byte[] key, byte[] value) {
            kvWriteList.add(new KvWrite(KvWriteActionEnum.ADD,key,value));
        }
        public void delete(byte[] key) {
            kvWriteList.add(new KvWrite(KvWriteActionEnum.DELETE,key,null));
        }
    }
    public static class KvWrite {
        private KvWriteActionEnum kvWriteActionEnum;
        private byte[] key;
        private byte[] value;
        public KvWrite(KvWriteActionEnum kvWriteActionEnum, byte[] key, byte[] value) {
            this.kvWriteActionEnum = kvWriteActionEnum;
            this.key = key;
            this.value = value;
        }
        public KvWriteActionEnum getKvWriteActionEnum() {
            return kvWriteActionEnum;
        }
        public byte[] getKey() {
            return key;
        }
        public byte[] getValue() {
            return value;
        }
    }
    public enum KvWriteActionEnum {
        ADD,DELETE
    }
}
