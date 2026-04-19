package com.xiaofangathome.senior.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LegacyJsonMigrationTest {
    @Test
    fun `migrates legacy json when target file is blank`() {
        val targetFile = createTempFile().apply { writeText("") }

        val migrated = migrateLegacyJsonFileIfNeeded(
            legacyJson = """[{"id":"chat_1"}]""",
            targetFile = targetFile,
        )

        assertTrue(migrated)
        assertEquals("""[{"id":"chat_1"}]""", targetFile.readText())
    }

    @Test
    fun `does not overwrite existing target file`() {
        val targetFile = createTempFile().apply { writeText("""[{"id":"new"}]""") }

        val migrated = migrateLegacyJsonFileIfNeeded(
            legacyJson = """[{"id":"old"}]""",
            targetFile = targetFile,
        )

        assertFalse(migrated)
        assertEquals("""[{"id":"new"}]""", targetFile.readText())
    }

    @Test
    fun `ignores blank or invalid legacy payloads`() {
        val blankFile = createTempFile()
        val invalidFile = createTempFile()

        assertFalse(migrateLegacyJsonFileIfNeeded("", blankFile))
        assertFalse(migrateLegacyJsonFileIfNeeded("""{"id":"bad"}""", invalidFile))
        assertEquals("", blankFile.readText())
        assertEquals("", invalidFile.readText())
    }

    private fun createTempFile(): File {
        return kotlin.io.path.createTempFile(prefix = "legacy-migration", suffix = ".json").toFile().apply {
            deleteOnExit()
        }
    }
}
