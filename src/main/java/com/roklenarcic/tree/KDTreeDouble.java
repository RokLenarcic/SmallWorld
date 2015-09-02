package com.roklenarcic.tree;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class KDTreeDouble<T> {

    private static final double MAX_COORD_VAL = (Math.sqrt(Double.MAX_VALUE) / 1.81) / 2;

    @SuppressWarnings("unchecked")
    private final Comparator<Point<T>>[] comparators = (Comparator<Point<T>>[]) new Comparator<?>[] { Point.createComparator(0), Point.createComparator(1) };

    private final Point<T> root;
    private final double xMax;
    private final double xMin;
    private final double yMax;
    private final double yMin;

    // Build a tree from list of points.
    public KDTreeDouble(List<Point<T>> points, double xMin, double yMin, double xMax, double yMax) {
        if (xMin < xMax && yMin < yMax) {
            this.xMin = xMin;
            this.yMin = yMin;
            this.xMax = xMax;
            this.yMax = yMax;
            if (xMax > MAX_COORD_VAL || xMin < -MAX_COORD_VAL || yMax > MAX_COORD_VAL || yMin < -MAX_COORD_VAL) {
                throw new IllegalArgumentException("Area limits too big, out of [-3.7E153...3.7E153] interval.");
            }
            root = buildTree(points, 0);
        } else {
            throw new IllegalArgumentException("Area limits are not correctly ordered: " + xMin + " < " + xMax + " " + yMin + " < " + yMax);
        }
    }

    public Point<T> findNearest(double x, double y, double maxDistance) {
        NearestPoint<T> nearest = new NearestPoint<T>();
        if (root != null) {
            double md = maxDistance;
            nearest.distance = md * md;
            root.findNearest(x, y, nearest);
        }
        return nearest.p;
    }

    public Iterable<Point<T>> findNearest(double x, double y, double maxDistance, int numberOfNearest) {
        if (root != null) {
            double md = maxDistance;
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

    // Search and wrap on x axis. The borders are inclusive e.g. -180, 180 means that both those
    // coordinates are valid.
    public Point<T> findNearestWithWrapping(double x, double y, double maxDistance) {
        NearestPoint<T> nearest = new NearestPoint<T>();
        if (root != null) {
            nearest.distance = maxDistance * maxDistance;
            root.findNearest(x, y, nearest);
            if (nearest.distance > 0) {
                // No point found within max distance, see if point + max distance crosses the wrap
                // It can only wrap around the nearest border.
                if (xMin + xMax > x / 2) {
                    double distanceToLeftBorder = x - xMin;
                    if (distanceToLeftBorder * distanceToLeftBorder < nearest.distance) {
                        root.findNearest(xMax + distanceToLeftBorder + 1, y, nearest);
                    }
                } else {
                    double distanceToRightBorder = xMax - x;
                    if (distanceToRightBorder * distanceToRightBorder < nearest.distance) {
                        root.findNearest(xMin - distanceToRightBorder - 1, y, nearest);
                    }
                }
            }
        }
        return nearest.p;
    }

    // Search and wrap on x axis. The borders are inclusive e.g. -180, 180 means that both those
    // coordinates are valid.
    public Iterable<Point<T>> findNearestWithWrapping(double x, double y, double maxDistance, int numberOfNearest) {
        if (root != null) {
            double md = maxDistance;
            LinkedList<T> nearestPoints = LinkedList.constructChain(numberOfNearest, md * md);
            nearestPoints = root.findNearest(x, y, nearestPoints);
            if (nearestPoints.distance > 0) {
                // No point found within max distance, see if point + max distance crosses the wrap
                // It can only wrap around the nearest border.
                if (xMin + xMax > x / 2) {
                    double distanceToLeftBorder = x - xMin;
                    if (distanceToLeftBorder * distanceToLeftBorder < nearestPoints.distance) {
                        nearestPoints = root.findNearest(xMax + distanceToLeftBorder + 1, y, nearestPoints);
                    }
                } else {
                    double distanceToRightBorder = xMax - x;
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

    public static class Point<T> {

        private static Comparator<Point<?>> createComparator(final int axis) {
            return new Comparator<Point<?>>() {
                public int compare(Point<?> o1, Point<?> o2) {
                    double d = axis == 0 ? o1.x - o2.x : o1.y - o2.y;
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
        private double otherValue;

        private Point<T> smaller, bigger;

        private final T value;

        private final double x, y;

        public Point(double x, double y, T value) {
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

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        @Override
        public String toString() {
            return "X=" + x + ", Y=" + y;
        }

        private LinkedList<T> findNearest(double queryAxis, double queryOther, LinkedList<T> currentBest) {
            // Negative number means this point is on the left to the query point.
            double diffAxis = queryAxis - axisValue;
            if (diffAxis >= 0) {
                // First check the closer side
                if (bigger != null) {
                    currentBest = bigger.findNearest(queryOther, queryAxis, currentBest);
                }
                // Now let's see it the other side is still relevant. Since that search
                // might have narrowed the circle.

                // Calculate distance to axis.
                double distanceToHyperplane = diffAxis * diffAxis;
                // See if line intersects circle
                if (distanceToHyperplane <= currentBest.distance) {
                    // If it does then this point might be the best one.
                    double diffOther = queryOther - otherValue;
                    double d = distanceToHyperplane + diffOther * diffOther;
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
                    if (smaller != null) {
                        currentBest = smaller.findNearest(queryOther, queryAxis, currentBest);
                    }
                }
            } else {
                // First check the closer side
                if (smaller != null) {
                    currentBest = smaller.findNearest(queryOther, queryAxis, currentBest);
                }
                // Now let's see it the other side is still relevant. Since that search
                // might have narrowed the circle.

                // Calculate distance to axis.
                double distanceToHyperplane = diffAxis * diffAxis;
                // See if line intersects circle
                if (distanceToHyperplane <= currentBest.distance) {
                    // If it does then this point might be the best one.
                    double diffOther = queryOther - otherValue;
                    double d = distanceToHyperplane + diffOther * diffOther;
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
                    if (bigger != null) {
                        currentBest = bigger.findNearest(queryOther, queryAxis, currentBest);
                    }
                }
            }
            return currentBest;
        }

        private void findNearest(double queryAxis, double queryOther, NearestPoint<T> currentBest) {
            // Negative number means this point is on the left to the query point.
            double diffAxis = queryAxis - axisValue;
            if (diffAxis >= 0) {
                // First check the closer side
                if (bigger != null) {
                    bigger.findNearest(queryOther, queryAxis, currentBest);
                }
                // Now let's see it the other side is still relevant. Since that search
                // might have narrowed the circle.

                // Calculate distance to axis.
                double distanceToHyperplane = diffAxis * diffAxis;
                // See if line intersects circle
                if (distanceToHyperplane <= currentBest.distance) {
                    // If it does then this point might be the best one.
                    double diffOther = queryOther - otherValue;
                    double d = distanceToHyperplane + diffOther * diffOther;
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
                double distanceToHyperplane = diffAxis * diffAxis;
                // See if line intersects circle
                if (distanceToHyperplane <= currentBest.distance) {
                    // If it does then this point might be the best one.
                    double diffOther = queryOther - otherValue;
                    double d = distanceToHyperplane + diffOther * diffOther;
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
                double temp = axisValue;
                axisValue = otherValue;
                otherValue = temp;
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
