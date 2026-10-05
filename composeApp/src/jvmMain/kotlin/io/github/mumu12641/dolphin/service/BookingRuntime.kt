package io.github.mumu12641.dolphin.service

import java.io.File

internal data class BookingRuntime(
    val root: File,
    val python: File,
    val script: File
)

internal fun findBookingRuntime(resourcesDirectory: String?): BookingRuntime {
    require(!resourcesDirectory.isNullOrBlank()) {
        "未找到应用资源目录，请通过 Gradle run 或安装后的 Dolphin 启动。"
    }
    val root = File(resourcesDirectory, "booking").canonicalFile
    val python = File(root, "python/python.exe")
    val script = File(root, "src/main.py")
    require(python.isFile && script.isFile) {
        "内置预约程序不完整，请重新安装 Dolphin：$root"
    }
    return BookingRuntime(root, python, script)
}
