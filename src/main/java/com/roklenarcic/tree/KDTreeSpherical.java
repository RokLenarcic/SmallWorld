package com.roklenarcic.tree;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * 3-D tree with coordinates of the double type.
 *
 * It is instantiated with a list of points, with the coordinates longitude and latitude. The values are
 * limited to range [-180, 180] and [-90, 90].
 *
 *
 * @author Rok Lenarcic
 *
 * @param <T>
 *            Value to be stored in points.
 */
public class KDTreeSpherical<T> {

    private static final double DEGREES_IN_RADIAN = 57.29577951308233;

    @SuppressWarnings("unchecked")
    private final Comparator<Point<T>>[] comparators = (Comparator<Point<T>>[]) new Comparator<?>[] { Point.createComparator(0), Point.createComparator(1),
            Point.createComparator(2) };

    private double maxDistance;
    private final Point<T> root;

    /**
     * Build a tree from list of points, where points are given by the objects of the Point class.
     *
     * @param points
     *            list of points with coordinates and values
     * @param maxDistance
     *            max distance from query point to search, must be in [0, 180] range
     */
    public KDTreeSpherical(List<Point<T>> points, double maxDistance) {
        // Convert maxDistance along the sphere into chord length
        // chord = 2 * sin (1/2 * angle) where angle is the maxDistance since we have a unit
        // sphere.
        if (maxDistance > 180 || maxDistance < 0) {
            throw new IllegalArgumentException("Max distance must be between 0 and 180.");
        }
        maxDistance = 2 * Math.sin(0.5 * maxDistance / DEGREES_IN_RADIAN);
        this.maxDistance = maxDistance * maxDistance;
        root = buildTree(points, 0);
    }

    /**
     * Find the nearest point to the coordinates given, that is within the maximum distance, inclusive. The
     * smaller the maximum distance, the faster the query.
     *
     * @param longitude
     *            longitude coordinate of the query point
     * @param latitude
     *            latitude coordinate of the query point
     * @return the point in the tree closest to the coordinates given within max distance
     */
    public Point<T> findNearest(double longitude, double latitude) {
        NearestPoint<T> nearest = new NearestPoint<T>();
        if (root != null) {
            nearest.distance = this.maxDistance;
            // Calculate those cartesian coordinates
            double azimuth = (longitude + 180) / DEGREES_IN_RADIAN;
            double inclination = (-latitude + 90) / DEGREES_IN_RADIAN;
            double sinAzimuth = Math.sin(azimuth);
            double cosAzimuth = Math.cos(azimuth);
            double sinInclination = Math.sin(inclination);
            double cosInclination = Math.cos(inclination);
            root.findNearest(sinInclination * cosAzimuth, sinInclination * sinAzimuth, cosInclination, nearest);
        }
        return nearest.p;
    }

