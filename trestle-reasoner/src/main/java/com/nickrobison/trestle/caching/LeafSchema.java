package com.nickrobison.trestle.caching;

/**
 * Created by nrobison on 2/13/17.
 */
public interface LeafSchema {
    void start(double start);
    void end(double end);
    void direction(short direction);
    double start();
    double end();
    short direction();
}
