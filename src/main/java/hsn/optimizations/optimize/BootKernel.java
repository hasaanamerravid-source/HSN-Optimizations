package hsn.optimizations.optimize;

import hsn.optimizations.HSNOptimizations;
import hsn.optimizations.config.HSNConfig;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * World-open prefetch. Tries C++ then Rust then C then Java.
 * ASM owns cache-line touches on buffers the higher layers already read.
 */
public final class BootKernel {

	private static final MethodHandle PREFETCH_CPP;
	private static final MethodHandle PREFETCH_RS;
	private static final MethodHandle PREFETCH_C;
	private static final MethodHandle TOUCH_ASM;
	private static final MethodHandle SET_PRIO;

	static {
		Linker linker = Linker.nativeLinker();
		PREFETCH_CPP = bind(linker, "libhsn_cpp.so", "hsn_cpp_prefetch_path",
				FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
		PREFETCH_RS = bind(linker, "libhsn_hotpath.so", "hsn_rs_prefetch_path",
				FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
		PREFETCH_C = bind(linker, "libhsn_c.so", "hsn_boot_prefetch_path",
				FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
		TOUCH_ASM = bind(linker, "libhsn_asm.so", "hsn_asm_touch_pages",
				FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
		SET_PRIO = bind(linker, "libhsn_c.so", "hsn_boot_set_current_priority",
				FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
	}

	private BootKernel() {
	}

	public static boolean nativeReady() {
		return PREFETCH_CPP != null || PREFETCH_RS != null || PREFETCH_C != null;
	}

	public static void prefetchWorld(Path worldRoot) {
		HSNConfig cfg = HSNConfig.get();
		if (cfg == null || !cfg.modEnabled || !cfg.prefetchRegionFiles || worldRoot == null) {
			return;
		}
		long t0 = System.nanoTime();
		int n = 0;
		String via = "java";
		try {
			n = invokePath(PREFETCH_CPP, worldRoot);
			if (n > 0) {
				via = "c++";
			} else {
				n = invokePath(PREFETCH_RS, worldRoot);
				if (n > 0) {
					via = "rust";
				} else {
					n = invokePath(PREFETCH_C, worldRoot);
					if (n > 0) {
						via = "c";
					} else {
						n = javaPrefetch(worldRoot);
					}
				}
			}
		} catch (Throwable t) {
			HSNOptimizations.LOGGER.debug("HSN boot prefetch skipped: {}", t.toString());
		}
		long ms = (System.nanoTime() - t0) / 1_000_000L;
		if (n > 0) {
			HSNOptimizations.LOGGER.info("HSN boot prefetch warmed {} files in {}ms ({})", n, ms, via);
		}
	}

	public static void setCurrentNativePriority(int javaPriority) {
		if (SET_PRIO == null) {
			return;
		}
		try {
			SET_PRIO.invokeExact(javaPriority);
		} catch (Throwable ignored) {
		}
	}

	public static void touchPages(MemorySegment segment, long bytes) {
		if (TOUCH_ASM == null || segment == null || bytes <= 0L) {
			return;
		}
		try {
			TOUCH_ASM.invokeExact(segment, bytes);
		} catch (Throwable ignored) {
		}
	}

	private static MethodHandle bind(Linker linker, String lib, String symbol, FunctionDescriptor desc) {
		try {
			Path path = NativeBridge.libraryPath(lib);
			if (path == null) {
				return null;
			}
			SymbolLookup lookup = SymbolLookup.libraryLookup(path, Arena.global());
			return linker.downcallHandle(lookup.find(symbol).orElseThrow(), desc);
		} catch (Throwable ignored) {
			return null;
		}
	}

	private static int invokePath(MethodHandle fn, Path root) {
		if (fn == null || root == null || !Files.isDirectory(root)) {
			return 0;
		}
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment cstr = arena.allocateFrom(root.toAbsolutePath().toString());
			return (int) fn.invokeExact(cstr);
		} catch (Throwable ignored) {
			return 0;
		}
	}

	private static int javaPrefetch(Path root) throws Exception {
		List<Path> files = new ArrayList<>();
		collect(root.resolve("region"), files);
		collect(root.resolve("entities"), files);
		collect(root.resolve("poi"), files);
		collect(root.resolve("data"), files);
		if (files.isEmpty()) {
			return 0;
		}
		int workers = Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors() / 2));
		ExecutorService pool = Executors.newFixedThreadPool(workers, r -> {
			Thread t = new Thread(r, "hsn-boot-prefetch");
			t.setDaemon(true);
			t.setPriority(Thread.MIN_PRIORITY + 1);
			return t;
		});
		try {
			List<CompletableFuture<Void>> jobs = new ArrayList<>(files.size());
			for (Path file : files) {
				jobs.add(CompletableFuture.runAsync(() -> warm(file), pool));
			}
			CompletableFuture.allOf(jobs.toArray(CompletableFuture[]::new)).join();
		} finally {
			pool.shutdownNow();
		}
		return files.size();
	}

	private static void collect(Path dir, List<Path> out) {
		if (dir == null || !Files.isDirectory(dir)) {
			return;
		}
		try (Stream<Path> stream = Files.list(dir)) {
			stream.filter(Files::isRegularFile).forEach(p -> {
				if (out.size() < 4096) {
					out.add(p);
				}
			});
		} catch (Exception ignored) {
		}
	}

	private static void warm(Path file) {
		try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
			ByteBuffer buf = ByteBuffer.allocateDirect(8192);
			ch.read(buf);
		} catch (Exception ignored) {
		}
	}
}
