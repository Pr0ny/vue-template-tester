package com.github.pr0ny.vuetemplatetester.utils

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object FileReaderUtil {

    fun readFileFromResources(resourcePath: String): String {
        val inputStream = javaClass.classLoader.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Resource not found: $resourcePath")

        return BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { it.readText() }
    }
}
