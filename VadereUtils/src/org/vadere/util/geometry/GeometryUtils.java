package org.vadere.util.geometry;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.Random;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.math.MathUtil;

import static org.vadere.util.geometry.GeometryUtils.Orientation.CCW;
import static org.vadere.util.geometry.GeometryUtils.Orientation.COLLINEAR;
import static org.vadere.util.geometry.GeometryUtils.Orientation.CW;

public class GeometryUtils {

	enum Orientation {
		CCW,
		CW,
		COLLINEAR;
	}

	/**
	 * Constant for comparison of double values. Everything below this is
	 * considered equal.
	 */
	public static final double DOUBLE_EPS = 1e-8;

	public static final Logger log = LogManager.getLogger(GeometryUtils.class);

	/**
	 * Interpolates between start and end with the given factor.
	 */
	public static VPoint interpolate(VPoint start, VPoint end, double factor) {
		VPoint result = new VPoint(start.x + factor * (end.x - start.x),
				start.y + factor * (end.y - start.y));
		return result;
	}

	public static double derterminant2D(double x1, double y1, double x2, double y2) {
		return x1 * y2 - y1 * x2;
	}

	//http://mathworld.wolfram.com/Line-LineIntersection.html
	public static VPoint intersectionPoint(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
		double a = derterminant2D(x1, x2, y1, y2);
		double b = derterminant2D(x3, x4, y3, y4);
		double c = derterminant2D(x1-x2, x3 - x4, y1 - y2, y3 - y4);

		double x = derterminant2D(a, b, x1 - x2, x3 - x4) / c;
		double y = derterminant2D(a, b, y1 - y2, y3 - y4) / c;

		return new VPoint(x,y);
	}

	public static VPoint getCentroid(@NotNull final List<? extends IPoint> polygon){
        double area = areaOfPolygon(polygon);
        double xValue = 0;
        double yValue = 0;

        assert polygon.size() > 2;

        int j = 0;
        for (int i = 0; i < polygon.size(); i++) {
        	if(i < polygon.size() - 1) {
        		j = i + 1;
	        }
	        else {
        		j = 0;
	        }

            xValue += (polygon.get(i).getX() + polygon.get(j).getX())
                    * (polygon.get(i).getX() * polygon.get(j).getY()
                    - polygon.get(i).getY() * polygon.get(j).getX());
            yValue += (polygon.get(i).getY() + polygon.get(j).getY())
                    * (polygon.get(i).getX() * polygon.get(j).getY()
                    - polygon.get(i).getY() * polygon.get(j).getX());
        }
        xValue /= (6 * area);
        yValue /= (6 * area);

        if(xValue == Double.NaN || yValue == Double.NaN || area == 0 || area == Double.NaN) {
        	throw new IllegalArgumentException("invalid point list");
        }

        return new VPoint(xValue, yValue);
    }

	public static boolean collectionContains(
			Collection<? extends VShape> collection, VPoint point) {
		for (VShape shape : collection) {
			if (shape.contains(point)) {
				return true;
			}
		}
		return false;
	}

	public static VPoint getCircumcenter(final IPoint p1, final IPoint p2, final IPoint p3) {
		double d = 2 * (p1.getX() * (p2.getY() - p3.getY()) + p2.getX() * (p3.getY() - p1.getY()) + p3.getX() * (p1.getY() - p2.getY()));
		double x = ((p1.getX() * p1.getX() + p1.getY() * p1.getY()) * (p2.getY() - p3.getY())
				+ (p2.getX() * p2.getX() + p2.getY() * p2.getY()) * (p3.getY() - p1.getY())
				+ (p3.getX() * p3.getX() + p3.getY() * p3.getY()) * (p1.getY() - p2.getY())) / d;
		double y = ((p1.getX() * p1.getX() + p1.getY() * p1.getY()) * (p3.getX() - p2.getX())
				+ (p2.getX() * p2.getX() + p2.getY() * p2.getY()) * (p1.getX() - p3.getX())
				+ (p3.getX() * p3.getX() + p3.getY() * p3.getY()) * (p2.getX() - p1.getX())) / d;

		return new VPoint(x,y);
	}

