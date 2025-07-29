package com.github.pr0ny.vuetemplatetester.services

import com.intellij.openapi.components.Service
import com.github.pr0ny.vuetemplatetester.toolWindow.EditorMemoryContent
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

@Service
class EditorMemoryService {
    private val objectMapper = jacksonObjectMapper()
    var savedJson: String = ""

    fun saveContent(content: EditorMemoryContent) {
        savedJson = objectMapper.writeValueAsString(content)
    }

    fun loadContent(): EditorMemoryContent {
        return if (savedJson.isBlank()) {
            EditorMemoryContent(
                imports = "",
                dataTest = "",
                selector = "",
                localPath = false
            )
        } else {
            try {
                objectMapper.readValue(savedJson)
            } catch (ex: Exception) {
                EditorMemoryContent(
                    imports = "",
                    dataTest = "",
                    selector = "",
                    localPath = false
                )
            }
        }
    }
}