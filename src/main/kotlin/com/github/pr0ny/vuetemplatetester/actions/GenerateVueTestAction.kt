package com.github.pr0ny.vuetemplatetester.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.ui.Messages
import java.io.File

class GenerateVueTestAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val virtualFile: VirtualFile? = event.getData(CommonDataKeys.VIRTUAL_FILE)

        if (virtualFile == null || !virtualFile.name.endsWith(".vue")) {
            Messages.showErrorDialog("Please select a Vue (.vue) file.", "Invalid Selection")
            return
        }

        val fileContent = virtualFile.inputStream.bufferedReader().use { it.readText() }

        val regex = Regex("""data-test=["'](.*?)["']""")
        val matches = regex.findAll(fileContent).map { it.groupValues[1] }.toList()

        if (matches.isEmpty()) {
            Messages.showInfoMessage("No data-test attributes found.", "Nothing to Generate")
            return
        }

        val testFileContent = generateTestFile(virtualFile.nameWithoutExtension, matches)

        val specFileName = virtualFile.nameWithoutExtension + ".spec.ts"
        val parent = virtualFile.parent
        val specFile = File(parent.path, specFileName)

        specFile.writeText(testFileContent)

        Messages.showInfoMessage("Test file generated: ${specFile.name}", "Success")
    }

    private fun generateTestFile(componentName: String, dataTestAttributes: List<String>): String {
        val checks = dataTestAttributes.joinToString("\n") {
            """expect(wrapper.find('[data-test="$it"]').exists()).toBe(true);"""
        }

        return """
            import { shallowMount } from '@vue/test-utils';
            import ${componentName} from './${componentName}.vue';
            import { describe, it, expect } from 'vitest';

            describe('${componentName}', () => {
                it('should render data-test attributes', () => {
                    const wrapper = shallowMount(${componentName});
                    $checks
                });
            });
        """.trimIndent()
    }
}