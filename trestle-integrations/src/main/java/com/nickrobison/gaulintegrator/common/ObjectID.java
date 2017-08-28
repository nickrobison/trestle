package com.nickrobison.gaulintegrator.common;

import org.checkerframework.checker.nullness.qual.Nullable;

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
     * @param id - Previously generated id (in string form)
     * @param version - Type of ID, {@link IDVersion}
     */
    public ObjectID(String id, int version) {
        @Nullable IDVersion idVersion = idFromInt(version);
        if (idVersion == null) {
            idVersion = IDVersion.SIMPLE;
        }
        this.data = storeData(UUID.fromString(id), idVersion);
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
            default: version = IDVersion.SIMPLE;
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObjectID objectID = (ObjectID) o;

        return Arrays.equals(data, objectID.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
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
        if (version.equals(IDVersion.SIMPLE)) {
            value = 1;
        } else if (version.equals(IDVersion.HIERARCHICAL)) {
            value = 2;
        }
//        Now write the Version number
        bb.put(value);
        return bb.array();
    }

    private static @Nullable IDVersion idFromInt(int id) {
        switch (id) {
            case 1: return IDVersion.SIMPLE;
            case 2: return IDVersion.HIERARCHICAL;
            default: return null;
        }
    }
}
