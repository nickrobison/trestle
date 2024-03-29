package com.nickrobison.trestle.reasoner.caching.tdtree;

/**
 * Created by nrobison on 2/13/17.
 */
public interface LeafKeySchema {
    void objectID(long objectID);
    void start(long start);
    void end(long end);
    long objectID();
    long start();
    long end();
}
