package com.nickrobison.gaulintegrator.common;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

/**
 * Unique ID for a given spatial object,
 * Created by nrobison on 5/6/16.
 */
public class ObjectID {

    public enum IDVersion {
        SIMPLE,
        HIERARCHICAL
    }
    //        UUID first - Version second
    private final byte[] data;


    /**
     * Default Constructor: Generates a simple UUID.
     */
    public ObjectID() {
        UUID u = UUID.randomUUID();
        data = storeData(u, IDVersion.SIMPLE);
    }

    /**
     *
     * @param id Previously generated UUID to use as base
     * @param version Type of ID, {@link IDVersion}
     */
    public ObjectID(UUID id, IDVersion version) {
        this.data = storeData(id, version);
    }

    /**
     *
     * @return the IDVersion, {@link IDVersion}
     */
    public IDVersion getVersion() {
//        return version;
        ByteBuffer bb = ByteBuffer.wrap(data);
//        Get the last byte of the array
        int versionInt = bb.get(16);
        IDVersion version = null;
        switch (versionInt) {
            case 1: version = IDVersion.SIMPLE;
                break;
            case 2: version= IDVersion.HIERARCHICAL;
                break;
        }
        return version;
    }

    /**
     *
     * @return the ID of the Object
     */
    public UUID getID() {
//        return bytesToUUID(idBytes);
        return readUUID(this.data);
    }

    @Override
    public String toString() {
        return readUUID(this.data).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ObjectID) {
            ObjectID other = (ObjectID) o;
            return Arrays.equals(data, other.data);
        }
        return false;
    }

    private static byte[] uuidToBytes(UUID id) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(id.getMostSignificantBits());
        bb.putLong(id.getLeastSignificantBits());
        return bb.array();
    }

    private static UUID byteBufferToUUID(ByteBuffer bb) {
        long firstLong = bb.getLong();
        long secondLong = bb.getLong();
        return new UUID(firstLong, secondLong);
    }

    private static UUID readUUID(byte[] inputBytes) {
        ByteBuffer bb  = ByteBuffer.wrap(inputBytes);
        return byteBufferToUUID(bb);
    }

    private static byte[] storeData(UUID id, IDVersion version) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[17]);
//        Write UUID first
        bb.put(uuidToBytes(id));
        byte value = 0;
        switch (version) {
            case SIMPLE: value = 1;
                break;
            case HIERARCHICAL: value = 2;
                break;
        }
//        Now write the Version number
        bb.put(value);
        return bb.array();
    }
}
