package utils

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.*

fun readResourceFileAsString(filename: String): String =
    object {}.javaClass.classLoader.getResource(filename)
        ?.readText()
        ?: throw FileNotFoundException("File $filename was not found in /main/resources")

fun readResourceFileAsProperties(filename: String): Properties {
    val properties = Properties()

    object {}.javaClass.classLoader.getResource(filename)
        ?.let { File(it.toURI()) }
        ?.let { FileInputStream(it) }
        ?.let { InputStreamReader(it, Charset.forName("UTF-8")) }
        ?.use { properties.load(it) }
        ?: throw FileNotFoundException("File $filename was not found in /main/resources")

    return properties
}