	public static boolean isInCircumscribedCycle(final IPoint p1, final IPoint p2, final IPoint p3, final IPoint point) {
		VPoint circumcenter = getCircumcenter(p1, p2, p3);
		return circumcenter.distance(point) < circumcenter.distance(p1);
	}

	/**
	 * Generates a set of points which are positioned inside a disc segment. The points are placed equidistant on one or multiple circles
	 * with the center at the center of the disc and the radius <= the radius of the disc.
	 *
	 * @param random                            a random number generator which will only be used if varyDirection is true.
	 * @param varyDirection                     if true the generated points will be rotated by a random offset
	 * @param circle                            the circle defining the disc (containing the points)
	 * @param numberOfCircles                   the number of circles
	 * @param numberOfPointsOfLargestCircle     the number of points of the most outer circle
	 * @param anchorAngle                       start angle of the segment
	 * @param angle                             anchorAngle + angle = end angle of the segment
	 * @return a set of points which are positioned inside a disc segment
	 */
	public static List<VPoint> getDiscDiscretizationPoints(
			@NotNull final Random random,
			final boolean varyDirection,
			@NotNull final VCircle circle,
			final int numberOfCircles,
			final int numberOfPointsOfLargestCircle,
			final double anchorAngle,
			final double angle) {
		double randOffset = varyDirection ? random.nextDouble() : 0;

		List<VPoint> reachablePositions = new ArrayList<>();

		// iterate through all circles
		for (int j = 1; j <= numberOfCircles; j++) {

			double circleOfGrid = circle.getRadius() * j / numberOfCircles;

			int numberOfGridPoints = (int) Math.ceil(circleOfGrid / circle.getRadius() * numberOfPointsOfLargestCircle);

			// reduce number of grid points proportional to the constraint of direction
			if (angle < 2 * Math.PI) {
				numberOfGridPoints = (int) Math.ceil(numberOfGridPoints * angle / (2 * Math.PI));
			}

			double angleDelta = angle / numberOfGridPoints;

			// iterate through all angles and compute absolute positions of grid points
			for (int i = 0; i < numberOfGridPoints; i++) {

				double x = circleOfGrid * Math.cos(anchorAngle + angleDelta * (randOffset + i)) + circle.getCenter().getX();
				double y = circleOfGrid * Math.sin(anchorAngle + angleDelta * (randOffset + i)) + circle.getCenter().getY();
				VPoint tmpPos = new VPoint(x, y);

				reachablePositions.add(tmpPos);
			}
		}

		return reachablePositions;
	}

	/**
	 * Computes the point on the line segment that is closest to the given point
	 * point. from:
	 * http://stackoverflow.com/questions/3120357/get-closest-point-to-a-line
	 *
	 * @param point
	 *        the point to which the counterpart should be computed
	 * @param line
	 *        line representing the segment
	 * @return the point on the line that is closest to p
	 */
	public static VPoint closestToSegment(VLine line, IPoint point) {
		if (new VPoint((Point2D.Double) line.getP1()).equals(point)) {
			return new VPoint(line.x1, line.y1);
		}

		VPoint a2p = new VPoint(point.getX() - line.x1, point.getY() - line.y1);
		VPoint a2b = new VPoint(line.x2 - line.x1, line.y2 - line.y1);
		double distAB = a2b.x * a2b.x + a2b.y * a2b.y;
		double a2p_dot_a2b = a2p.x * a2b.x + a2p.y * a2b.y;

		// normalize t to [0,1] to stay on the line segment
		double t = Math.min(1, Math.max(0, a2p_dot_a2b / distAB));
		return new VPoint(line.x1 + a2b.x * t, line.y1 + a2b.y * t);
	}

