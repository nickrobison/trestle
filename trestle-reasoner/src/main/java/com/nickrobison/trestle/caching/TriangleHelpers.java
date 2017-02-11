package com.nickrobison.trestle.caching;

import org.apache.commons.math3.util.FastMath;

/**
 * Created by nrobison on 2/10/17.
 */
public class TriangleHelpers {
    static boolean checkIntersection(TriangleApex apex, int direction, int leafLength, long startTime, long endTime) {
//        find the apex points
        final double[] verticies = getTriangleVerticies(getAdjustedLength(leafLength), direction, apex.start, apex.end);
        return pointInTriangle(startTime, endTime, verticies);
    }

    /**
     * Determines if a given point in contained within the given triangle
     * Verticies are passed in as an array (X/Y pairs) in an anti-clockwise orientation
     *
     * @param start             - start (X) coordinate of point
     * @param end               - end (Y) coordinate of point
     * @param triangleVerticies - Triangle verticies (x/y clockwise points)
     * @return - point in triangle?
     */
    static boolean pointInTriangle(long start, long end, double[] triangleVerticies) {
        final boolean b1, b2, b3;
        b1 = sign(start, end, triangleVerticies[0], triangleVerticies[1], triangleVerticies[2], triangleVerticies[3]) < 0.0d;
        b2 = sign(start, end, triangleVerticies[2], triangleVerticies[3], triangleVerticies[4], triangleVerticies[5]) < 0.0d;
        b3 = sign(start, end, triangleVerticies[4], triangleVerticies[5], triangleVerticies[0], triangleVerticies[1]) < 0.0d;

        return ((b1 == b2) & (b2 == b3));
    }

    //    http://stackoverflow.com/questions/2049582/how-to-determine-if-a-point-is-in-a-2d-triangle
    static double sign(double p1x, double p1y, double p2x, double p2y, double p3x, double p3y) {
        return (p1x - p3x) * (p2y - p3y) - (p2x - p3x) * (p1y - p3y);
    }

    /**
     * Gets the
     * Returns an anti-clockwise array of double x/y pairs in the form P(n)X, P(n)Y
     *
     * @param adjustedLength - Adjusted length of Triangle side
     * @param direction      - Direction of triangle
     * @param triangleStart  - Triangle start (X)
     * @param triangleEnd    - Triangle end (Y)
     * @return - double Array of triangle vertex coordinates (X/Y)
     */
    static double[] getTriangleVerticies(double adjustedLength, int direction, double triangleStart, double triangleEnd) {
        double[] verticies = new double[6];
        verticies[0] = triangleStart;
        verticies[1] = triangleEnd;
        if (direction == 0) {
            final double l2 = (adjustedLength * TDTree.ROOTTWO) * 2;
            verticies[2] = triangleStart - l2;
            verticies[3] = triangleEnd - l2;
            verticies[4] = triangleStart + l2;
            verticies[5] = triangleEnd + l2;
        } else if (direction == 1) {
            verticies[2] = triangleStart - adjustedLength;
            verticies[3] = triangleEnd;
            verticies[4] = triangleStart;
            verticies[5] = triangleEnd - adjustedLength;
        } else if (direction == 2) {
            final double l2 = (adjustedLength * TDTree.ROOTTWO) * 2;
            verticies[2] = triangleStart - l2;
            verticies[3] = triangleEnd + l2;
            verticies[4] = triangleStart - l2;
            verticies[5] = triangleEnd - l2;

        } else if (direction == 3) {
            verticies[2] = triangleStart;
            verticies[3] = triangleEnd + adjustedLength;
            verticies[4] = triangleStart - adjustedLength;
            verticies[5] = triangleEnd;
        } else if (direction == 4) {
            final double l2 = (adjustedLength * TDTree.ROOTTWO) * 2;
            verticies[2] = triangleStart + l2;
            verticies[3] = triangleEnd + l2;
            verticies[4] = triangleStart - l2;
            verticies[5] = triangleEnd + l2;
        } else if (direction == 5) {
            verticies[2] = triangleStart + adjustedLength;
            verticies[3] = triangleEnd;
            verticies[4] = triangleStart;
            verticies[5] = triangleEnd + adjustedLength;
        } else if (direction == 6) {
            final double l2 = (adjustedLength * TDTree.ROOTTWO) * 2;
            verticies[2] = triangleStart + l2;
            verticies[3] = triangleEnd - l2;
            verticies[4] = triangleStart + l2;
            verticies[5] = triangleEnd + l2;
        } else {
            verticies[2] = triangleStart;
            verticies[3] = triangleEnd + adjustedLength;
            verticies[4] = triangleStart + adjustedLength;
            verticies[5] = triangleEnd;
        }

        return verticies;
    }

    static ChildDirection calculateChildDirection(int parentDirection) {
        if (parentDirection >= 1 & parentDirection <= 4) {
            return new ChildDirection((parentDirection + 5) % 8, parentDirection + 3);
        } else {
            return new ChildDirection((parentDirection + 3) % 8, (parentDirection + 5) % 8);
        }
    }

    static TriangleApex calculateChildApex(int leafLength, int parentDirection, double parentStart, double parentEnd) {
        final double length = getAdjustedLength(leafLength);
        if (parentDirection == 0) {
            return new TriangleApex(
                    parentStart,
                    parentEnd - length);
        } else if (parentDirection == 1) {
            return new TriangleApex(
                    parentStart - (length / TDTree.ROOTTWO),
                    parentEnd - (length / TDTree.ROOTTWO));
        } else if (parentDirection == 2) {
            return new TriangleApex(
                    parentStart - length,
                    parentEnd);
        } else if (parentDirection == 3) {
            return new TriangleApex(
                    parentStart - (length / TDTree.ROOTTWO),
                    parentEnd + length);
        } else if (parentDirection == 4) {
            return new TriangleApex(
                    parentStart,
                    parentEnd + length);
        } else if (parentDirection == 5) {
            return new TriangleApex(
                    parentStart + (length / TDTree.ROOTTWO),
                    parentEnd + (length / TDTree.ROOTTWO));
        } else if (parentDirection == 6) {
            return new TriangleApex(
                    parentStart + length,
                    parentEnd);
        } else {
            return new TriangleApex(
                    parentStart + (length / TDTree.ROOTTWO),
                    parentEnd - (length / TDTree.ROOTTWO));
        }
    }

    /**
     * Gets the number of bits used for the integer value
     *
     * @param leafID - Integer of leaf ID
     * @return - number of used bits
     */
    static int getIDLength(int leafID) {
        return (int) FastMath.floor(Math.log10(leafID) / FastMath.log10(2)) + 1;
    }

    /**
     * Get the adjusted length for the given leaf ID length
     *
     * @param leafLength - length of LeadID
     * @return - double of adjusted length
     */
    static double getAdjustedLength(int leafLength) {
        return TDTree.maxValue * (FastMath.pow(TDTree.ROOTTWO / 2, leafLength - 1));
    }

    static class ChildDirection {
        final int lowerChild;
        final int higherChild;

        ChildDirection(int lowerChild, int higherChild) {
            this.lowerChild = lowerChild;
            this.higherChild = higherChild;
        }
    }

    static class TriangleApex {
        final double start;
        final double end;

        TriangleApex(double start, double end) {
            this.end = end;
            this.start = start;
        }
    }
}
