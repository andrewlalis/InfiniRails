package nl.andrewl.infinirails;

import nl.andrewl.infinirails.model.World;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.lwjgl.Version;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

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

		World world = new World();
		world.getCamera().setPosition(50, 20, 50);

		long windowHandle = glfwCreateWindow(1200, 800, "InfiniRails", NULL, NULL);
		if (windowHandle == NULL) throw new RuntimeException("Failed to create GLFW window.");
		glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
			if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
				glfwSetWindowShouldClose(windowHandle, true);
			}
		});
		glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
		glfwSetInputMode(windowHandle, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
		glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> {
			// TODO: Handle mouse buttons
		});

		AtomicReference<Float> lastX = new AtomicReference<>(0f);
		AtomicReference<Float> lastY = new AtomicReference<>(0f);
		glfwSetCursorPosCallback(windowHandle, (window, xpos, ypos) -> {
			double[] xb = new double[1];
			double[] yb = new double[1];
			glfwGetCursorPos(windowHandle, xb, yb);
			float x = (float) xb[0];
			float y = (float) yb[0];
			float dx = x - lastX.get();
			float dy = y - lastY.get();
			world.getCamera().moveOrientation(dx * 0.1f, dy * -0.1f);
			lastX.set(x);
			lastY.set(y);
		});

		glfwSetWindowPos(windowHandle, 50, 50);
		glfwSetCursorPos(windowHandle, 0, 0);

		glfwMakeContextCurrent(windowHandle);
		glfwSwapInterval(1);
		glfwShowWindow(windowHandle);



		GL.createCapabilities();
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		glEnable(GL_CULL_FACE);
		glEnable(GL_DEPTH_TEST);
		glCullFace(GL_BACK);

		world.generateFragment(new Vector2i(0, 0));

		Matrix4f projectionTransform = new Matrix4f();
		projectionTransform.perspective(70, 800 / 600.0f, 0.01f, 1000.0f);

		Vector3f lightPosition = new Vector3f(0, -300, 500);
		Vector3f lightColor = new Vector3f(1.0f, 0.8f, 0.8f);

		int prog = createShaderProgram();
		int modelTransformUniform = glGetUniformLocation(prog, "modelTransform");
		int normalTransformUniform = glGetUniformLocation(prog, "normalTransform");
		int projectionTransformUniform = glGetUniformLocation(prog, "projectionTransform");
		int viewTransformUniform = glGetUniformLocation(prog, "viewTransform");

		int lightPositionUniform = glGetUniformLocation(prog, "lightPosition");
		int lightColorUniform = glGetUniformLocation(prog, "lightColor");
		int cameraPositionUniform = glGetUniformLocation(prog, "cameraPosition");

		glUniformMatrix4fv(projectionTransformUniform, false, projectionTransform.get(new float[16]));

		glUniform3fv(lightPositionUniform, toArray(lightPosition));
		glUniform3fv(lightColorUniform, toArray(lightColor));

		while (!glfwWindowShouldClose(windowHandle)) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			glUniformMatrix4fv(viewTransformUniform, false, world.getCamera().getViewTransformValues());
			glUniform3fv(cameraPositionUniform, world.getCamera().getPositionValues());

			for (var terrainFragment : world.getFragmentsNearPlayer()) {
				glUniformMatrix4fv(modelTransformUniform, false, terrainFragment.getWorldTransform().get(new float[16]));
				glUniformMatrix3fv(normalTransformUniform, false, terrainFragment.getNormalTransform().get(new float[9]));
				terrainFragment.draw();
			}

			glfwSwapBuffers(windowHandle);
			glfwPollEvents();

			if (glfwGetKey(windowHandle, GLFW_KEY_W) == GLFW_PRESS) {
				world.updateCameraPosition(new Vector3f(0, 0, -1));
			}
			if (glfwGetKey(windowHandle, GLFW_KEY_A) == GLFW_PRESS) {
				world.updateCameraPosition(new Vector3f(-1, 0, 0));
			}
			if (glfwGetKey(windowHandle, GLFW_KEY_S) == GLFW_PRESS) {
				world.updateCameraPosition(new Vector3f(0, 0, 1));
			}
			if (glfwGetKey(windowHandle, GLFW_KEY_D) == GLFW_PRESS) {
				world.updateCameraPosition(new Vector3f(1, 0, 0));
			}
			if (glfwGetKey(windowHandle, GLFW_KEY_SPACE) == GLFW_PRESS) {
				world.updateCameraPosition(new Vector3f(0, -1, 0));
			}
			if (glfwGetKey(windowHandle, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) {
				world.updateCameraPosition(new Vector3f(0, 1, 0));
			}
		}

		Callbacks.glfwFreeCallbacks(windowHandle);
		glfwDestroyWindow(windowHandle);
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}

	public static String readClasspathFile(String res) throws IOException {
		InputStream is = InfiniRails.class.getClassLoader().getResourceAsStream(res);
		if (is == null) throw new IOException("Could not load classpath resource: " + res);
		String s = new String(is.readAllBytes());
		is.close();
		return s;
	}

	public static int createShaderProgram() throws IOException {
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
		return prog;
	}

	public static float[] toArray(Vector3f v) {
		return new float[]{v.x, v.y, v.z};
	}
}