    /**
     * Find a number of nearest points to the coordinates given that are within the maximum distance given,
     * inclusive. The smaller the maximum distance, the faster the query. The points returned are sorted from
     * the closest to the farthest.
     *
     * @param longitude
     *            longitude coordinate of the query point
     * @param latitude
     *            latitude coordinate of the query point
     * @param numberOfNearest
     *            number of points to return
     * @return an iterable of points in the tree closest to the coordinates given, in order of ascending
     *         distance
     */
    public Iterable<Point<T>> findNearest(double longitude, double latitude, int numberOfNearest) {
        if (root != null) {
            LinkedList<T> nearestPoints = LinkedList.constructChain(numberOfNearest, this.maxDistance);
            // Calculate those cartesian coordinates
            double azimuth = (longitude + 180) / DEGREES_IN_RADIAN;
            double inclination = (-latitude + 90) / DEGREES_IN_RADIAN;
            double sinAzimuth = Math.sin(azimuth);
            double cosAzimuth = Math.cos(azimuth);
            double sinInclination = Math.sin(inclination);
            double cosInclination = Math.cos(inclination);
            nearestPoints = root.findNearest(sinInclination * cosAzimuth, sinInclination * sinAzimuth, cosInclination, nearestPoints).dropEmptyPrefix();
            if (nearestPoints != null) {
                return nearestPoints.reverse();
            } else {
                return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
        }
    }

    private Point<T> buildTree(List<Point<T>> points, int axis) {
        if (points.size() == 0) {
            return null;
        } else {
            // Sort by axis.
            Collections.sort(points, comparators[axis % 3]);
            int pivotIdx = points.size() >> 1;
            if ((points.size() & 1) == 0) { // If odd size
                // Shift pivot to the left every second level so for lists of size 4
                // the pivot is idx 1 and 2 every other level.
                pivotIdx -= axis & 1;
            }

            Point<T> p = points.get(pivotIdx);
            p.rotate(axis % 3);
            // Build subtree. Bigger branch also contains points that has equal axis value to the pivot.
            p.smaller = buildTree(points.subList(0, pivotIdx), axis + 1);
            p.bigger = buildTree(points.subList(pivotIdx + 1, points.size()), axis + 1);
            return p;
        }
    }

    /**
     * Point in the 3-D space on a sphere with a user specified value attached to it.
     *
     * @author Rok Lenarcic
     *
     * @param <T>
     *            type of value
     */
    public static class Point<T> {

        private static Comparator<Point<?>> createComparator(final int axis) {
            return new Comparator<Point<?>>() {
                public int compare(Point<?> o1, Point<?> o2) {
                    double d = axis == 0 ? o1.axisValue - o2.axisValue : (axis == 1 ? o1.otherValue - o2.otherValue : o1.otherValue2 - o2.otherValue2);
                    if (d > 0) {
                        return 1;
                    } else if (d < 0) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            };
        }

        // Axis value other value contain x and y, but in such order
        // that the value that is used as axis for this point is in axisValue variable.
        private double axisValue;
        private final double longitude, latitude;
        private double otherValue;

        private double otherValue2;

        private Point<T> smaller, bigger;

        private final T value;

        /**
         * New point with the specified coordinates and the value. The constructor maps the point to 3-D space
         * (non-trivial cost).
         *
         * @param longitude
         * @param latitude
         * @param value
         */
        public Point(double longitude, double latitude, T value) {
            this.longitude = longitude;
            this.latitude = latitude;
            if (longitude > 180 || longitude < -180 || latitude > 90 || latitude < -90) {
                throw new IllegalArgumentException("Point " + this + " has longitude outside [-180, 180] or latitude outside [-90, 90].");
            }
            // Save the coordinates
            // Now translate them:
            // Latitude 90 is 0 inclination, -90 is 180
            // Longitude -180 is 0 azimuth (different than earth, but we don't care)
            // then into the radians.
            double azimuth = (longitude + 180) / DEGREES_IN_RADIAN;
            double inclination = (-latitude + 90) / DEGREES_IN_RADIAN;
            double sinAzimuth = Math.sin(azimuth);
            double cosAzimuth = Math.cos(azimuth);
            double sinInclination = Math.sin(inclination);
            double cosInclination = Math.cos(inclination);
            this.axisValue = sinInclination * cosAzimuth;
            this.otherValue = sinInclination * sinAzimuth;
            this.otherValue2 = cosInclination;
            this.value = value;
        }

        /**
         *
         * @return the latitude of the point
         */
        public double getLatitude() {
            return latitude;
        }

        /**
         *
         * @return the longitude of the point
         */
        public double getLongitude() {
            return longitude;
        }

        /**
         *
         * @return the value of the point
         */
        public T getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "Longitude=" + longitude + ", Latitude=" + latitude;
        }

        private LinkedList<T> findNearest(double queryAxis, double queryOther, double queryOther2, LinkedList<T> currentBest) {
            // Negative number means this point is on the left to the query point.
            double diffAxis = queryAxis - axisValue;
            Point<T> closerChild, fartherChild;
            if (diffAxis >= 0) {
                closerChild = bigger;
                fartherChild = smaller;
            } else {
                closerChild = smaller;
                fartherChild = bigger;
            }
            // First check the closer side
            if (closerChild != null) {
                currentBest = closerChild.findNearest(queryOther, queryOther2, queryAxis, currentBest);
            }
            // Now let's see it the other side is still relevant. Since that search
            // might have narrowed the circle.

            // Calculate distance to axis.
            double distanceToHyperplane = diffAxis * diffAxis;
            // See if line intersects circle
            if (distanceToHyperplane <= currentBest.distance) {
                // If it does then this point might be the best one.
                double diffOther = queryOther - otherValue;
                double diffOther2 = queryOther2 - otherValue2;
                double d = distanceToHyperplane + diffOther * diffOther + diffOther2 * diffOther2;
                if (d <= currentBest.distance) {
                    // Start with the farthest point in the list
                    // This point is farther than the this point
                    LinkedList<T> farther = currentBest;
                    LinkedList<T> newHead = currentBest;
                    // Scroll down the list to find the last node that is farther than this node
                    while (farther.tail != null && d <= farther.tail.distance) {
                        farther = farther.tail;
                        newHead = currentBest.tail;
                    }
                    currentBest.head = this;
                    currentBest.distance = d;
                    // Here's a bit of a trickeroo. We've got 2 scenarios:
                    // - The farthest (first) point in the list is the one being replaced. In that case
                    // it's really easy, we're done already.
                    // - In other cases we need assign first point (currentBest) into the chain as tail of
                    // "farther" then we need to update currentBest as tail of currentBest to keep the
                    // currentBest as the start of the chain.
                    //
                    // The trick both cases can be solved by the same code.
                    LinkedList<T> tail = farther.tail;
                    farther.tail = currentBest;
                    currentBest.tail = tail;
                    currentBest = newHead;
                }
                // Search the other side.
                if (fartherChild != null) {
                    currentBest = fartherChild.findNearest(queryOther, queryOther2, queryAxis, currentBest);
                }
            }
            return currentBest;
        }

        private void findNearest(double queryAxis, double queryOther, double queryOther2, NearestPoint<T> currentBest) {
            // Negative number means this point is on the left to the query point.
            double diffAxis = queryAxis - axisValue;
            Point<T> closerChild, fartherChild;
            if (diffAxis >= 0) {
                closerChild = bigger;
                fartherChild = smaller;
            } else {
                closerChild = smaller;
                fartherChild = bigger;
            }
            // First check the closer side
            if (closerChild != null) {
                closerChild.findNearest(queryOther, queryOther2, queryAxis, currentBest);
            }
            // Now let's see it the other side is still relevant. Since that search
            // might have narrowed the circle.

            // Calculate distance to axis.
            double distanceToHyperplane = diffAxis * diffAxis;
            // See if line intersects circle
            if (distanceToHyperplane <= currentBest.distance) {
                // If it does then this point might be the best one.
                double diffOther = queryOther - otherValue;
                double diffOther2 = queryOther2 - otherValue2;
                double d = distanceToHyperplane + diffOther * diffOther + diffOther2 * diffOther2;
                if (d <= currentBest.distance) {
                    currentBest.p = this;
                    currentBest.distance = d;
                }
                // Search the other side.
                if (fartherChild != null) {
                    fartherChild.findNearest(queryOther, queryOther2, queryAxis, currentBest);
                }
            }
        }

        private void rotate(int axis) {
            if (axis == 1) {
                double temp = otherValue;
                otherValue = otherValue2;
                otherValue2 = axisValue;
                axisValue = temp;
            } else if (axis == 2) {
                // Rotate by 2 slots
                double temp = otherValue2;
                otherValue2 = otherValue;
                otherValue = axisValue;
                axisValue = temp;
            }
        }

    }

    private static class LinkedList<T> implements Iterable<Point<T>> {

        private static <T> LinkedList<T> constructChain(int length, double distance) {
            LinkedList<T> ret = new LinkedList<T>(distance);
            for (int i = 1; i < length; i++) {
                LinkedList<T> nextNode = new LinkedList<T>(distance);
                nextNode.tail = ret;
                ret = nextNode;
            }
            return ret;
        }

        private double distance;
        private Point<T> head;
        private LinkedList<T> tail;

        private LinkedList(double distance) {
            this.distance = distance;
        }

        public LinkedList<T> dropEmptyPrefix() {
            LinkedList<T> c = LinkedList.this;
            while (c != null && c.head == null) {
                c = c.tail;
            }
            return c;
        }

        public Iterator<Point<T>> iterator() {
            return new Iterator<Point<T>>() {

                private LinkedList<T> cursor = LinkedList.this;

                public boolean hasNext() {
                    return cursor != null;
                }

                public Point<T> next() {
                    Point<T> p = cursor.head;
                    cursor = cursor.tail;
                    return p;
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public LinkedList<T> reverse() {
            LinkedList<T> c = LinkedList.this;
            LinkedList<T> prev = null;
            while (c != null) {
                LinkedList<T> tmp = c;
                c = c.tail;
                tmp.tail = prev;
                prev = tmp;
            }
            return prev;
        }
    }

    private static class NearestPoint<T> {
        private double distance;
        private Point<T> p;
    }
}