	/**
	 * Orders a given list angular relative to a given point, starting with
	 * angle 0.
	 *
	 * @param allPoints
	 * @param center
	 * @return an ordered DataPoint list with the angle of the point as data and
	 *         the original index set.
	 */
	public static List<DataPoint> orderByAngle(List<VPoint> allPoints,
											   VPoint center) {
		List<DataPoint> orderedList = new ArrayList<DataPoint>();

		for (int i = 0; i < allPoints.size(); i++) {
			Vector2D p = new Vector2D(allPoints.get(i));
			orderedList.add(new DataPoint(p.x, p.y, GeometryUtils.angleTo(p, center)));
		}
		// sort by angle
		Collections.sort(orderedList, DataPoint.getComparator());

		return orderedList;
	}

	/**
	 * Computes area (it maybe a negative area) of the parallelogram defined by p, q, r.
	 *
	 * @param pX x-coordinate of p
	 * @param pY y-coordinate of p
	 * @param qX x-coordinate of q
	 * @param qY y-coordinate of q
	 * @param rX x-coordinate of r
	 * @param rY y-coordinate of r
	 * @return
	 */
	public static double ccw(final double qX, final double qY, final double pX, final double pY, final double rX, final double rY) {
		return -((qX - pX) * (rY - pY) - (rX - pX) * (qY - pY));
	}

	/**
	 * Computes area (it maybe a negative area) of the parallelogram defined by p, q, r.
	 *
	 * @param pX x-coordinate of p
	 * @param pY y-coordinate of p
	 * @param qX x-coordinate of q
	 * @param qY y-coordinate of q
	 * @param rX x-coordinate of r
	 * @param rY y-coordinate of r
	 * @return
	 */
	public static double ccwRobust(final double qX, final double qY, final double pX, final double pY, final double rX, final double rY) {
		double result = -((qX - pX) * (rY - pY) - (rX - pX) * (qY - pY));
		if(Math.abs(result) <= DOUBLE_EPS) {
			return 0.0;
		}
		else {
			return result;
		}
	}


	/**
	 * Returns true if q is right of the oriented-line defined by (p1, p2).
	 * @param p1
	 * @param p2
	 * @param q
	 * @return true if q is right of the oriented-line defined by (p1, p2), false otherwise
	 */
	public static boolean isRightOf(final IPoint p1, final IPoint p2, final IPoint q) {
		return isRightOf(p1, p2, q.getX(), q.getY());
	}

	/**
	 * Returns true if q is left of the oriented-line defined by (p1, p2).
	 * @param p1
	 * @param p2
	 * @param q
	 * @return true if q is right of the oriented-line defined by (p1, p2), false otherwise
	 */
	public static boolean isLeftOf(final IPoint p1, final IPoint p2, final IPoint q) {
		return isLeftOf(p1, p2, q.getX(), q.getY());
	}

	public static boolean isRightOf(final IPoint p1, final IPoint p2, final double x, final double y) {
		return isCW(p1.getX(), p1.getY(), p2.getX(), p2.getY(), x, y);
	}

	public static boolean isLeftOf(final IPoint p1, final IPoint p2, final double x, final double y) {
		return isCCW(p1.getX(), p1.getY(), p2.getX(), p2.getY(), x, y);
	}

	/**
	 * Returns the angle between the x-axis, p1 and p2.
	 */
	public static double angleTo(VPoint p1, VPoint p2) {
		double atan2 = Math.atan2(p1.y - p2.y, p1.x - p2.x);

		if (atan2 < 0.0) {
			atan2 = Math.PI * 2 + atan2;
		}

		return atan2;
	}

	/**
	 * Calculate the counter clockwise result for the three given points.<br>
	 * ccw(p1,p2,p3) < 0 if p3 is left of Line(p1,p2)<br>
	 * ccw(p1,p2,p3) = 0 if p3 lies on Line(p1,p2)<br>
	 * ccw(p1,p2,p3) > 0 if p3 is right of Line(p1,p2)<br>
	 *
	 * @param p1
	 *        first point
	 * @param p2
	 *        second point
	 * @param p3
	 *        third point
	 * @return ccw(p1 p2 p3)
	 */
	public static double ccw(final IPoint p1, final IPoint p2, final IPoint p3) {
		return ccwRobust(p1.getX(), p1.getY(), p2.getX(), p2.getY(), p3.getX(), p3.getY());
	}

