//! World-open prefetch. Walks region / entity / poi files and pulls the
//! first bytes into page cache. No extra crates.

use std::fs::{self, File};
use std::io::Read;
use std::path::{Path, PathBuf};
use std::thread;

#[cfg(unix)]
extern "C" {
	fn posix_fadvise(fd: i32, offset: i64, len: i64, advice: i32) -> i32;
}

#[cfg(unix)]
const POSIX_FADV_WILLNEED: i32 = 3;

fn collect(dir: &Path, out: &mut Vec<PathBuf>) {
	if out.len() >= 4096 {
		return;
	}
	let ok = fs::read_dir(dir);
	let Ok(entries) = ok else {
		return;
	};
	for entry in entries.flatten() {
		if out.len() >= 4096 {
			break;
		}
		let path = entry.path();
		if path.is_file() {
			out.push(path);
		}
	}
}

fn touch(path: &Path) {
	let Ok(mut file) = File::open(path) else {
		return;
	};
	#[cfg(unix)]
	{
		use std::os::unix::io::AsRawFd;
		unsafe {
			let _ = posix_fadvise(file.as_raw_fd(), 0, 1 << 20, POSIX_FADV_WILLNEED);
		}
	}
	let mut buf = [0u8; 8192];
	let _ = file.read(&mut buf);
}

pub fn prefetch_path(root: &Path) -> i32 {
	let mut files = Vec::with_capacity(256);
	collect(&root.join("region"), &mut files);
	collect(&root.join("entities"), &mut files);
	collect(&root.join("poi"), &mut files);
	collect(&root.join("data"), &mut files);
	if files.is_empty() {
		collect(root, &mut files);
	}
	if files.is_empty() {
		return 0;
	}
	let workers = std::cmp::max(2, std::cmp::min(8, std::thread::available_parallelism().map(|n| n.get()).unwrap_or(2)));
	let chunk = (files.len() + workers - 1) / workers;
	thread::scope(|scope| {
		for slice in files.chunks(chunk) {
			scope.spawn(move || {
				for path in slice {
					touch(path);
				}
			});
		}
	});
	files.len() as i32
}

#[no_mangle]
pub extern "C" fn hsn_rs_prefetch_path(path: *const core::ffi::c_char) -> i32 {
	if path.is_null() {
		return 0;
	}
	let cstr = unsafe { core::ffi::CStr::from_ptr(path) };
	let Ok(text) = cstr.to_str() else {
		return 0;
	};
	prefetch_path(Path::new(text))
}
