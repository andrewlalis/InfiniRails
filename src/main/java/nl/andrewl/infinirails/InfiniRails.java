package nl.andrewl.infinirails;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.Version;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class InfiniRails {
	public static void main(String[] args) throws Exception {
		System.out.println("LWJGL Version: " + Version.getVersion());
		GLFWErrorCallback.createPrint(System.err).set();
		if (!glfwInit()) throw new IllegalStateException("Could not initialize GLFW");
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

		long windowHandle = glfwCreateWindow(800, 600, "InfiniRails", NULL, NULL);
		if (windowHandle == NULL) throw new RuntimeException("Failed to create GLFW window.");
		glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
			if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
				glfwSetWindowShouldClose(windowHandle, true);
			}
		});

		glfwSetWindowPos(windowHandle, 50, 50);

		glfwMakeContextCurrent(windowHandle);
		glfwSwapInterval(1);
		glfwShowWindow(windowHandle);

		GL.createCapabilities();
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		glEnable(GL_CULL_FACE);
		glCullFace(GL_FRONT);

		int prog = glCreateProgram();
		int fragShader = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fragShader, readClasspathFile("shader/fragment.glsl"));
		glCompileShader(fragShader);
		glAttachShader(prog, fragShader);
		int vertShader = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vertShader, readClasspathFile("shader/vertex.glsl"));
		glCompileShader(vertShader);
		glAttachShader(prog, vertShader);

		glValidateProgram(prog);
		glLinkProgram(prog);
		glUseProgram(prog);

		Matrix4f modelTransform = new Matrix4f();
		modelTransform.translate(0, 0, 0);
		Matrix3f normalTransform = new Matrix3f();
		modelTransform.normal(normalTransform);
		Matrix4f projectionTransform = new Matrix4f();
		projectionTransform.perspective(90, 800 / 600.0f, 0.01f, 1000.0f);

		Vector3f lightPosition = new Vector3f(-500, 300, 500);
		Vector3f lightColor = new Vector3f(1.0f, 0.8f, 0.8f);
		Vector3f cameraPosition = new Vector3f(0, 100, 0);
		Matrix4f viewTransform = new Matrix4f();
		viewTransform.lookAt(cameraPosition, new Vector3f(100, 0, 0), new Vector3f(0, 1, 0));

		int modelTransformUniform = glGetUniformLocation(prog, "modelTransform");
		int normalTransformUniform = glGetUniformLocation(prog, "normalTransform");
		int projectionTransformUniform = glGetUniformLocation(prog, "projectionTransform");
		int viewTransformUniform = glGetUniformLocation(prog, "viewTransform");

		int lightPositionUniform = glGetUniformLocation(prog, "lightPosition");
		int lightColorUniform = glGetUniformLocation(prog, "lightColor");
		int cameraPositionUniform = glGetUniformLocation(prog, "cameraPosition");

		glUniformMatrix4fv(modelTransformUniform, false, modelTransform.get(new float[16]));
		glUniformMatrix3fv(normalTransformUniform, false, normalTransform.get(new float[9]));
		glUniformMatrix4fv(projectionTransformUniform, false, projectionTransform.get(new float[16]));
		glUniformMatrix4fv(viewTransformUniform, false, viewTransform.get(new float[16]));

		glUniform3fv(lightPositionUniform, toArray(lightPosition));
		glUniform3fv(lightColorUniform, toArray(lightColor));
		glUniform3fv(cameraPositionUniform, toArray(cameraPosition));

		var noise = new OpenSimplexNoise();
		Vector3f[][] heightmap = new Vector3f[1000][1000];
		for (int i = 0; i < heightmap.length; i++) {
			heightmap[i] = new Vector3f[1000];
			for (int j = 0; j < heightmap[i].length; j++) {
				float x = i - 500;
				float z = j - 500;
				float y = (float) (20 * noise.eval(x / 100.0f, z / 100.0f));
				heightmap[i][j] = new Vector3f(x, y, z);
			}
		}
		int meshVBO = glGenBuffers();
		int meshVAO = glGenVertexArrays();
		glBindBuffer(GL_ARRAY_BUFFER, meshVBO);
		glBindVertexArray(meshVAO);

		var data = heightMapToData(heightmap);
		glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);

		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 9 * Float.BYTES, 0);
		glEnableVertexAttribArray(1);
		glVertexAttribPointer(1, 3, GL_FLOAT, false, 9 * Float.BYTES, 3 * Float.BYTES);
		glEnableVertexAttribArray(2);
		glVertexAttribPointer(2, 3, GL_FLOAT, false, 9 * Float.BYTES, 6 * Float.BYTES);

		while (!glfwWindowShouldClose(windowHandle)) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			glBindBuffer(GL_ARRAY_BUFFER, meshVBO);
			glBindVertexArray(meshVAO);
			glDrawArrays(GL_TRIANGLES, 0, data.length / 9);

			glfwSwapBuffers(windowHandle);
			glfwPollEvents();
		}

		Callbacks.glfwFreeCallbacks(windowHandle);
		glfwDestroyWindow(windowHandle);
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}

	public static float[] heightMapToData(Vector3f[][] heightmap) {
		List<Float> data = new ArrayList<>();
		for (int i = 0; i < heightmap.length - 1; i++) {
			for (int j = 0; j < heightmap[i].length - 1; j++) {
				var topLeft = heightmap[i][j];
				var topRight = heightmap[i][j+1];
				var bottomLeft = heightmap[i+1][j];
				var bottomRight = heightmap[i+1][j+1];
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

	public static Vector3f computeHeightMapNormal(Vector3f[][] heightmap, int i, int j) {
		var p = heightmap[i][j];
		List<Vector3f> faceNormals = new ArrayList<>(4);
		if (i > 0 && j > 0) {// Top left
			faceNormals.add(computeFaceNormal(heightmap[i - 1][j - 1], heightmap[i - 1][j], heightmap[i][j - 1], p));
		}
		if (i > 0 && j < heightmap[0].length - 1) {// Top right
			faceNormals.add(computeFaceNormal(heightmap[i - 1][j], heightmap[i - 1][j + 1], p, heightmap[i][j + 1]));
		}
		if (i < heightmap.length - 1 && j > 0) {// Bottom left
			faceNormals.add(computeFaceNormal(heightmap[i][j - 1], p, heightmap[i + 1][j - 1], heightmap[i + 1][j]));
		}
		if (i < heightmap.length - 1 && j < heightmap[0].length - 1) {// Bottom right
			faceNormals.add(computeFaceNormal(p, heightmap[i][j + 1], heightmap[i + 1][j], heightmap[i + 1][j + 1]));
		}
		Vector3f n = new Vector3f();
		for (var faceNormal : faceNormals) {
			n.add(faceNormal);
		}
		n.normalize();
		return n;
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

	public static String readClasspathFile(String res) throws IOException {
		InputStream is = InfiniRails.class.getClassLoader().getResourceAsStream(res);
		if (is == null) throw new IOException("Could not load classpath resource: " + res);
		String s = new String(is.readAllBytes());
		is.close();
		return s;
	}

	public static float[] toArray(Vector3f v) {
		return new float[]{v.x, v.y, v.z};
	}
}
