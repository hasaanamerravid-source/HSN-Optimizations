package hsn.modod.client.optimize;

/**
 * Six view-frustum planes (a,b,c,d) packed into 24 floats.
 * Used only when native frustum culling is enabled in settings.
 */
public final class CameraFrustum {

	private CameraFrustum() {
	}

	public static void fillPlanes(float[] planes24,
			double camX, double camY, double camZ,
			double lookX, double lookY, double lookZ,
			float near, float far, float halfH, float halfV) {
		if (planes24 == null || planes24.length < 24) {
			return;
		}
		double lx = lookX, ly = lookY, lz = lookZ;
		double len = Math.sqrt(lx * lx + ly * ly + lz * lz);
		if (len < 1.0e-8) {
			lx = 0;
			ly = 0;
			lz = 1;
		} else {
			lx /= len;
			ly /= len;
			lz /= len;
		}
		double wx = 0, wy = 1, wz = 0;
		if (Math.abs(ly) > 0.95) {
			wx = 1;
			wy = 0;
			wz = 0;
		}
		double rx = ly * wz - lz * wy;
		double ry = lz * wx - lx * wz;
		double rz = lx * wy - ly * wx;
		double rlen = Math.sqrt(rx * rx + ry * ry + rz * rz);
		if (rlen < 1.0e-8) {
			rx = 1;
			ry = 0;
			rz = 0;
		} else {
			rx /= rlen;
			ry /= rlen;
			rz /= rlen;
		}
		double ux = ry * lz - rz * ly;
		double uy = rz * lx - rx * lz;
		double uz = rx * ly - ry * lx;

		double ch = Math.cos(halfH);
		double sh = Math.sin(halfH);
		double cv = Math.cos(halfV);
		double sv = Math.sin(halfV);

		// near, far, left, right, bottom, top
		plane(planes24, 0, lx, ly, lz, camX + lx * near, camY + ly * near, camZ + lz * near);
		plane(planes24, 1, -lx, -ly, -lz, camX + lx * far, camY + ly * far, camZ + lz * far);
		plane(planes24, 2, lx * ch + rx * sh, ly * ch + ry * sh, lz * ch + rz * sh, camX, camY, camZ);
		plane(planes24, 3, lx * ch - rx * sh, ly * ch - ry * sh, lz * ch - rz * sh, camX, camY, camZ);
		plane(planes24, 4, lx * cv + ux * sv, ly * cv + uy * sv, lz * cv + uz * sv, camX, camY, camZ);
		plane(planes24, 5, lx * cv - ux * sv, ly * cv - uy * sv, lz * cv - uz * sv, camX, camY, camZ);
	}

	private static void plane(float[] o, int slot, double nx, double ny, double nz,
			double px, double py, double pz) {
		double nlen = Math.sqrt(nx * nx + ny * ny + nz * nz);
		if (nlen < 1.0e-8) {
			nlen = 1.0;
		}
		nx /= nlen;
		ny /= nlen;
		nz /= nlen;
		int i = slot * 4;
		o[i] = (float) nx;
		o[i + 1] = (float) ny;
		o[i + 2] = (float) nz;
		o[i + 3] = (float) -(nx * px + ny * py + nz * pz);
	}
}
