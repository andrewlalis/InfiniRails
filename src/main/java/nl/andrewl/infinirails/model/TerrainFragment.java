package nl.andrewl.infinirails.model;

import lombok.Getter;
import nl.andrewl.infinirails.OpenSimplexNoise;
import nl.andrewl.infinirails.Vertex;
import nl.andrewl.infinirails.util.MathUtils;
import org.joml.*;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;

/**
 * A square section of a terrain heightmap.
 */
@Getter
public class TerrainFragment {
	private final World world;
	private final Vector2i index;
	private final Vector2f center;
	private final float[][] heightmap;

	private final int meshBuffer;
	private final int meshArray;
	private final int vertexCount;
	@Getter
	private final Matrix4f worldTransform;
	@Getter
	private final Matrix3f normalTransform;

	public TerrainFragment(World world, Vector2i index, OpenSimplexNoise noise) {
		this.world = world;
		this.index = index;
		float originX = index.x * (World.TERRAIN_FRAGMENT_SIZE);
		float originY = index.y * (World.TERRAIN_FRAGMENT_SIZE);
		this.heightmap = new float[World.TERRAIN_FRAGMENT_SIZE + 1][World.TERRAIN_FRAGMENT_SIZE + 1];
		this.center = new Vector2f(originX + World.TERRAIN_FRAGMENT_SIZE / 2.0f, originY + World.TERRAIN_FRAGMENT_SIZE / 2.0f);
		buildHeightmap(noise, originX, originY);
		meshBuffer = glGenBuffers();
		meshArray = glGenVertexArrays();
		vertexCount = buildMesh();
		this.worldTransform = new Matrix4f().translate(originX, 0, originY);
		this.normalTransform = new Matrix3f();
		worldTransform.normal(normalTransform);
		System.out.printf("Generated terrain fragment [%d, %d] with %d vertices and origin [%.2f, %.2f].\n", index.x, index.y, vertexCount, originX, originY);
	}

	private Vector3f getHeightmapPointAsVector(int i, int j) {
		return new Vector3f(j, heightmap[i][j], i);
	}

	public void draw() {
		glBindBuffer(GL_ARRAY_BUFFER, meshBuffer);
		glBindVertexArray(meshArray);
		glDrawArrays(GL_TRIANGLES, 0, vertexCount);
	}

	private void buildHeightmap(OpenSimplexNoise noise, float originX, float originY) {
		float noiseStretch = 40;
		float noiseScale = 10;
		for (int i = 0; i < heightmap.length; i++) {
			for (int j = 0; j < heightmap.length; j++) {
				heightmap[i][j] = (float) noise.eval((originX + j) / noiseStretch, (originY + i) / noiseStretch) * noiseScale;
			}
		}
	}

	private int buildMesh() {
		float[] vertexData = heightMapToVertexData(heightmap);
		glBindBuffer(GL_ARRAY_BUFFER, meshBuffer);
		glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW);

		glBindVertexArray(meshArray);
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 9 * Float.BYTES, 0);
		glEnableVertexAttribArray(1);
		glVertexAttribPointer(1, 3, GL_FLOAT, false, 9 * Float.BYTES, 3 * Float.BYTES);
		glEnableVertexAttribArray(2);
		glVertexAttribPointer(2, 3, GL_FLOAT, false, 9 * Float.BYTES, 6 * Float.BYTES);
		return vertexData.length / 9;
	}

	private float[] heightMapToVertexData(float[][] heightmap) {
		List<Float> data = new ArrayList<>();
		for (int i = 0; i < heightmap.length - 1; i++) {
			for (int j = 0; j < heightmap.length - 1; j++) {
				var topLeft = getHeightmapPointAsVector(i, j);
				var topRight = getHeightmapPointAsVector(i, j + 1);
				var bottomLeft = getHeightmapPointAsVector(i + 1, j);
				var bottomRight = getHeightmapPointAsVector(i + 1, j + 1);
				Vector3f color = new Vector3f(0.1f, 0.8f, 0.2f);
				var v1 = new Vertex(topRight, color, computeHeightMapNormal(heightmap, i, j + 1));
				var v2 = new Vertex(topLeft, color, computeHeightMapNormal(heightmap, i, j));
				var v3 = new Vertex(bottomLeft, color, computeHeightMapNormal(heightmap, i + 1, j));
				var v4 = new Vertex(bottomLeft, color, computeHeightMapNormal(heightmap, i + 1, j));
				var v5 = new Vertex(bottomRight, color, computeHeightMapNormal(heightmap, i + 1, j + 1));
				var v6 = new Vertex(topRight, color, computeHeightMapNormal(heightmap, i, j + 1));
				data.addAll(v1.getFloats());
				data.addAll(v2.getFloats());
				data.addAll(v3.getFloats());
				data.addAll(v4.getFloats());
				data.addAll(v5.getFloats());
				data.addAll(v6.getFloats());
			}
		}
		var f = new float[data.size()];
		for (int i = 0; i < data.size(); i++) {
			f[i] = data.get(i);
		}
		return f;
	}

	private Vector3f computeHeightMapNormal(float[][] heightmap, int i, int j) {
		var p = getHeightmapPointAsVector(i, j);
		List<Vector3f> faceNormals = new ArrayList<>(4);
		if (i > 0 && j > 0) {// Top left
			faceNormals.add(MathUtils.computeFaceNormal(
					getHeightmapPointAsVector(i - 1, j - 1),
					getHeightmapPointAsVector(i - 1, j),
					getHeightmapPointAsVector(i, j - 1),
					p
			));
		}
		if (i > 0 && j < heightmap[0].length - 1) {// Top right
			faceNormals.add(MathUtils.computeFaceNormal(
					getHeightmapPointAsVector(i - 1, j),
					getHeightmapPointAsVector(i - 1, j + 1),
					p,
					getHeightmapPointAsVector(i, j + 1)
			));
		}
		if (i < heightmap.length - 1 && j > 0) {// Bottom left
			faceNormals.add(MathUtils.computeFaceNormal(
					getHeightmapPointAsVector(i, j - 1),
					p,
					getHeightmapPointAsVector(i + 1, j - 1),
					getHeightmapPointAsVector(i + 1, j)
			));
		}
		if (i < heightmap.length - 1 && j < heightmap[0].length - 1) {// Bottom right
			faceNormals.add(MathUtils.computeFaceNormal(
					p,
					getHeightmapPointAsVector(i, j + 1),
					getHeightmapPointAsVector(i + 1, j),
					getHeightmapPointAsVector(i + 1, j + 1)
			));
		}
		Vector3f n = new Vector3f(0, 1, 0);
		for (var faceNormal : faceNormals) {
			n.add(faceNormal);
		}
		n.normalize();
		return n;
	}
}
