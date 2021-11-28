package nl.andrewl.infinirails.util;

import org.joml.Vector3f;

public class MathUtils {
	public static double normalize(double value, double start, double end) {
		final double width = end - start;
		final double offsetValue = value - start;
		return offsetValue - (Math.floor(offsetValue / width) * width) + start;
	}

	public static Vector3f computeFaceNormal(Vector3f topLeft, Vector3f topRight, Vector3f bottomLeft, Vector3f bottomRight) {
		Vector3f n = new Vector3f();
		var x = new Vector3f();
		bottomRight.sub(topLeft, x);
		var y = new Vector3f();
		bottomLeft.sub(topRight, y);
		x.cross(y, n);
		return n.normalize();
	}
}