	public static boolean isCCW(final double qX, final double qY, final double pX, final double pY, final double rX, final double rY) {
		return ccwRobust(qX, qY, pX, pY, rX, rY) > 0;
	}

	public static boolean isCCW(final IPoint p1, final IPoint p2, final IPoint p3) {
		return isCCW(p1.getX(), p1.getY(), p2.getX(), p2.getY(), p3.getX(), p3.getY());
	}

	public static boolean isCW(final double qX, final double qY, final double pX, final double pY, final double rX, final double rY) {
		return ccwRobust(qX, qY, pX, pY, rX, rY) < 0;
	}

	public static boolean isCW(final IPoint p1, final IPoint p2, final IPoint p3) {
		return ccw(p1, p2, p3) < 0;
	}

	public static Orientation orientation(final IPoint p1, final IPoint p2, final IPoint p3) {
		double ccw = ccw(p1, p2, p3);
		if(ccw < 0) {
			return CW;
		}
		else if(ccw > 0) {
			return CCW;
		}
		else {
			return COLLINEAR;
		}
	}

	/**
	 * Tests if the line-segment (p1, p2) intersects the line defined by p, q.
	 * @param p     point defining the line
	 * @param q     point defining the line
	 * @param p1    point defining the line-segment
	 * @param p2    point defining the line-segment
	 * @return true if the line-segment intersects the line defined, otherwise false.
	 */
	public static boolean intersectLine(final IPoint p, final IPoint q, final IPoint p1, final IPoint p2) {
		double ccw1 = ccw(p, q, p1);
		double ccw2 = ccw(p, q, p2);
		return (ccw1 < 0 && ccw2 > 0) || (ccw1 > 0 && ccw2 < 0);
	}

	public static boolean intersectLine(final double pX, final double pY, final double qX, final double qY, final double p1X, final double p1Y, final double p2X, final double p2Y) {
		double ccw1 = ccw(pX, pY, qX, qY, p1X, p1Y);
		double ccw2 = ccw(pX, pY, qX, qY, p2X, p2Y);
		return (ccw1 < 0 && ccw2 > 0) || (ccw1 > 0 && ccw2 < 0);
	}


	public static VPoint getIncenter(final IPoint p1, final IPoint p2, final IPoint p3) {
		double a = p1.distance(p2);
		double b = p2.distance(p3);
		double c = p3.distance(p1);
		double perimeter = a + b + c;

		VPoint incenter = new VPoint((a * p3.getX() + b * p1.getX() + c * p2.getX()) / perimeter,
				(a * p3.getY() + b * p1.getY() + c * p2.getY()) / perimeter);

		return incenter;
	}

	/**
	 * Tests if the half-line-segment starting at p in the direction (q-p) intersects the line-segment (p1,p2).
	 * @param p     the starting point of the half-line-segment
	 * @param q     the point defining the direction (q-p) of the half-line-segment
	 * @param p1    point defining the line-segment
	 * @param p2    point defining the line-segment
	 * @return true if the line-segment intersects the  half-line-segment defined, otherwise false.
	 */
	public static boolean intersectHalfLineSegment(final IPoint p, final IPoint q, final IPoint p1, final IPoint p2) {
		double ccw1 = ccw(p, q, p1);
		double ccw2 = ccw(p, q, p2);

		if((ccw1 < 0 && ccw2 > 0)) {
			return isCCW(p, p2, p1);
		}
		else if((ccw1 > 0 && ccw2 < 0)) {
			return isCCW(p, p1, p2);
		}
		else {
			return false;
		}
	}

