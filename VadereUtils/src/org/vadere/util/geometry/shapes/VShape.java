package org.vadere.util.geometry.shapes;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

import org.vadere.util.geometry.ShapeType;

/**
 * Geometric shape and position.
 * 
 */
public interface VShape extends Shape, Cloneable {
	double distance(IPoint point);

	VPoint closestPoint(IPoint point);

	boolean contains(IPoint point);

	VShape translate(final IPoint vector);

	VShape translatePrecise(final IPoint vector);

	VShape scale(final double scalar);

	boolean intersects(VLine intersectingLine);

	VPoint getCentroid();

	ShapeType getType();

	boolean intersect(VShape shape);

	List<VPoint> getPath();

	static VPolygon generateHexagon(final double radius) {
		List<VPoint> points = new ArrayList<>();

		// in cw-order
		points.add(new VPoint(radius, 0));
		points.add(new VPoint(radius * Math.cos(1.0 / 3.0 * Math.PI), radius * Math.sin(1.0 / 3.0 * Math.PI)));
		points.add(new VPoint(radius * Math.cos(2.0 / 3.0 * Math.PI), radius * Math.sin(2.0 / 3.0 * Math.PI)));
		points.add(new VPoint(-radius, 0));
		points.add(new VPoint(radius * Math.cos(4.0 / 3.0 * Math.PI), radius * Math.sin(4.0 / 3.0 * Math.PI)));
		points.add(new VPoint(radius * Math.cos(5.0 / 3.0 * Math.PI), radius * Math.sin(5.0 / 3.0 * Math.PI)));


		Path2D path2D = new Path2D.Double();

		path2D.moveTo(points.get(0).getX(),points.get(0).getY());
		path2D.lineTo(points.get(0).getX(),points.get(0).getY());

		for(int i = 1; i < points.size(); i++) {
			path2D.lineTo(points.get(i).getX(),points.get(i).getY());
		}

		path2D.lineTo(points.get(0).getX(),points.get(0).getY());

		return new VPolygon(path2D);
	}
}
