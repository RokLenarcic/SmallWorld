# Small World

A small and fast library for finding nearest neighbor in a small data-set using a kd-tree.

Java 5 compatible.

## Maven dependency

```
    <dependency>
        <groupId>com.github.roklenarcic</groupId>
        <artifactId>small-world</artifactId>
        <version>1.0</version>
    </dependency>
```

## Sample use

```java
import com.roklenarcic.tree.KDTreeInt
import com.roklenarcic.tree.KDTreeInt.Point

List<Point<MyData>> points = ...construct data set

// Parameters are points, minimum X coord, minimum Y coord, maximum X coord, maximum Y coord, all inclusive.
KDTreeInt<MyData> tree = new KDTreeInt<MyData>(points, -180, -90, 180, 90);

Point<MyData> closestPoint = tree.findNearest(4, 5, Integer.MAX_VALUE);

// Find nearest point with X axis wrapping.
Point<MyData> closestPointWithWrapping = tree.findNearestWithWrapping(4, 5, Integer.MAX_VALUE);
```

## KDTreeInt and KDTreeDouble

There's two flavors of tree offered. `KDTreeInt` with `int` coordinates and `KDTreeDouble` with `double` coordinates.

Geographical data usually has coordinates in double format, but arguably you never need more than 5 decimals.
So one can easily use the `int` tree by simply multiplying and truncating:

`(int)(54.34356 * 100000)`

The `int` tree has an advantage that every number and distance is representable and thus correctness is guaranteed.
To achieve that the range on `int` tree coordinates is limited to [-590000000, 590000000] range (inclusive) or IllegalArgumentException will be thrown in constructor.

The `int` tree is about 20% faster than `double` tree on normal desktop CPUs and it takes less memory.

The `double` tree allows a larger range of [-3.7E153..3.7E153], but it has a few disadvantages.
Some values are not exactly representable (e.g. 10.1, 3.3 etc) so the calculations might be slightly inaccurate.
Points that are extremely close together might cause an underflow when calculating distance:

e.g. `(10.00000000000000001 - 10.00000000000000002) ^ 2 = 0.0000000000000000000.....`

It might also be slower on platforms with slow floating point operations.

## KDTreeSpherical

This variant uses a 3-D tree which models the world as a sphere. This solves problems with wrapping in 2 dimensions and with distance distortion, which is present in 2-D trees.

But it is slower by about 50%. The azimuth value should be between [-180,180] and inclination should be [-90,90].

## How fast is this?

Fast enough. Single-threaded micro benchmark on Core i5 with 40k random points shows that 2-D trees need 400-500 (`int`) 500-600 (`double`) nanoseconds per lookup, 15% slower if using wrapping.
3-D spherical tree needs 1100-1200 nanoseconds per lookup.
