package com.sanket.tools.nexpad

import com.sanket.tools.nexpad.runtime.plugin.NxprcDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NxprcAndroidDecodeTest {

    @Test
    fun testDecodeFileFromDesktop() {
        val file = File("C:\\Users\\parma\\OneDrive\\Desktop\\cyber_reactor_a.nxprc")
        if (file.exists()) {
            val bytes = file.readBytes()
            val result = NxprcDocument.decodeFromBytes(bytes)
            assertTrue("Android must decode the .nxprc file successfully: ${result.exceptionOrNull()?.message}", result.isSuccess)
            val doc = result.getOrThrow()
            assertEquals("rc.cyber_reactor_a", doc.manifest.id)
            assertEquals("Cyber Reactor A", doc.manifest.name)
            assertEquals(3, doc.canvas.layers.size)
        }
    }

    @Test
    fun testDecodeSanketNxprc() {
        val file = File("C:\\Users\\parma\\OneDrive\\Desktop\\sanket.nxprc")
        if (file.exists()) {
            val bytes = file.readBytes()
            val result = NxprcDocument.decodeFromBytes(bytes)
            assertTrue("Android must decode sanket.nxprc successfully: ${result.exceptionOrNull()?.message}", result.isSuccess)
            val doc = result.getOrThrow()
            assertEquals("rc.sanket_btn_a", doc.manifest.id)
            assertEquals("Sanket Realistic A", doc.manifest.name)
            assertEquals(5, doc.canvas.layers.size)
            println("Android successfully decoded sanket.nxprc with 5 layers!")
        }
    }
}
