package nl.andrewl.infinirails.model;

import lombok.Getter;
import nl.andrewl.infinirails.util.MathUtils;
import org.joml.Math;
import org.joml.*;

public class Camera {
	private static final Vector3fc UP = new Vector3f(0, 1, 0);
	private static final Vector3fc RIGHT = new Vector3f(1, 0, 0);
	private static final Vector3fc FORWARD = new Vector3f(0, 0, -1);

	@Getter
	private final Vector3f position;
	@Getter
	private final Vector2f orientation;

	private final Matrix4f viewTransform;
	private final float[] viewTransformValues = new float[16];

	public Camera() {
		this.position = new Vector3f();
		this.orientation = new Vector2f();
		this.viewTransform = new Matrix4f();
	}

	private void updateViewTransform() {
		float horizontalAngle = Math.toRadians(orientation.x);
		float verticalAngle = Math.toRadians(orientation.y * -1);
//		System.out.printf("Camera orientation: %.2f degrees horizontal => %.2f rad, %.2f degrees vertical => %.2f rad\n", orientation.x, horizontalAngle, orientation.y, verticalAngle);
		viewTransform.identity();
		viewTransform.rotate(verticalAngle, new Vector3f(1, 0, 0));
		viewTransform.rotate(horizontalAngle, UP);
		viewTransform.translate(position);
		viewTransform.get(viewTransformValues);
	}

	public void setPosition(float x, float y, float z) {
		position.set(x, y, z);
		updateViewTransform();
	}

	public void movePosition(Vector3f movement) {
		Matrix4f moveTransform = new Matrix4f();
		moveTransform.rotate(Math.toRadians(orientation.x), UP);
		moveTransform.transformDirection(movement);
		position.add(movement);
		updateViewTransform();
	}

	public void setOrientation(float x, float y) {
		orientation.x = (float) MathUtils.normalize(x, 0, 360);
		orientation.y = (float) MathUtils.normalize(y, -90, 90);
		updateViewTransform();
	}

	public void moveOrientation(float x, float y) {
		orientation.x = (float) MathUtils.normalize(orientation.x + x, 0, 360);
		orientation.y = Math.max(-90, Math.min(90, orientation.y + y));
		updateViewTransform();
	}

	public float[] getPositionValues() {
		return new float[]{position.x, position.y, position.z};
	}

	public Matrix4f getViewTransform() {
		return viewTransform;
	}

	public float[] getViewTransformValues() {
		return viewTransformValues;
	}
}
