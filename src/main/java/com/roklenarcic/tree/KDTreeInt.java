package com.roklenarcic.tree;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * 2-D tree with coordinates of the int type.
 *
 * It is instantiated with a list of points, and the coordinates of the area of the map. The area information
 * is used in wrapping.
 *
 * The maximum area coordinates are limited to [-590000000, 590000000].
 *
 *
 * @author Rok Lenarcic
 *
 * @param <T>
 *            Value to be stored in points.
 */
public class KDTreeInt<T> {

    @SuppressWarnings("unchecked")
    private final Comparator<Point<?>>[] comparators = (Comparator<Point<?>>[]) new Comparator<?>[] { Point.createComparator(0), Point.createComparator(1) };

    private final Point<T> root;
    private final int xMax;
    private final int xMin;
    private final int yMax;
    private final int yMin;

    /**
     * Build a tree from list of points, where points are given by the objects of the Point class. The map
     * area is limited by the limits given, inclusive.
     *
     * @param points
     *            list of points with coordinates and values
     * @param xMin
     *            minimum value of x coordinate of a point, inclusive
     * @param yMin
     *            minimum value of y coordinate of a point, inclusive
     * @param xMax
     *            maximum value of x coordinate of a point, inclusive
     * @param yMax
     *            maximum value of y coordinate of a point, inclusive
     */
    public KDTreeInt(List<Point<T>> points, int xMin, int yMin, int xMax, int yMax) {
        if (xMin < xMax && yMin < yMax) {
            this.xMin = xMin;
            this.yMin = yMin;
            this.xMax = xMax;
            this.yMax = yMax;
            if (xMax > 590000000 || xMin < -590000000 || yMax > 590000000 || yMin < -590000000) {
                throw new IllegalArgumentException("Area limits too big, out of [-590M...590M] interval.");
            }
            root = buildTree(points, 0);
        } else {
            throw new IllegalArgumentException("Area limits are not correctly ordered: " + xMin + " < " + xMax + " " + yMin + " < " + yMax);
        }
    }

    /**
     * Find the nearest point to the coordinates given, that is within the maximum distance given, inclusive.
     * The smaller the maximum distance, the faster the query.
     *
     * @param x
     *            x coordinate of the query point
     * @param y
     *            y coordinate of the query point
     * @param maxDistance
     *            maximum distance from the point of the query to search
     * @return the point in the tree closest to the coordinates given within max distance
     */
    public Point<T> findNearest(int x, int y, int maxDistance) {
        NearestPoint<T> nearest = new NearestPoint<T>();
        if (root != null) {
            long md = maxDistance;
            nearest.distance = md * md;
            root.findNearest(x, y, nearest);
        }
        return nearest.p;
    }

