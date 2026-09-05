package net.caffeinemc.mods.sodium.client.util.collections;

/**
 * Compile-only type used by {@code SodiumCircularMixin}.
 * Sodium ships the real queue at runtime; this interface only has to match
 * the methods the mixin mentions so the client sources compile without Sodium.
 */
public interface WriteQueue<T> {

	void add(T item);

	default boolean isEmpty() {
		return true;
	}

	default int size() {
		return 0;
	}
}
