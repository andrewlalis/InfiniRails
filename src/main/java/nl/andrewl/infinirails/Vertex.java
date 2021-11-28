package nl.andrewl.infinirails;

import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;

public class Vertex {
	private Vector3f position;
	private Vector3f color;
	private Vector3f normal;

	public Vertex(Vector3f position, Vector3f color, Vector3f normal) {
		this.position = position;
		this.color = color;
		this.normal = normal;
	}

	public Vertex(Vector3f position) {
//		this(position, new Vector3f((float) Math.random(), (float) Math.random(), (float) Math.random()));
		this(position, new Vector3f(0.1f, 0.5f, 0.1f), new Vector3f(0, 1, 0));
	}

	public Vertex(float x, float y, float z) {
		this(new Vector3f(x, y, z));
	}

	public Collection<Float> getFloats() {
		return List.of(position.x, position.y, position.z, normal.x, normal.y, normal.z, color.x, color.y, color.z);
	}

	@Override
	public String toString() {
		return "Vertex{" +
				"position=" + position +
				", color=" + color +
				", normal=" + normal +
				'}';
	}
}
