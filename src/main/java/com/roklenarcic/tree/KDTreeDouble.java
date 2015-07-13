package com.roklenarcic.tree;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class KDTreeDouble<T> {

    private static final double MAX_COORD_VAL = Math.sqrt(Double.MAX_VALUE);

    @SuppressWarnings("unchecked")
    private final Comparator<Point<T>>[] comparators = (Comparator<Point<T>>[]) new Comparator<?>[] { new Comparator<KDTreeDouble.Point<T>>() {

        public int compare(Point<T> o1, Point<T> o2) {
            double d = o1.x - o2.x;
            if (d > 0) {
                return 1;
            } else if (d < 0) {
                return -1;
            } else {
                return 0;
            }
        }

    }, new Comparator<KDTreeDouble.Point<T>>() {

        public int compare(Point<T> o1, Point<T> o2) {
            double d = o1.y - o2.y;
            if (d > 0) {
                return 1;
            } else if (d < 0) {
                return -1;
            } else {
                return 0;
            }
        }

    } };

    private final Point<T> root;

    // Build a tree from list of points.
    // Point coordinates are limited to [-10^9...10^9]
    public KDTreeDouble(List<Point<T>> points) {
        root = buildTree(points, 0);
        if (root != null) {
            // After building the tree, correctly order x and y into axisValue, otherValue
            root.skipFlip();
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

    // Search and wrap on x axis. The borders are inclusive e.g. -180, 180 means that both those
    // coordinates are valid.
    public Point<T> findNearest(double x, double y, double maxDistance, double wrapLeft, double wrapRight) {
        NearestPoint<T> nearest = new NearestPoint<T>();
        if (root != null) {
            double md = maxDistance;
            nearest.distance = md * md;
            root.findNearest(x, y, nearest);
            if (nearest.distance > 0) {
                // No point found within max distance, see if point + max distance crosses the wrap
                // It can only wrap around the nearest border.
                if (wrapLeft + wrapRight > x / 2) {
                    double distanceToLeftBorder = x - wrapLeft;
                    if (distanceToLeftBorder * distanceToLeftBorder < nearest.distance) {
                        root.findNearest(wrapRight + distanceToLeftBorder + 1, y, nearest);
                    }
                } else {
                    double distanceToRightBorder = wrapRight - x;
                    if (distanceToRightBorder * distanceToRightBorder < nearest.distance) {
                        root.findNearest(wrapLeft - distanceToRightBorder - 1, y, nearest);
                    }
                }
            }
        }
        return nearest.p;
    }

    private Point<T> buildTree(List<Point<T>> points, int axis) {
        if (points.size() == 1) {
            // Point coordinates are limited to [-10^9...10^9]
            Point<T> p = points.get(0);
            if (p.x > MAX_COORD_VAL || p.x < -MAX_COORD_VAL || p.y > MAX_COORD_VAL || p.y < -MAX_COORD_VAL) {
                throw new IllegalArgumentException("Point " + p + " has coordinates out of [-" + MAX_COORD_VAL + "..." + MAX_COORD_VAL + "] interval.");
            }
            return p;
        } else if (points.size() == 0) {
            return null;
        } else {
            // Sort by axis.
            Collections.sort(points, comparators[axis]);
            int pivotIdx = points.size() >> 1;
            double pivotValue = points.get(pivotIdx).getCoordinate(axis);
            // Find first point with the same axis value.
            while (--pivotIdx >= 0 && points.get(pivotIdx).getCoordinate(axis) == pivotValue) {
            }
            pivotIdx++;
            Point<T> p = points.get(pivotIdx);
            if (p.x > MAX_COORD_VAL || p.x < -MAX_COORD_VAL || p.y > MAX_COORD_VAL || p.y < -MAX_COORD_VAL) {
                throw new IllegalArgumentException("Point " + p + " has coordinates out of [-" + MAX_COORD_VAL + "..." + MAX_COORD_VAL + "] interval.");
            }
            axis = axis ^ 1;
            // Build subtree. Bigger branch also contains points that has equal axis value to the pivot.
            p.smaller = buildTree(points.subList(0, pivotIdx), axis);
            p.bigger = buildTree(points.subList(pivotIdx + 1, points.size()), axis);
            return p;
        }
    }

    public static class Point<T> {

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

        private void flip() {
            double temp = axisValue;
            axisValue = otherValue;
            otherValue = temp;
            if (smaller != null) {
                smaller.skipFlip();
            }
            if (bigger != null) {
                bigger.skipFlip();
            }
        }

        private double getCoordinate(int axis) {
            return axis == 0 ? x : y;
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
        private double distance;
        private Point<T> p;
    }
}
