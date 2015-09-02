package com.roklenarcic.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import com.roklenarcic.tree.KDTreeInt.Point;

public class KDTreeIntTest {

    @Test
    public void speedTestMultipleMatches() {
        KDTreeInt<Void> k = new KDTreeInt<Void>(generateRandomPoints(40000, 1000000), 0, 0, 1000000, 1000000);
        List<Point<Void>> checkPoints = generateRandomPoints(1000, 1000000);
        long start = System.nanoTime();
        long sum = 0;
        for (int i = 0; i < 100; i++) {
            for (Point<Void> p : checkPoints) {
                sum += k.findNearest(p.getX(), p.getY(), Integer.MAX_VALUE, 5).hashCode();
            }
        }
        System.out.println("Sum " + sum + " Time " + (System.nanoTime() - start) / (100 * checkPoints.size()) + " for 5 matches.");
    }

    @Test
    public void speedTestRandom() {
        KDTreeInt<Void> k = new KDTreeInt<Void>(generateRandomPoints(40000, 1000000), 0, 0, 1000000, 1000000);
        List<Point<Void>> checkPoints = generateRandomPoints(1000, 1000000);
        long start = System.nanoTime();
        long sum = 0;
        for (int i = 0; i < 100; i++) {
            for (Point<Void> p : checkPoints) {
                sum += k.findNearest(p.getX(), p.getY(), Integer.MAX_VALUE).getX();
            }
        }
        System.out.println("Sum " + sum + " Time " + (System.nanoTime() - start) / (100 * checkPoints.size()));
    }

    @Test
    public void speedTestRandomWrapping() {
        KDTreeInt<Void> k = new KDTreeInt<Void>(generateRandomPoints(40000, 1000000), 0, 0, 1000000, 1000000);
        List<Point<Void>> checkPoints = generateRandomPoints(1000, 1000000);
        long start = System.nanoTime();
        long sum = 0;
        for (int i = 0; i < 100; i++) {
            for (Point<Void> p : checkPoints) {
                sum += k.findNearest(p.getX(), p.getY(), Integer.MAX_VALUE).getX();
            }
        }
        System.out.println("Sum " + sum + " Time with wrapping " + (System.nanoTime() - start) / (100 * checkPoints.size()));
    }

    @Test
    public void testBalancing() {
        List<Point<Void>> datasetPoints = generateRandomPoints1D(300, 100000);
        List<Point<Void>> checkPoints = generateRandomPoints1D(10000, 100000);
        KDTreeInt<Void> k = new KDTreeInt<Void>(datasetPoints, 0, 0, 100000, 100000);
        for (Point<Void> p : checkPoints) {
            confirm(p.getX(), p.getY(), k.findNearest(p.getX(), p.getY(), Integer.MAX_VALUE), datasetPoints, Integer.MAX_VALUE);
        }
    }

    @Test
    public void testRandom() {
        List<Point<Void>> datasetPoints = generateRandomPoints(300, 100000);
        List<Point<Void>> checkPoints = generateRandomPoints(10000, 100000);
        KDTreeInt<Void> k = new KDTreeInt<Void>(datasetPoints, 0, 0, 100000, 100000);
        for (Point<Void> p : checkPoints) {
            confirm(p.getX(), p.getY(), k.findNearest(p.getX(), p.getY(), Integer.MAX_VALUE), datasetPoints, Integer.MAX_VALUE);
        }
    }

    @Test
    public void testSmallMaxDistance() {
        List<Point<Void>> datasetPoints = generateRandomPoints(300, 100000);
        KDTreeInt<Void> k = new KDTreeInt<Void>(datasetPoints, 0, 0, 100000, 100000);
        for (Point<Void> p : datasetPoints) {
            Assert.assertTrue("Point " + p + "doesn't resolve to itself", k.findNearest(p.getX(), p.getY(), 0) == p);
            confirm(p.getX(), p.getY(), k.findNearest(p.getX(), p.getY(), 0), datasetPoints, 0);
        }
    }

    @Test
    public void testWrapping() {
        List<Point<Void>> datasetPoints = new ArrayList<Point<Void>>();
        datasetPoints.add(new Point<Void>(1000000, 0, null));
        KDTreeInt<Void> k = new KDTreeInt<Void>(datasetPoints, 0, 0, 1000000, 1000000);
        Point<Void> p = k.findNearestWithWrapping(1, 0, 2);
        Assert.assertEquals(1000000, p.getX());
        p = k.findNearestWithWrapping(1, 0, 1);
        Assert.assertEquals(null, p);
    }

    @Test
    public void testWrappingWithMultipleMatches() {
        List<Point<Void>> datasetPoints = new ArrayList<Point<Void>>();
        datasetPoints.add(new Point<Void>(1000000, 0, null));
        datasetPoints.add(new Point<Void>(999999, 0, null));
        datasetPoints.add(new Point<Void>(3, 0, null));
        KDTreeInt<Void> k = new KDTreeInt<Void>(datasetPoints, 0, 0, 1000000, 1000000);
        Iterator<Point<Void>> iter = k.findNearestWithWrapping(1, 0, 3, 3).iterator();
        Assert.assertEquals(1000000, iter.next().getX());
        Assert.assertEquals(3, iter.next().getX());
        Assert.assertEquals(999999, iter.next().getX());
        Assert.assertEquals(false, iter.hasNext());
        Assert.assertEquals(Collections.EMPTY_LIST, k.findNearestWithWrapping(1, 0, 1, 3));
    }

    private void confirm(int x, int y, Point<Void> calculatedPoint, List<Point<Void>> datasetPoints, int maxDistance) {
        Point<Void> minPoint = getClosest(x, y, datasetPoints, maxDistance);
        if (calculatedPoint != null) {
            long dx = calculatedPoint.getX() - x;
            long dy = calculatedPoint.getY() - y;
            long calculatedDist = dx * dx + dy * dy;
            dx = minPoint.getX() - x;
            dy = minPoint.getY() - y;
            long minDist = dx * dx + dy * dy;
            Assert.assertTrue("Point " + calculatedPoint + " is not closest(" + calculatedDist + "), " + minPoint + "(" + minDist + ") is for coordinate " + x
                    + " " + y, minDist == calculatedDist);
        } else {
            Assert.assertTrue("Point is null should be " + minPoint, minPoint == null);
        }
    }

    private List<Point<Void>> generateRandomPoints(int number, int range) {
        Random r = new Random();
        List<Point<Void>> l = new ArrayList<KDTreeInt.Point<Void>>();
        for (int i = 0; i < number; i++) {
            l.add(new Point<Void>(r.nextInt(range), r.nextInt(range), null));
        }
        return l;
    }

    private List<Point<Void>> generateRandomPoints1D(int number, int range) {
        Random r = new Random();
        List<Point<Void>> l = new ArrayList<KDTreeInt.Point<Void>>();
        for (int i = 0; i < number; i++) {
            l.add(new Point<Void>(r.nextInt(range), 0, null));
        }
        return l;
    }

    private Point<Void> getClosest(int x, int y, List<Point<Void>> datasetPoints, int maxDistance) {
        long minDist = ((long) maxDistance) * ((long) maxDistance);
        Point<Void> minPoint = null;
        for (Point<Void> p : datasetPoints) {
            long dx = p.getX() - x;
            long dy = p.getY() - y;
            long dist = dx * dx + dy * dy;
            if (minDist >= dist) {
                minDist = dist;
                minPoint = p;
            }
        }
        return minPoint;
    }
}