	/**
	 * Tests if the first line-segment (p,q) intersects the second line-segment (p1,p2).
	 * @param p     point defining the first line-segment
	 * @param q     point defining the first line-segment
	 * @param p1    point defining the second line-segment
	 * @param p2    point defining the second line-segment
	 * @return true if the first line-segment intersects the second line-segment, otherwise false.
	 */
	public static boolean intersectLineSegment(final IPoint p, final IPoint q, final IPoint p1, final IPoint p2) {
		return intersectLine(p, q, p1, p2) && intersectLine(p1, p2, p, q);
	}

	/**
	 * Tests if the triangle (p1,p2,p3) contains the point r.
	 *
	 * @param p1    point of the triangle
	 * @param p2    point of the triangle
	 * @param p3    point of the triangle
	 * @param r     point which the triangle might contain.
	 * @return true if the triangle (p1,p2,p3) contains the point r, otherwise false.
	 */
	public static boolean triangleContains(final IPoint p1, final IPoint p2, final IPoint p3, final IPoint r) {
		boolean b1, b2, b3;
		double d1 = GeometryUtils.ccw(r, p1, p2);
		double d2 = GeometryUtils.ccw(r, p2, p3);
		double d3 = GeometryUtils.ccw(r, p3, p1);
		b1 = d1 < 0.0;
		b2 = d2 < 0.0;
		b3 = d3 < 0.0;
		return ((b1 == b2) && (b2 == b3));
	}

	/**
	 * Tests if the circle defined by three non-lin points (p1,p2,p3) contains the point p.
	 * The center of the circle is the circumcenter of the triangle and the radius is equalt to the
	 * distance between the circumcenter and any point of {p1, p2, p3}.
	 *
	 * Assumtion: a, b, c are in ccw-order!
	 *
	 * @param a    point of the triangle
	 * @param b    point of the triangle
	 * @param c    point of the triangle
	 * @param p    point which the circle might contain.
	 * @return true, if the circle defined by three non-lin points (p1,p2,p3) contains the point p, otherwise false
	 */
	public static boolean isInsideCircle(final IPoint a, final IPoint b, final IPoint c, final IPoint p) {
		return isInsideCircle(a, b, c, p.getX(), p.getY());
	}


	public static boolean isInsideCircle(final IPoint a, final IPoint b, final IPoint c, double x , double y) {
		/*IPoint qp = q.subtract(p);
		IPoint rp = r.subtract(p);
		IPoint tp = t.subtract(p);

		double a = qp.getX() * tp.getY() - qp.getY() * tp.getX();
		double b = tp.getX() * (t.getX() - q.getX()) + tp.getY() * (t.getY() - q.getY());
		double c = qp.getX() * rp.getY() - qp.getY() * rp.getX();
		double d = rp.getX() * (r.getX() - q.getX()) + rp.getY() * (r.getY() - q.getY());

		return a * d > c * b;*/


		double adx = a.getX() - x;
		double ady = a.getY() - y;
		double bdx = b.getX() - x;
		double bdy = b.getY() - y;
		double cdx = c.getX() - x;
		double cdy = c.getY() - y;

		double abdet = adx * bdy - bdx * ady;
		double bcdet = bdx * cdy - cdx * bdy;
		double cadet = cdx * ady - adx * cdy;
		double alift = adx * adx + ady * ady;
		double blift = bdx * bdx + bdy * bdy;
		double clift = cdx * cdx + cdy * cdy;

		double disc = alift * bcdet + blift * cadet + clift * abdet;
		//log.info("inCicle = " + disc);
		return disc > 0;
	}

	/**
	 * Computes the cross product of two vectors and store it in the cross
	 * vector.
	 *
	 * @param v1
	 * @param v2
	 * @param cross
	 */
	public static void cross(double[] v1, double[] v2, double[] cross) {
		cross[0] = v1[1] * v2[2] - v1[2] * v2[1];
		cross[1] = v1[2] * v2[0] - v1[0] * v2[2];
		cross[2] = v1[0] * v2[1] - v1[1] * v2[0];
	}

