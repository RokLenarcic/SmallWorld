package com.roklenarcic.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import com.roklenarcic.tree.KDTreeSpherical.Point;

public class KDTreeSphericalTest {

    @Test
    public void speedTestRandom() {
        KDTreeSpherical<Void> k = new KDTreeSpherical<Void>(generateRandomPoints(40000), 180);
        List<Point<Void>> checkPoints = generateRandomPoints(1000);
        long start = System.nanoTime();
        long sum = 0;
        for (int i = 0; i < 100; i++) {
            for (Point<Void> p : checkPoints) {
                sum += k.findNearest(p.getAzimuth(), p.getInclination()).getAzimuth();
            }
        }
        System.out.println("Sum " + sum + " Time " + (System.nanoTime() - start) / (100 * checkPoints.size()));
    }

    @Test
    public void testRandom() {
        List<Point<Void>> datasetPoints = generateRandomPoints(300);
        List<Point<Void>> checkPoints = generateRandomPoints(10000);
        KDTreeSpherical<Void> k = new KDTreeSpherical<Void>(datasetPoints, 180);
        for (Point<Void> p : checkPoints) {
            confirm(p.getAzimuth(), p.getInclination(), k.findNearest(p.getAzimuth(), p.getInclination()), datasetPoints, Integer.MAX_VALUE);
        }
    }

    @Test
    public void testSmallMaxDistance() {
        List<Point<Void>> datasetPoints = generateRandomPoints(300);
        KDTreeSpherical<Void> k = new KDTreeSpherical<Void>(datasetPoints, 0);
        for (Point<Void> p : datasetPoints) {
            Assert.assertTrue("Point " + p + " doesn't resolve to itself but " + k.findNearest(p.getAzimuth(), p.getInclination()),
                    k.findNearest(p.getAzimuth(), p.getInclination()) == p);
            confirm(p.getAzimuth(), p.getInclination(), k.findNearest(p.getAzimuth(), p.getInclination()), datasetPoints, 0);
        }
    }

    private void confirm(double x, double y, Point<Void> calculatedPoint, List<Point<Void>> datasetPoints, double maxDistance) {
        Point<Void> minPoint = getClosest(x, y, datasetPoints, maxDistance);
        if (calculatedPoint != null) {
            double dy = (calculatedPoint.getInclination() - y) / 57.29578;
            double dx = (calculatedPoint.getAzimuth() - x) / 57.29578;
            double a = Math.pow(Math.sin(dy / 2), 2) + Math.cos(calculatedPoint.getInclination() / 57.29578) * Math.cos(y / 57.29578)
                    * Math.pow(Math.sin(dx / 2), 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double dist = c;
            double calculatedDist = dist * dist;
            dy = (minPoint.getInclination() - y) / 57.29578;
            dx = (minPoint.getAzimuth() - x) / 57.29578;
            a = Math.pow(Math.sin(dy / 2), 2) + Math.cos(calculatedPoint.getInclination() / 57.29578) * Math.cos(y / 57.29578) * Math.pow(Math.sin(dx / 2), 2);
            c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            dist = c;
            double minDist = dist * dist;
            Assert.assertTrue("Point " + calculatedPoint + " is not closest(" + calculatedDist + "), " + minPoint + "(" + minDist + ") is for coordinate " + x
                    + " " + y, minDist == calculatedDist);
        } else {
            Assert.assertTrue("Point is null should be " + minPoint, minPoint == null);
        }
    }

    private List<Point<Void>> generateRandomPoints(int number) {
        Random r = new Random();
        List<Point<Void>> l = new ArrayList<KDTreeSpherical.Point<Void>>();
        for (int i = 0; i < number; i++) {
            l.add(new Point<Void>(r.nextDouble() * 360 - 180, r.nextDouble() * 180 - 90, null));
        }
        return l;
    }

    private Point<Void> getClosest(double x, double y, List<Point<Void>> datasetPoints, double maxDistance) {
        double minDist = maxDistance;
        Point<Void> minPoint = null;
        for (Point<Void> p : datasetPoints) {
            // Check by comparing great circle distance.
            double dy = (p.getInclination() - y) / 57.29578;
            double dx = (p.getAzimuth() - x) / 57.29578;
            double a = Math.pow(Math.sin(dy / 2), 2) + Math.cos(p.getInclination() / 57.29578) * Math.cos(y / 57.29578) * Math.pow(Math.sin(dx / 2), 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double dist = c;
            if (minDist >= dist) {
                minDist = dist;
                minPoint = p;
            }
        }
        return minPoint;
    }
}
