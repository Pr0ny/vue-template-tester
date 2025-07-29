package com.github.pr0ny.vuetemplatetester.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.github.pr0ny.vuetemplatetester.MyBundle
import com.github.pr0ny.vuetemplatetester.services.MyProjectService
import javax.swing.JButton

import com.github.pr0ny.vuetemplatetester.services.EditorMemoryService


class MyToolWindowFactory : ToolWindowFactory {

    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val memoryService = project.service<EditorMemoryService>()
        val initialContent = memoryService.loadContent()

        val panel = TextMemoryEditorPanel(
            project,
            initialImports = initialContent.imports,
            initialDataTest = initialContent.dataTest,
            initialSelector = initialContent.selector,
            initialConditional = initialContent.localPath,
            onSave = { content ->
                memoryService.saveContent(content)
            },
            onCancel = {
            }
        )
        val content = ContentFactory.getInstance().createContent(panel, "Settings", false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(toolWindow: ToolWindow) {

        private val service = toolWindow.project.service<MyProjectService>()

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            val label = JBLabel(MyBundle.message("randomLabel", "?"))

            add(label)
            add(JButton(MyBundle.message("shuffle")).apply {
                addActionListener {
                    label.text = MyBundle.message("randomLabel", service.getRandomNumber())
                }
            })
        }
    }
}
