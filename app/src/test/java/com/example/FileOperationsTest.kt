package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.SortDirection
import com.example.data.model.SortOption
import com.example.data.repository.FileOperationsRepository
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FileOperationsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var repo: FileOperationsRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repo = FileOperationsRepository(context)
    }

    @Test
    fun `create folder file rename copy move and delete`() = runTest {
        val root = tempFolder.newFolder("workspace")
        val createDirResult = repo.createFolder(root.absolutePath, isSafUri = false, name = "Docs")
        assertTrue(createDirResult.isSuccess)
        val docsDir = File(root, "Docs")
        assertTrue(docsDir.exists() && docsDir.isDirectory)

        val createFileResult = repo.createEmptyFile(docsDir.absolutePath, isSafUri = false, name = "hello.txt")
        assertTrue(createFileResult.isSuccess)
        val helloFile = File(docsDir, "hello.txt")
        helloFile.writeText("Onyx AMOLED test payload")

        val listed = repo.listDirectoryStream(
            path = docsDir.absolutePath,
            isSafUri = false,
            showHidden = false,
            sortOption = SortOption.NAME,
            sortDirection = SortDirection.ASCENDING
        ).last()
        assertEquals(1, listed.size)
        val item = listed.first()
        assertEquals("hello.txt", item.name)

        // Rename
        assertTrue(repo.renameItem(item, "renamed.txt").isSuccess)
        val renamedFile = File(docsDir, "renamed.txt")
        assertTrue(renamedFile.exists())
        assertFalse(helloFile.exists())

        // Copy to another folder
        val destDir = File(root, "Backup").apply { mkdirs() }
        val renamedListed = repo.listDirectoryStream(
            path = docsDir.absolutePath,
            isSafUri = false,
            showHidden = false,
            sortOption = SortOption.NAME,
            sortDirection = SortDirection.ASCENDING
        ).last()
        repo.pasteItems(renamedListed, destDir.absolutePath, isDestinationSaf = false, isMove = false).toList()
        assertTrue(File(destDir, "renamed.txt").exists())
        assertTrue(renamedFile.exists())

        // Properties & SHA-256
        val props = repo.getFileProperties(renamedListed.first(), includeChecksum = true)
        assertNotNull(props.sha256)
        assertEquals(64, props.sha256!!.length)

        // Delete
        val deleteResult = repo.deleteItems(renamedListed)
        assertTrue(deleteResult.isSuccess)
        assertFalse(renamedFile.exists())
    }

    @Test
    fun `zip compression and extraction round trip`() = runTest {
        val root = tempFolder.newFolder("zip_workspace")
        val srcDir = File(root, "pack").apply { mkdirs() }
        File(srcDir, "a.txt").writeText("Alpha")
        File(srcDir, "sub").apply { mkdirs() }.let { File(it, "b.txt").writeText("Beta") }

        val items = repo.listDirectoryStream(
            path = root.absolutePath,
            isSafUri = false,
            showHidden = false,
            sortOption = SortOption.NAME,
            sortDirection = SortDirection.ASCENDING
        ).last()

        repo.compressToZip(items, root.absolutePath, "bundle.zip").toList()
        val zipFile = File(root, "bundle.zip")
        assertTrue(zipFile.exists() && zipFile.length() > 0L)

        val extractTarget = File(root, "extracted").apply { mkdirs() }
        val zipItem = repo.listDirectoryStream(
            path = root.absolutePath,
            isSafUri = false,
            showHidden = false,
            sortOption = SortOption.NAME,
            sortDirection = SortDirection.ASCENDING
        ).last().first { it.name == "bundle.zip" }

        repo.extractZip(zipItem, extractTarget.absolutePath).toList()
        assertTrue(File(extractTarget, "bundle/pack/a.txt").exists())
        assertEquals("Beta", File(extractTarget, "bundle/pack/sub/b.txt").readText())
    }

    @Test
    fun `large directory with 2500 files streams incrementally`() = runTest {
        val bigDir = tempFolder.newFolder("large_dir")
        repeat(2500) { idx ->
            File(bigDir, "file_${idx.toString().padStart(4, '0')}.dat").createNewFile()
        }

        val emissions = repo.listDirectoryStream(
            path = bigDir.absolutePath,
            isSafUri = false,
            showHidden = false,
            sortOption = SortOption.NAME,
            sortDirection = SortDirection.ASCENDING
        ).toList()

        assertTrue("Expected incremental chunked emissions", emissions.size > 1)
        assertEquals(2500, emissions.last().size)
    }

    @Test
    fun `deep recursive search finds nested files`() = runTest {
        val root = tempFolder.newFolder("search_root")
        val nested = File(root, "level1/level2").apply { mkdirs() }
        File(nested, "onyx_secret_report.pdf").writeText("data")
        File(root, "other.txt").writeText("noise")

        val results = repo.searchFiles(
            query = "secret_report",
            rootPath = root.absolutePath,
            isSafUri = false,
            showHidden = false,
            deepSearch = true
        ).last()

        assertEquals(1, results.size)
        assertEquals("onyx_secret_report.pdf", results.first().name)
    }
}
