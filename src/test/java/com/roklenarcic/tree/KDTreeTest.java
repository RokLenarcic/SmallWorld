package com.roklenarcic.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import com.roklenarcic.tree.KDTree.Point;

public class KDTreeTest {

    @Test
    public void speedTestRandom() {
        KDTree<Void> k = new KDTree<Void>(generateRandomPoints(40000, 1000000));
        List<Point<Void>> checkPoints = generateRandomPoints(1000, 1000000);
        long start = System.nanoTime();
        long sum = 0;
        for (int i = 0; i < 100; i++) {
            for (Point<Void> p : checkPoints) {
                sum += k.findNearest(p.coor[0], p.coor[1], Integer.MAX_VALUE).coor[0];
            }
        }
        System.out.println("Sum " + sum + " Time " + (System.nanoTime() - start) / (100 * checkPoints.size()));
    }

    @Test
    public void testRandom() {
        List<Point<Void>> datasetPoints = generateRandomPoints(300, 100000);
        List<Point<Void>> checkPoints = generateRandomPoints(10000, 100000);
        KDTree<Void> k = new KDTree<Void>(datasetPoints);
        for (Point<Void> p : checkPoints) {
            confirm(p.coor[0], p.coor[1], k.findNearest(p.coor[0], p.coor[1], Integer.MAX_VALUE), datasetPoints, Integer.MAX_VALUE);
        }
    }

    @Test
    public void testSmallMaxDistance() {
        List<Point<Void>> datasetPoints = generateRandomPoints(300, 100000);
        List<Point<Void>> checkPoints = generateRandomPoints(10, 100000);
        KDTree<Void> k = new KDTree<Void>(datasetPoints);
        for (Point<Void> p : checkPoints) {
            confirm(p.coor[0], p.coor[1], k.findNearest(p.coor[0], p.coor[1], 0), datasetPoints, 0);
        }
    }

    private void confirm(int x, int y, Point<Void> calculatedPoint, List<Point<Void>> datasetPoints, int maxDistance) {
        long minDist = ((long) maxDistance) * ((long) maxDistance);
        Point<Void> minPoint = null;
        for (Point<Void> p : datasetPoints) {
            long dx = p.coor[0] - x;
            long dy = p.coor[1] - y;
            long dist = dx * dx + dy * dy;
            if (minDist > dist) {
                minDist = dist;
                minPoint = p;
            }
        }
        if (calculatedPoint != null) {
            long dx = calculatedPoint.coor[0] - x;
            long dy = calculatedPoint.coor[1] - y;
            long calculatedDist = dx * dx + dy * dy;
            Assert.assertTrue("Point " + calculatedPoint + " is not closest, " + minPoint + " is for coordinate " + x + " " + y, minDist == calculatedDist);
        } else {
            Assert.assertTrue("Point " + calculatedPoint + " should be null", minPoint == null);
        }
    }

    private List<Point<Void>> generateRandomPoints(int number, int range) {
        Random r = new Random();
        List<Point<Void>> l = new ArrayList<KDTree.Point<Void>>();
        for (int i = 0; i < number; i++) {
            l.add(new Point<Void>(new int[] { r.nextInt(range), r.nextInt(range) }, null));
        }
        return l;
    }
}
