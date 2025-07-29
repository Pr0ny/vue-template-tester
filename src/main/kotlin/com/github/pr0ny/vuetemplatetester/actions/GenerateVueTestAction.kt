package com.github.pr0ny.vuetemplatetester.actions

import com.github.pr0ny.vuetemplatetester.services.EditorMemoryService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.platform.ide.progress.ModalTaskOwner.project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import java.io.File

class GenerateVueTestAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project: Project = event.project ?: return
        val virtualFileEvent: VirtualFile? = event.getData(CommonDataKeys.VIRTUAL_FILE)

        if (virtualFileEvent == null || !virtualFileEvent.name.endsWith(".vue")) {
            Messages.showErrorDialog("Please select a Vue (.vue) file.", "Invalid Selection")
            return
        }

        val fileContent = virtualFileEvent.inputStream.bufferedReader().use { it.readText() }

        val regex = Regex("""data-test=["'](.*?)["']""")
        val matches = regex.findAll(fileContent).map { it.groupValues[1] }.toList()

        if (matches.isEmpty()) {
            Messages.showInfoMessage("No data-test attributes found.", "Nothing to Generate")
            return
        }

        val testFileContent = generateTestFile(virtualFileEvent.nameWithoutExtension, matches, project, virtualFileEvent)

        val specFileName = virtualFileEvent.nameWithoutExtension + ".spec.ts"
        val parent = virtualFileEvent.parent ?: return

        // Création du fichier et écriture de son contenu
        val specFile = File(parent.path, specFileName)
        specFile.writeText(testFileContent)

        // Synchroniser avec le système de fichiers virtuel de l'IDE
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(specFile)
            ?: throw IllegalStateException("Cannot locate the created file!")

        WriteCommandAction.runWriteCommandAction(project) {
            // Rendre le fichier PsiFile compatible pour reformater
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                ?: throw IllegalStateException("Cannot create PsiFile for virtual file!")

            // Reformater le fichier avec CodeStyleManager
            CodeStyleManager.getInstance(project).reformat(psiFile)
        }

        Messages.showInfoMessage("Test file generated: ${specFile.name}", "Success")
    }


    private fun getUserImports(project: Project): String {
        return project.service<EditorMemoryService>().loadContent().imports
    }

    private fun getUserDataTest(project: Project): String {
        return project.service<EditorMemoryService>().loadContent().dataTest
    }

    private fun getUserSelector(project: Project): String {
        return project.service<EditorMemoryService>().loadContent().selector
    }

    private fun getImports(project: Project, componentName: String): String {
        val userImports = getUserImports(project)
        val defaultImports = """
            import { shallowMount } from '@vue/test-utils'
            import { flushPromises, mount } from '@vue/test-utils'
            import ${componentName} from './${componentName}.vue'
            import { beforeEach, describe, it, expect, vi } from 'vitest'
            import type { VueWrapper } from '@vue/test-utils'
        """.trimIndent()
        return  "${defaultImports}\n${userImports}\n\n"
    }

    private fun getWrapperCreator(componentName: String): String {
        return """
        vi.mock('@/folder/file', () => {
            return {
                default: vi.fn(),
            }
        })
            
        vi.mock('@/folder/Name', () => {
            const actual = await vi.importActual('@/folder/file')
            return {
                ...actual,
                name: {
                    functionName: () => Promise.resolve(),
                },
            }
        })
            
        const mocks = {}
        
        const createWrapper = (): VueWrapper => {
            return shallowMount(${componentName}, {
                global: {
                    plugins: [],
                    mocks,
                },
            })
        }

        let wrapper: VueWrapper<${componentName}> = createWrapper()
        
        """.trimIndent()
    }

    private fun getSelector(project: Project, attr: String): String {
        val selector = """[data-test="$attr"]"""
        val userSelector = getUserSelector(project)

        if (userSelector === "") {
            return selector
        }

        return userSelector.replace("\$attr", attr)
    }

    private fun getCheck(dataTest: String, name: String): String {
        return """
            it('should render $name attributes', () => {
            expect(wrapper.find('$dataTest').exists()).to.be.true
        })
        """.trimIndent()
    }

    private fun getDescribe(componentName: String, checks: String): String {
        return """describe('Should display ${componentName}', () => {
            beforeEach(() => {
                wrapper = createWrapper()
            })
            
            $checks
        })
        """.trimIndent()
    }

    private fun generateTestFile(componentName: String, dataTestAttributes: List<String>, project: Project, virtualFile: VirtualFile): String {
        val componentName = virtualFile.nameWithoutExtension
        val componentNameWithCapitalLetter = componentName.substring(0, 1).uppercase() + componentName.substring(1)
        val imports = getImports(project, componentName)
        val wrapper = getWrapperCreator(componentNameWithCapitalLetter)

        val checks = dataTestAttributes.joinToString("\n") { attr ->
            getCheck(getSelector(project, attr), attr)
        }

        val describe = getDescribe(componentNameWithCapitalLetter, checks)

        return imports + wrapper + describe
    }
}