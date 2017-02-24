package com.nickrobison.trestle.common;

/**
 * Created by nrobison on 2/23/17.
 */

/**
 * Simple class for returning a pair of typed objects from things like maps and asyncs
 * @param <Left> - Type of left side of the pair
 * @param <Right> - Type or right side of the pair
 */
public class TrestlePair<Left, Right> {

    private final Left left;
    private final Right right;

    public TrestlePair(Left left, Right right) {
        this.left = left;
        this.right = right;
    }

    public Left getLeft() {
        return this.left;
    }

    public Right getRight() {
        return this.right;
    }
}