    /**
     * Find a number of nearest points to the coordinates given that are within the maximum distance given,
     * inclusive. The smaller the maximum distance, the faster the query. The points returned are sorted from
     * the closest to the farthest.
     *
     * @param x
     *            x coordinate of the query point
     * @param y
     *            y coordinate of the query point
     * @param maxDistance
     *            maximum distance from the point of the query to search
     * @param numberOfNearest
     *            number of points to return
     * @return an iterable of points in the tree closest to the coordinates given, in order of ascending
     *         distance
     */
    public Iterable<Point<T>> findNearest(int x, int y, int maxDistance, int numberOfNearest) {
        if (root != null) {
            long md = maxDistance;
            LinkedList<T> nearestPoints = LinkedList.constructChain(numberOfNearest, md * md);
            nearestPoints = root.findNearest(x, y, nearestPoints).dropEmptyPrefix();
            if (nearestPoints != null) {
                return nearestPoints.reverse();
            } else {
                return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Find the nearest point to the coordinates given, that is within the maximum distance given, inclusive.
     * The smaller the maximum distance, the faster the query. The search wraps across the x axis, where
     * borders used are the ones given in the constructor. Borders are inclusive.
     *
     * @param x
     *            x coordinate of the query point
     * @param y
     *            y coordinate of the query point
     * @param maxDistance
     *            maximum distance from the point of the query to search
     * @return the point in the tree closest to the coordinates given within max distance
     */
    public Point<T> findNearestWithWrapping(int x, int y, int maxDistance) {
        NearestPoint<T> nearest = new NearestPoint<T>();
        if (root != null) {
            long md = maxDistance;
            nearest.distance = md * md;
            root.findNearest(x, y, nearest);
            if (nearest.distance > 0) {
                // No point found within max distance, see if point + max distance crosses the wrap
                // It can only wrap around the nearest border.
                if (xMin + xMax > x << 1) {
                    long distanceToLeftBorder = x - xMin;
                    if (distanceToLeftBorder * distanceToLeftBorder < nearest.distance) {
                        root.findNearest(xMax + distanceToLeftBorder + 1, y, nearest);
                    }
                } else {
                    long distanceToRightBorder = xMax - x;
                    if (distanceToRightBorder * distanceToRightBorder < nearest.distance) {
                        root.findNearest(xMin - distanceToRightBorder - 1, y, nearest);
                    }
                }
            }
        }
        return nearest.p;
    }

    /**
     * Find a number of nearest points to the coordinates given that are within the maximum distance given,
     * inclusive. The smaller the maximum distance, the faster the query. The points returned are sorted from
     * the closest to the farthest.
     *
     * The search wraps across the x axis, where borders used are the ones given in the constructor. Borders
     * are inclusive.
     *
     * @param x
     *            x coordinate of the query point
     * @param y
     *            y coordinate of the query point
     * @param maxDistance
     *            maximum distance from the point of the query to search
     * @param numberOfNearest
     *            number of points to return
     * @return an iterable of points in the tree closest to the coordinates given, in order of ascending
     *         distance
     */
    public Iterable<Point<T>> findNearestWithWrapping(int x, int y, int maxDistance, int numberOfNearest) {
        if (root != null) {
            long md = maxDistance;
            LinkedList<T> nearestPoints = LinkedList.constructChain(numberOfNearest, md * md);
            nearestPoints = root.findNearest(x, y, nearestPoints);
            if (nearestPoints.distance > 0) {
                // No point found within max distance, see if point + max distance crosses the wrap
                // It can only wrap around the nearest border.
                if (xMin + xMax > x << 1) {
                    long distanceToLeftBorder = x - xMin;
                    if (distanceToLeftBorder * distanceToLeftBorder < nearestPoints.distance) {
                        nearestPoints = root.findNearest(xMax + distanceToLeftBorder + 1, y, nearestPoints);
                    }
                } else {
                    long distanceToRightBorder = xMax - x;
                    if (distanceToRightBorder * distanceToRightBorder < nearestPoints.distance) {
                        nearestPoints = root.findNearest(xMin - distanceToRightBorder - 1, y, nearestPoints);
                    }
                }
            }
            nearestPoints = nearestPoints.dropEmptyPrefix();
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
            Collections.sort(points, comparators[axis]);
            int pivotIdx = points.size() >> 1;
            if ((points.size() & 1) == 0) { // If odd size
                // Shift pivot to the left every second level so for lists of size 4
                // the pivot is idx 1 and 2 every other level.
                pivotIdx -= axis;
            }
            Point<T> p = points.get(pivotIdx);
            p.rotate(axis);
            if (p.x > xMax || p.x < xMin || p.y > yMax || p.y < yMin) {
                throw new IllegalArgumentException("Point " + p + " has coordinates out of the tree area.");
            }
            axis = axis ^ 1;
            // Build subtree. Bigger branch also contains points that has equal axis value to the pivot.
            p.smaller = buildTree(points.subList(0, pivotIdx), axis);
            p.bigger = buildTree(points.subList(pivotIdx + 1, points.size()), axis);
            return p;
        }
    }

    /**
     * Point in the 2-D space with a user specified value attached to it.
     *
     * @author Rok Lenarcic
     *
     * @param <T>
     *            type of value
     */
    public static class Point<T> {

        private static Comparator<Point<?>> createComparator(final int axis) {
            return new Comparator<KDTreeInt.Point<?>>() {
                public int compare(Point<?> o1, Point<?> o2) {
                    return axis == 0 ? o1.x - o2.x : o1.y - o2.y;
                }
            };
        }

        // Axis value other value contain x and y, but in such order
        // that the value that is used as axis for this point is in axisValue variable.
        private int axisValue;
        private int otherValue;

        private Point<T> smaller, bigger;

        private final T value;

        private final int x, y;

        /**
         * New point with the specified coordinates and the value.
         *
         * @param x
         * @param y
         * @param value
         */
        public Point(int x, int y, T value) {
            super();
            this.x = x;
            this.y = y;
            this.axisValue = x;
            this.otherValue = y;
            this.value = value;
        }

        /**
         *
         * @return the value of the point
         */
        public T getValue() {
            return value;
        }

        /**
         *
         * @return the x coordinate of the point
         */
        public int getX() {
            return x;
        }

        /**
         *
         * @return the y coordinate of the point
         */
        public int getY() {
            return y;
        }

        private LinkedList<T> findNearest(long queryAxis, long queryOther, LinkedList<T> currentBest) {
            // Negative number means this point is on the left to the query point.
            long diffAxis = queryAxis - axisValue;
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
                currentBest = closerChild.findNearest(queryOther, queryAxis, currentBest);
            }
            // Now let's see it the other side is still relevant. Since that search
            // might have narrowed the circle.

            // Calculate distance to axis.
            long distanceToHyperplane = diffAxis * diffAxis;
            // See if line intersects circle
            if (distanceToHyperplane <= currentBest.distance) {
                // If it does then this point might be the best one.
                long diffOther = queryOther - otherValue;
                long d = distanceToHyperplane + diffOther * diffOther;
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
                    currentBest = fartherChild.findNearest(queryOther, queryAxis, currentBest);
                }
            }
            return currentBest;
        }

        private void findNearest(long queryAxis, long queryOther, NearestPoint<T> currentBest) {
            // Negative number means this point is on the left to the query point.
            long diffAxis = queryAxis - axisValue;
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
                closerChild.findNearest(queryOther, queryAxis, currentBest);
            }
            // Now let's see it the other side is still relevant. Since that search
            // might have narrowed the circle.

            // Calculate distance to axis.
            long distanceToHyperplane = diffAxis * diffAxis;
            // See if line intersects circle
            if (distanceToHyperplane <= currentBest.distance) {
                // If it does then this point might be the best one.
                long diffOther = queryOther - otherValue;
                long d = distanceToHyperplane + diffOther * diffOther;
                if (d <= currentBest.distance) {
                    currentBest.p = this;
                    currentBest.distance = d;
                }
                // Search the other side.
                if (fartherChild != null) {
                    fartherChild.findNearest(queryOther, queryAxis, currentBest);
                }
            }
        }

        private void rotate(int axis) {
            if (axis == 1) {
                int temp = axisValue;
                axisValue = otherValue;
                otherValue = temp;
            }
        }

    }

    private static class LinkedList<T> implements Iterable<Point<T>> {

        private static <T> LinkedList<T> constructChain(int length, long distance) {
            LinkedList<T> ret = new LinkedList<T>(distance);
            for (int i = 1; i < length; i++) {
                LinkedList<T> nextNode = new LinkedList<T>(distance);
                nextNode.tail = ret;
                ret = nextNode;
            }
            return ret;
        }

        private long distance;
        private Point<T> head;
        private LinkedList<T> tail;

        private LinkedList(long distance) {
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
        private long distance;
        private Point<T> p;
    }

}
