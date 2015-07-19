package com.roklenarcic.tree;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class KDTreeInt<T> {

    @SuppressWarnings("unchecked")
    private final Comparator<Point<?>>[] comparators = (Comparator<Point<?>>[]) new Comparator<?>[] { Point.createComparator(0), Point.createComparator(1) };

    private final Point<T> root;
    private final int xMax;
    private final int xMin;
    private final int yMax;
    private final int yMin;

    // Build a tree from list of points.
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

    public Point<T> findNearest(int x, int y, int maxDistance) {
        NearestPoint<T> nearest = new NearestPoint<T>();
        if (root != null) {
            long md = maxDistance;
            nearest.distance = md * md;
            root.findNearest(x, y, nearest);
        }
        return nearest.p;
    }

    // Search and wrap on x axis. The borders are inclusive e.g. -180, 180 means that both those
    // coordinates are valid.
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

        public Point(int x, int y, T value) {
            super();
            this.x = x;
            this.y = y;
            this.axisValue = x;
            this.otherValue = y;
            this.value = value;
        }

        public T getValue() {
            return value;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        @Override
        public String toString() {
            return "X=" + x + ", Y=" + y;
        }

        private void findNearest(long queryAxis, long queryOther, NearestPoint<T> currentBest) {
            // Negative number means this point is on the left to the query point.
            long diffAxis = queryAxis - axisValue;
            if (diffAxis >= 0) {
                // First check the closer side
                if (bigger != null) {
                    bigger.findNearest(queryOther, queryAxis, currentBest);
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
                    if (smaller != null) {
                        smaller.findNearest(queryOther, queryAxis, currentBest);
                    }
                }
            } else {
                // First check the closer side
                if (smaller != null) {
                    smaller.findNearest(queryOther, queryAxis, currentBest);
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
                    if (bigger != null) {
                        bigger.findNearest(queryOther, queryAxis, currentBest);
                    }
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

    private static class NearestPoint<T> {
        private long distance;
        private Point<T> p;
    }
}