package com.nickrobison.gaulintegrator.common;

import java.util.UUID;

/**
 * Generates ObjectIDs for a given object. Right now, it's really simple, but eventually we'll do more error checking and advanced generation
 *
 *  @author nrobison
 *  @since 2016-05-06
 * Created by nrobison on 5/6/16.
 */
public class IDGenerator {

    public IDGenerator() {    }

    /**
     * Generates a simple ID, no collision checking or hierarchical generation.
     * @return new ObjectID of type IDVersion.SIMPLE
     */
    public static ObjectID GenerateSimpleID() {
        return new ObjectID(UUID.randomUUID(), ObjectID.IDVersion.SIMPLE);
    }
}