	public static VPolygon polygonFromPoints2D(final List<VPoint> vertices) {
		return polygonFromPoints2D(vertices.toArray(new VPoint[0]));
	}

	/**
	 * Constructs a new Polygon defined by the vertices. It is assumed that
	 * all vertices are distinct.
	 *
	 * @param vertices the defining distinct vertices.
	 * @return a new Polygon
	 */
	public static VPolygon polygonFromPoints2D(final IPoint... vertices) {
		Path2D.Double result = new Path2D.Double();
		if (vertices.length == 0)
			return new VPolygon(result);

		IPoint last = vertices[vertices.length - 1];
		result.moveTo(last.getX(), last.getY());

		for (int i = 0; i < vertices.length; i++) {
			result.lineTo(vertices[i].getX(), vertices[i].getY());
		}

		return new VPolygon(result);
	}

	/**
	 * Computes the area of a Polygon.
	 *
	 * @param vertices distinct vertices defining the polygon.
	 * @return the area of a polygon
	 */
	public static double areaOfPolygon(final List<? extends IPoint> vertices) {
		double result = 0;
		if(vertices.size() >= 3) {
			for (int i = 0; i < vertices.size() - 1; i++) {
				result += vertices.get(i).getX() * vertices.get(i + 1).getY() - vertices.get(i + 1).getX() * vertices.get(i).getY();
			}
			int n = vertices.size() - 1;
			result += vertices.get(n).getX() * vertices.get(0).getY() - vertices.get(0).getX() * vertices.get(n).getY();
		}
		return Math.abs(result) / 2.0;
	}

	/**
	 * Computes the distance from the line-segment defined by (p1,p2) to the point p.
	 * @param p1    first point of the line-segment
	 * @param p2    second point of the line-segment
	 * @param p     the point
	 * @return he distance from the line-segment defined by (p1,p2) to the point p.
	 */
	public static double distanceToLineSegment(final IPoint p1, final IPoint p2, final IPoint p) {
		return distanceToLineSegment(p1, p2, p.getX(), p.getY());
	}

	public static double distanceToLineSegment(final IPoint p1, final IPoint p2, final double x, final double y) {
		if (p1.getX() == p2.getX() && p1.getY() == p2.getY())
			return p1.distance(x,y);

		double len2 = (p2.getX() - p1.getX()) * (p2.getX() - p1.getX()) + (p2.getY() - p1.getY()) * (p2.getY() - p1.getY());
		double r = ((x - p1.getX()) * (p2.getX() - p1.getX()) + (y - p1.getY()) * (p2.getY() - p1.getY())) / len2;

		if (r <= 0.0)
			return p1.distance(x,y);
		if (r >= 1.0)
			return p2.distance(x,y);

		double s = ((p1.getY() - y) * (p2.getX() - p1.getX()) - (p1.getX() - x) * (p2.getY() - p1.getY())) / len2;
		return Math.abs(s) * Math.sqrt(len2);
	}

	public static boolean isOnEdge(final IPoint p1, final IPoint p2, final IPoint p, double tolerance) {
		return distanceToLineSegment(p1, p2, p) < tolerance;
	}


	/**
	 * Computes the intersection points of a line and a circle. The line is supposed to have infinity
	 * length and is defined by the two points of the VLine.
	 *
	 * @param line      the line
	 * @param circle    the circle
	 * @return  all intersection poins of the line with the circle i.e. 1, 2 or 0 results.
	 */
	public static VPoint[] intersection(final VLine line, final VCircle circle) {
		double m = line.slope();
		double d = line.getY1() - m * line.getX1();
		double a = circle.getCenter().getX();
		double b = circle.getCenter().getY();

		double discreminant = circle.getRadius() * circle.getRadius() * (1 + m*m) - (b -m * a -d) * (b - m * a - d);


		if(discreminant < 0) {
			return new VPoint[0];
		}
		else if(discreminant == 0){
			double x = (a + b * m - d * m) / (1 + m*m);
			double y = m * x + d;
			return  new VPoint[]{new VPoint(x, y)};
		}
		else {
			double x1 = (a + b * m - d * m + Math.sqrt(discreminant)) / (1 + m*m);
			double y1 = m * x1 + d;

			double x2 = (a + b * m - d * m - Math.sqrt(discreminant)) / (1 + m*m);
			double y2 = m * x2 + d;

			return new VPoint[]{ new VPoint(x1, y1), new VPoint(x2, y2)};
		}
	}

