#include <atomic>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <filesystem>
#include <string>
#include <thread>
#include <vector>

#if defined(__linux__)
#include <fcntl.h>
#include <sys/stat.h>
#include <unistd.h>
#endif

namespace {

void touch_file(const std::filesystem::path& path) {
#if defined(__linux__)
	int fd = ::open(path.c_str(), O_RDONLY | O_CLOEXEC);
	if (fd < 0) {
		return;
	}
#if defined(POSIX_FADV_WILLNEED)
	posix_fadvise(fd, 0, 1 << 20, POSIX_FADV_WILLNEED);
#endif
	alignas(32) unsigned char buf[8192];
	ssize_t n = ::read(fd, buf, sizeof(buf));
	(void)n;
	::close(fd);
#else
	(void)path;
#endif
}

void collect(const std::filesystem::path& dir, std::vector<std::filesystem::path>& out) {
	std::error_code ec;
	if (!std::filesystem::is_directory(dir, ec)) {
		return;
	}
	for (auto it = std::filesystem::directory_iterator(dir, ec);
	     it != std::filesystem::directory_iterator() && out.size() < 4096;
	     it.increment(ec)) {
		if (ec) {
			break;
		}
		if (it->is_regular_file(ec)) {
			out.push_back(it->path());
		}
	}
}

} // namespace

extern "C" __attribute__((visibility("default")))
int hsn_cpp_prefetch_path(const char* path) {
	if (!path || !path[0]) {
		return 0;
	}
	std::filesystem::path root(path);
	std::vector<std::filesystem::path> files;
	files.reserve(256);
	collect(root / "region", files);
	collect(root / "entities", files);
	collect(root / "poi", files);
	collect(root / "data", files);
	if (files.empty()) {
		collect(root, files);
	}
	if (files.empty()) {
		return 0;
	}
	unsigned workers = std::max(2u, std::min(8u, std::thread::hardware_concurrency()));
	std::atomic<size_t> next{0};
	std::vector<std::thread> pool;
	pool.reserve(workers);
	for (unsigned w = 0; w < workers; ++w) {
		pool.emplace_back([&]() {
			for (;;) {
				size_t i = next.fetch_add(1, std::memory_order_relaxed);
				if (i >= files.size()) {
					break;
				}
				touch_file(files[i]);
			}
		});
	}
	for (auto& t : pool) {
		t.join();
	}
	return static_cast<int>(files.size());
}

