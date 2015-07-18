package com.roklenarcic.tree;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class KDTreeSpherical<T> {

    private static final double DEGREES_IN_RADIAN = 57.29577951308233;

    @SuppressWarnings("unchecked")
    private final Comparator<Point<T>>[] comparators = (Comparator<Point<T>>[]) new Comparator<?>[] { Point.createComparator(0), Point.createComparator(1),
            Point.createComparator(2) };

    private double maxDistance;
    private final Point<T> root;

    // Build a tree from list of points.
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

    public Point<T> findNearest(double azimuth, double inclination) {
        NearestPoint<T> nearest = new NearestPoint<T>();
        if (root != null) {
            nearest.distance = this.maxDistance;
            // Calculate those cartesian coordinates
            azimuth = (azimuth + 180) / DEGREES_IN_RADIAN;
            inclination = (-inclination + 90) / DEGREES_IN_RADIAN;
            double sinAzimuth = Math.sin(azimuth);
            double cosAzimuth = Math.cos(azimuth);
            double sinInclination = Math.sin(inclination);
            double cosInclination = Math.cos(inclination);
            root.findNearest(sinInclination * cosAzimuth, sinInclination * sinAzimuth, cosInclination, nearest);
        }
        return nearest.p;
    }

    public Point<T> findNearest(Point<T> p) {
        NearestPoint<T> nearest = new NearestPoint<T>();
        if (root != null) {
            nearest.distance = Double.MAX_VALUE;
            // Calculate those cartesian coordinates
            root.findNearest(p.axisValue, p.otherValue, p.otherValue2, nearest);
        }
        return nearest.p;
    }

    private Point<T> buildTree(List<Point<T>> points, int axis) {
        if (points.size() == 1) {
            Point<T> p = points.get(0);
            p.rotate(axis);
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
            p.rotate(axis);
            axis = (axis + 1) % 3;
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
        private final double azimuth, inclination;
        private double otherValue;

        private double otherValue2;

        private Point<T> smaller, bigger;

        private final T value;

        public Point(double azimuth, double inclination, T value) {
            this.azimuth = azimuth;
            this.inclination = inclination;
            if (azimuth > 180 || azimuth < -180 || inclination > 90 || inclination < -90) {
                throw new IllegalArgumentException("Point " + this + " has azimuth outside [-180, 180] or inclination [-90, 90].");
            }
            // Save the coordinates
            // Now translate them
            // 90 is 0 inclination, -90 is 180
            // -180 is 0 azimuth (different than earth, but we don't care)
            // then into the radians.
            azimuth = (azimuth + 180) / DEGREES_IN_RADIAN;
            inclination = (-inclination + 90) / DEGREES_IN_RADIAN;
            double sinAzimuth = Math.sin(azimuth);
            double cosAzimuth = Math.cos(azimuth);
            double sinInclination = Math.sin(inclination);
            double cosInclination = Math.cos(inclination);
            this.axisValue = sinInclination * cosAzimuth;
            this.otherValue = sinInclination * sinAzimuth;
            this.otherValue2 = cosInclination;
            this.value = value;
        }

        public double getAzimuth() {
            return azimuth;
        }

        public double getInclination() {
            return inclination;
        }

        public T getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "Azimuth=" + azimuth + ", Inclination=" + inclination;
        }

        private void findNearest(double queryAxis, double queryOther, double queryOther2, NearestPoint<T> currentBest) {
            // Negative number means this point is on the left to the query point.
            double diffAxis = queryAxis - axisValue;
            if (diffAxis >= 0) {
                // First check the closer side
                if (bigger != null) {
                    bigger.findNearest(queryOther, queryOther2, queryAxis, currentBest);
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
                    if (smaller != null) {
                        smaller.findNearest(queryOther, queryOther2, queryAxis, currentBest);
                    }
                }
            } else {
                // First check the closer side
                if (smaller != null) {
                    smaller.findNearest(queryOther, queryOther2, queryAxis, currentBest);
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
                    if (bigger != null) {
                        bigger.findNearest(queryOther, queryOther2, queryAxis, currentBest);
                    }
                }
            }
        }

        private double getCoordinate(int axis) {
            return axis == 0 ? axisValue : (axis == 1 ? otherValue : otherValue2);
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

    private static class NearestPoint<T> {
        private double distance;
        private Point<T> p;
    }
}
