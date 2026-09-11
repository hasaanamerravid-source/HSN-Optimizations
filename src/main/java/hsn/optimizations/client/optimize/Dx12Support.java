package hsn.optimizations.client.optimize;

/**
 * DirectX 12 is not a stock Minecraft 26.2 Fabric backend. This class
 * reports whether a D3D12 device showed up anyway (future renderer /
 * wrapper) and otherwise falls back: DX12 → Vulkan → OpenGL → CPU.
 * It never creates its own ID3D12Device next to the game.
 */
public final class Dx12Support {

	private Dx12Support() {
	}

	public static boolean active() {
		return GraphicsBackend.directx12();
	}

	public static boolean availableOnOs() {
		String os = System.getProperty("os.name", "").toLowerCase();
		return os.contains("win");
	}

	public static String status() {
		if (GraphicsBackend.directx12()) {
			return "active";
		}
		if (availableOnOs()) {
			return "os-ready-fallback-" + GraphicsBackend.label();
		}
		return "not-windows";
	}
}
