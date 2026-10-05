package io.github.mumu12641.dolphin.service

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BookingRuntimeTest {
    @Test
    fun resolvesBundledRuntimeWithoutUsingWorkingDirectory() {
        val resources = Files.createTempDirectory("dolphin resources ")
        val booking = resources.resolve("booking")
        val python = booking.resolve("python/python.exe")
        val script = booking.resolve("src/main.py")
        try {
            Files.createDirectories(python.parent)
            Files.createDirectories(script.parent)
            Files.createFile(python)
            Files.createFile(script)

            val runtime = findBookingRuntime(resources.toString())
            assertEquals(python.toFile().canonicalFile, runtime.python)
            assertEquals(script.toFile().canonicalFile, runtime.script)
        } finally {
            Files.deleteIfExists(script)
            Files.deleteIfExists(python)
            Files.deleteIfExists(script.parent)
            Files.deleteIfExists(python.parent)
            Files.deleteIfExists(booking)
            Files.deleteIfExists(resources)
        }
    }

    @Test
    fun reportsMissingPackagedResources() {
        assertFailsWith<IllegalArgumentException> { findBookingRuntime(null) }
    }
}
