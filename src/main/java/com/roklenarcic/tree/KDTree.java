package com.roklenarcic.tree;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class KDTree<T> {

    private static long distance(Point<?> p, int[] query) {
        long dx = p.coor[0] - query[0];
        long dy = p.coor[1] - query[1];
        return dx * dx + dy * dy;
    }

    @SuppressWarnings("unchecked")
    private final Comparator<Point<T>>[] comparators = (Comparator<Point<T>>[]) new Comparator<?>[] { new Comparator<KDTree.Point<T>>() {

        public int compare(Point<T> o1, Point<T> o2) {
            return o1.coor[0] - o2.coor[0];
        }

    }, new Comparator<KDTree.Point<T>>() {

        public int compare(Point<T> o1, Point<T> o2) {
            return o1.coor[1] - o2.coor[1];
        }

    } };

    private Point<T> root;

    public KDTree(List<Point<T>> points) {
        root = buildSubtree(points, 0);
    }

    public Point<T> findNearest(int x, int y, int maxDistance) {
        NearestPoint<T> nearest = new NearestPoint<T>();
        int[] query = { x, y };
        long md = maxDistance;
        nearest.distance = md * md;
        return root.findNearest(query, nearest, 0).p;
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
            int pivotValue = points.get(pivotIdx).coor[axis];
            while (--pivotIdx > 0) {
                if (points.get(pivotIdx).coor[axis] != pivotValue) {
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

    public static class NearestPoint<T> {

        long distance;
        Point<T> p;

    }

    public static class Point<T> {

        Point<T> bigger;

        int[] coor;

        Point<T> smaller;
        T value;

        public Point(int[] coor, T value) {
            super();
            this.coor = coor;
            this.value = value;
        }

        @Override
        public String toString() {
            return "x " + coor[0] + " y " + coor[1];
        }

        private NearestPoint<T> findNearest(int[] query, NearestPoint<T> currentBest, int axis) {
            // Negative number means this point is on the left to the query point.
            int diff = query[axis] - this.coor[axis];
            // First check the closer side
            if (diff >= 0) {
                if (bigger != null) {
                    bigger.findNearest(query, currentBest, (axis + 1) % 2);
                }
                // Now let's see it the other side is still relevant.
                long distanceToHyperplane = diff * diff;
                // See if line intersects circle
                if (distanceToHyperplane <= currentBest.distance) {
                    // If it does then this point might be the best one
                    long d = KDTree.distance(this, query);
                    if (d < currentBest.distance) {
                        currentBest.p = this;
                        currentBest.distance = d;
                    }
                    if (smaller != null) {
                        smaller.findNearest(query, currentBest, (axis + 1) % 2);
                    }
                }
            } else {
                if (smaller != null) {
                    smaller.findNearest(query, currentBest, (axis + 1) % 2);
                }
                // Now let's see it the other side is still relevant.
                long distanceToHyperplane = diff * diff;
                // See if line intersects circle
                if (distanceToHyperplane <= currentBest.distance) {
                    // If it does then this point might be the best one
                    long d = KDTree.distance(this, query);
                    if (d < currentBest.distance) {
                        currentBest.p = this;
                        currentBest.distance = d;
                    }
                    if (bigger != null) {
                        bigger.findNearest(query, currentBest, (axis + 1) % 2);
                    }
                }
            }
            return currentBest;
        }
    }
}
