package nl.andrewl.infinirails.model;

import lombok.Getter;
import nl.andrewl.infinirails.OpenSimplexNoise;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.joml.Vector3f;

import java.util.*;

/**
 * Represents a world with some terrain and objects in it.
 */
public class World {
	/**
	 * The size of terrain fragments, in terms of the number of tiles. This
	 * means each fragment has SIZE + 1 vertex dimensions.
	 */
	public static final int TERRAIN_FRAGMENT_SIZE = 100;
	/**
	 * The scale of each fragment. A scale of 1 implies 1 unit in the fragment
	 * means 1 meter of distance.
	 */
	public static final float TERRAIN_FRAGMENT_SCALE = 1.0f;

	public static final int RENDER_RADIUS = 5;

	@Getter
	private Map<Vector2ic, TerrainFragment> terrainFragments;
	private OpenSimplexNoise noise;
	@Getter
	private Camera camera;

	public World() {
		this.camera = new Camera();
		this.noise = new OpenSimplexNoise(0L);
		this.terrainFragments = new HashMap<>();
	}

	public void updateCameraPosition(Vector3f m) {
		camera.movePosition(m);
		Vector2f cameraHorizontalPos = new Vector2f(camera.getPosition().x, camera.getPosition().z);
		var fragIndex = getTerrainFragmentIndex(cameraHorizontalPos);
//		System.out.printf("Pos: [%.2f, %.2f] => Frag: [%d, %d]\n", cameraHorizontalPos.x, cameraHorizontalPos.y, fragIndex.x, fragIndex.y);
		for (int x = fragIndex.x - 2; x <= fragIndex.x + 2; x++) {
			for (int y = fragIndex.y - 2; y <= fragIndex.y + 2; y++) {
				var pos = new Vector2i(x, y);
				var fragment = terrainFragments.get(pos);
				if (fragment == null) {
					generateFragment(pos);
				}
			}
		}
	}

	public void generateFragment(Vector2i index) {
		terrainFragments.put(index, new TerrainFragment(this, index, noise));
	}

	public Collection<TerrainFragment> getFragmentsNearPlayer() {
		Vector2f cameraHorizontalPos = new Vector2f(camera.getPosition().x, camera.getPosition().z);
		var fragIndex = getTerrainFragmentIndex(cameraHorizontalPos);
		List<TerrainFragment> frags = new ArrayList<>(25);
		for (int x = fragIndex.x - 2; x <= fragIndex.x + 2; x++) {
			for (int y = fragIndex.y - 2; y <= fragIndex.y + 2; y++) {
				var pos = new Vector2i(x, y);
				var fragment = terrainFragments.get(pos);
				if (fragment != null) {
					frags.add(fragment);
				}
			}
		}
		return frags;
	}

	public TerrainFragment getFragmentAt(Vector2f c) {
		var fragmentIndex = getTerrainFragmentIndex(c);
		return terrainFragments.get(fragmentIndex);
	}

	public static Vector2i getTerrainFragmentIndex(Vector2f p) {
		int px = (int) p.x;
		int py = (int) p.y;
		int x = px / 100;
		if (px < 0) x--;
		int y = py / 100;
		if (py < 0) y--;
		return new Vector2i(x, y);
	}
}
