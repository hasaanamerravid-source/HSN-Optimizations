package net.caffeinemc.mods.sodium.client.render.chunk;

/**
 * Compile-only stand-in for Sodium's section node. The mixin only needs
 * origin accessors; at runtime Sodium's class is loaded instead of this one.
 */
public class RenderSection {

	private int chunkX;
	private int chunkY;
	private int chunkZ;

	public RenderSection() {
	}

	public RenderSection(int chunkX, int chunkY, int chunkZ) {
		this.chunkX = chunkX;
		this.chunkY = chunkY;
		this.chunkZ = chunkZ;
	}

	public int getChunkX() {
		return this.chunkX;
	}

	public int getChunkY() {
		return this.chunkY;
	}

	public int getChunkZ() {
		return this.chunkZ;
	}

	public int getX() {
		return this.chunkX;
	}

	public int getY() {
		return this.chunkY;
	}

	public int getZ() {
		return this.chunkZ;
	}

	public int getOriginX() {
		return this.chunkX << 4;
	}

	public int getOriginY() {
		return this.chunkY << 4;
	}

	public int getOriginZ() {
		return this.chunkZ << 4;
	}

	public int getCenterX() {
		return getOriginX() + 8;
	}

	public int getCenterY() {
		return getOriginY() + 8;
	}

	public int getCenterZ() {
		return getOriginZ() + 8;
	}
}