	/**
	 * The (smallest possible) angle at C from the triangle ACB.
	 *
	 * @param A
	 * @param C
	 * @param B
	 * @return
	 */
	public static double angle(IPoint A, IPoint C, IPoint B) {
		double phi1 = angleTo(A, C);
		double phi2 = angleTo(B, C);
		double phi = Math.abs(phi1 - phi2);
		return Math.min(phi, 2 * Math.PI - phi);
	}

	/**
	 *
	 * Computes the angle between the x-axis through the given Point "center" and this.
	 * Result is in interval (0,2*PI) according to standard math usage.
	 */
	public static double angleTo(final IPoint from, final IPoint to) {
		double atan2 = Math.atan2(from.getY() - to.getY(), from.getX() - to.getX());

		if (atan2 < 0.0) {
			atan2 = Math.PI * 2 + atan2;
		}

		return atan2;
	}

	public static double angleTo(final IPoint to) {
		return angleTo(new VPoint(0,0), to);
	}

	/**
	 * Returns the angle between line1 and line2 in clock wise order (cw).
	 * @param line1
	 * @param line2
	 * @return
	 */
	public static double angleBetween2Lines(final VLine line1, final VLine line2)
	{
		double angle1 = Math.atan2(line1.getY1() - line1.getY2(),
				line1.getX1() - line1.getX2());
		double angle2 = Math.atan2(line2.getY1() - line2.getY2(),
				line2.getX1() - line2.getX2());
		return (angle1-angle2) < 0 ? (angle1-angle2) + 2*Math.PI :(angle1-angle2);
	}

	public static double sign(final double x1, final double y1, final double x2, final double y2, final double x3, final double y3) {
		return (x1 - x3) * (y2 - y3) - (x2 -x3) * (y1 - y3);
	}

	public static <P extends IPoint> VRectangle bound(final Collection<P> points) {
		return bound(points, 0.0);
	}

