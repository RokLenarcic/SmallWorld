package com.roklenarcic.tree;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class KDTree<T> {

    private static long distance(int x1, int y1, int x2, int y2) {
        long dx = x1 - x2;
        long dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    @SuppressWarnings("unchecked")
    private final Comparator<Point<T>>[] comparators = (Comparator<Point<T>>[]) new Comparator<?>[] { new Comparator<KDTree.Point<T>>() {

        public int compare(Point<T> o1, Point<T> o2) {
            return o1.axisValue - o2.axisValue;
        }

    }, new Comparator<KDTree.Point<T>>() {

        public int compare(Point<T> o1, Point<T> o2) {
            return o1.otherValue - o2.otherValue;
        }

    } };

    private Point<T> root;

    public KDTree(List<Point<T>> points) {
        root = buildSubtree(points, 0);
        root.skipFlip();
    }

    public Point<T> findNearest(int x, int y, int maxDistance) {
        NearestPoint<T> nearest = new NearestPoint<T>();
        long md = maxDistance;
        nearest.distance = md * md;
        root.findNearest(x, y, nearest);
        return nearest.p;
    }

    @Override
    public String toString() {
        return root.toString();
    }

    private Point<T> buildSubtree(List<Point<T>> points, int axis) {
        if (points.size() == 1) {
            return points.get(0);
        } else if (points.size() == 0) {
            return null;
        }
        Collections.sort(points, comparators[axis]);
        int pivotIdx = points.size() / 2;
        {
            int pivotValue = axis == 0 ? points.get(pivotIdx).axisValue : points.get(pivotIdx).otherValue;
            while (--pivotIdx > 0) {
                if ((axis == 0 ? points.get(pivotIdx).axisValue : points.get(pivotIdx).otherValue) != pivotValue) {
                    pivotIdx++;
                    break;
                }
            }
        }
        axis = (axis + 1) % 2;
        Point<T> p = points.get(pivotIdx);
        p.smaller = buildSubtree(points.subList(0, pivotIdx), axis);
        p.bigger = buildSubtree(points.subList(pivotIdx + 1, points.size()), axis);
        return p;
    }

    public static class Point<T> {

        int axisValue;
        int otherValue;

        Point<T> smaller, bigger;

        T value;

        public Point(int x, int y, T value) {
            super();
            this.axisValue = x;
            this.otherValue = y;
            this.value = value;
        }

        @Override
        public String toString() {
            return "Axis " + axisValue + " other " + otherValue;
        }

        private void findNearest(int queryAxis, int queryOther, NearestPoint<T> currentBest) {
            // Negative number means this point is on the left to the query point.
            int diff = queryAxis - axisValue;
            // First check the closer side
            if (diff >= 0) {
                if (bigger != null) {
                    bigger.findNearest(queryOther, queryAxis, currentBest);
                }
                // Now let's see it the other side is still relevant.
                long distanceToHyperplane = diff * diff;
                // See if line intersects circle
                if (distanceToHyperplane <= currentBest.distance) {
                    // If it does then this point might be the best one
                    long d = KDTree.distance(queryAxis, queryOther, axisValue, otherValue);
                    if (d < currentBest.distance) {
                        currentBest.p = this;
                        currentBest.distance = d;
                    }
                    if (smaller != null) {
                        smaller.findNearest(queryOther, queryAxis, currentBest);
                    }
                }
            } else {
                if (smaller != null) {
                    smaller.findNearest(queryOther, queryAxis, currentBest);
                }
                // Now let's see it the other side is still relevant.
                long distanceToHyperplane = diff * diff;
                // See if line intersects circle
                if (distanceToHyperplane <= currentBest.distance) {
                    // If it does then this point might be the best one
                    long d = KDTree.distance(queryAxis, queryOther, axisValue, otherValue);
                    if (d < currentBest.distance) {
                        currentBest.p = this;
                        currentBest.distance = d;
                    }
                    if (bigger != null) {
                        bigger.findNearest(queryOther, queryAxis, currentBest);
                    }
                }
            }
        }

        private void flip() {
            int temp = axisValue;
            axisValue = otherValue;
            otherValue = temp;
            if (smaller != null) {
                smaller.skipFlip();
            }
            if (bigger != null) {
                bigger.skipFlip();
            }
        }

        private void skipFlip() {
            if (smaller != null) {
                smaller.flip();
            }
            if (bigger != null) {
                bigger.flip();
            }
        }

    }

    private static class NearestPoint<T> {
        long distance;
        Point<T> p;
    }
}