	public static <P extends IPoint> VRectangle bound(final Collection<P> points, final double epilon) {
		if(points.isEmpty()) {
			throw new IllegalArgumentException("the point collection is empty.");
		}

		VPoint pMax = points.stream().map(p -> new VPoint(p.getX(), p.getY())).reduce((p1, p2) -> new VPoint(Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()))).get();
		VPoint pMin = points.stream().map(p -> new VPoint(p.getX(), p.getY())).reduce((p1, p2) -> new VPoint(Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()))).get();

		return new VRectangle(pMin.getX()-epilon, pMin.getY()-epilon, pMax.getX() - pMin.getX() + 2*epilon, pMax.getY() - pMin.getY() + 2*epilon);
	}


	/**
	 * This method follows the construction from
	 * https://proofwiki.org/wiki/Obtuse_Triangle_Divided_into_Acute_Triangles
	 * i.e. divides an non-acute triangle acb into 7 acute triangles:
	 *  new VTriangle(a, f, e),
	 *  new VTriangle(e, f, d),
	 *  new VTriangle(d, c, e),
	 *  new VTriangle(d, h ,c),
	 *  new VTriangle(d, g, h),
	 *  new VTriangle(f, g, d),
	 *  new VTriangle(g, b, h);.
	 *  if the triangle is non-acute at c. If the triangle is already acute the method
	 *  returns the original triangle.
	 *
	 * @param triangle
	 * @throws throws an illegal argument exception if the triangle is not a feasible triangle.
	 */
	public static VTriangle[] generateAcuteTriangles(final VTriangle triangle) {
		double angle1 = angle(triangle.p1, triangle.p2, triangle.p3);
		double angle2 = angle(triangle.p2, triangle.p3, triangle.p1);
		double angle3 = angle(triangle.p3, triangle.p1, triangle.p2);
		double tolerance = 0.000001;

		// non-acute triangle
		if(triangle.isNonAcute()) {
			VPoint c;
			VPoint a;
			VPoint b;
			if(angle1 > angle2 && angle1 > angle3) {
				a = triangle.p3;
				c = triangle.p2;
				b = triangle.p1;
			}
			else if(angle2 > angle1 && angle2 > angle3) {
				a = triangle.p1;
				c = triangle.p3;
				b = triangle.p2;
			}
			else if(angle3 > angle1 && angle3 > angle2) {
				a = triangle.p2;
				c = triangle.p1;
				b = triangle.p3;
			}
			else {
				throw new IllegalArgumentException(triangle + " is not a feasible triangle");
			}

			VPoint d = triangle.getIncenter();
			VCircle circle = new VCircle(d, d.distance(c));
			VPoint[] iPoints = intersection(new VLine(a, c), circle);

			VPoint e = iPoints[0].equals(c, tolerance) ? iPoints[1] : iPoints[0];
			iPoints = intersection(new VLine(b, c), circle);
			VPoint h = iPoints[0].equals(c, tolerance) ? iPoints[1] : iPoints[0];

			iPoints = intersection(new VLine(a, b), circle);
			VPoint f = iPoints[0].distance(a) < iPoints[1].distance(a) ? iPoints[0] : iPoints[1];
			VPoint g = iPoints[0].distance(a) < iPoints[1].distance(a) ? iPoints[1] : iPoints[0];

			return new VTriangle[]{
					new VTriangle(a, f, e),
					new VTriangle(e, f, d),
					new VTriangle(d, c, e),
					new VTriangle(d, h ,c),
					new VTriangle(d, g, h),
					new VTriangle(f, g, d),
					new VTriangle(g, b, h)
			};
		}
		else {
			return new VTriangle[]{triangle};
		}

	}

	public static VPoint add(final VPoint p1, final VPoint p2) {
		return new VPoint(p1.x + p2.x, p1.y + p2.y);
	}

	public static boolean isValid(@NotNull final VTriangle triangle) {
		List<VPoint> points = triangle.getPoints();
		return GeometryUtils.isLeftOf(points.get(0), points.get(1), points.get(2));
	}

	public static double qualityOf(@NotNull final VTriangle triangle) {
		VLine[] lines = triangle.getLines();
		double a = lines[0].length();
		double b = lines[1].length();
		double c = lines[2].length();
		double part = 0.0;
		if(a != 0.0 && b != 0.0 && c != 0.0) {
			part = ((b + c - a) * (c + a - b) * (a + b - c)) / (a * b * c);
		}
		else {
			part = 0.0;
		}
		return part;
	}

	public static VPoint lineIntersectionPoint(final double x1,
											   final double y1,
											   final double x2,
											   final double y2,
											   final double x3,
											   final double y3,
											   final double x4,
											   final double y4) {
		assert new VLine(new VPoint(x1, y1), new VPoint(x2, y2)).intersectsLine(x3, y3, x4, y4);
		double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
		assert d != 0;

		double x = ((x1 * y2 - y1 - x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
		double y = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (y3 * y4 - y3 * x4)) / d;
		return new VPoint(x, y);
	}

	public static VPoint lineIntersectionPoint(final VPoint p1, final VPoint p2, final VPoint q1, final VPoint q2) {
		return lineIntersectionPoint(p1.x, p1.y, p2.x, p2.y, q1.x, q1.y, q2.x, q2.y);
	}

	public static VPoint lineIntersectionPoint(final VLine line,
											   final double x3,
											   final double y3,
											   final double x4,
											   final double y4) {
		return lineIntersectionPoint(line.getX1(), line.getY1(), line.getX2(), line.getY2(), x3, y3, x4, y4);
	}
}
