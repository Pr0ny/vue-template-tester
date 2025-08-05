package com.github.pr0ny.vuetemplatetester.actions

import com.github.pr0ny.vuetemplatetester.services.EditorMemoryService
import com.github.pr0ny.vuetemplatetester.utils.FileReaderUtil
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
import java.io.IOException

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

    private fun geLocalPath(project: Project): Boolean {
        return project.service<EditorMemoryService>().loadContent().localPath
    }

    private fun getUserSelector(project: Project): String {
        return project.service<EditorMemoryService>().loadContent().selector
    }

    private fun getImports(project: Project, componentName: String, virtualFile: VirtualFile): String {
        val userImports = getUserImports(project)
        val localPath = geLocalPath(project)
        val componentPath = virtualFile.path
        var componantImport = """import $componentName from './${componentName}.vue'"""

        if (localPath == false) {
            val srcIndex = componentPath.indexOf("/src/")

            if (srcIndex != -1) {
                val relativePath = componentPath.substring(srcIndex + 5) // +5 pour ignorer "/src/"
                componantImport = """import $componentName from '@/${relativePath}'"""
            } else {
                Messages.showErrorDialog("An error occurred. Using local path to import the component.", "Error")
            }
        }

        try {
            val snippetPath = "snippets/imports.txt"
            val snippetContent = FileReaderUtil.readFileFromResources(snippetPath)
            val textWithReplacedAttr = snippetContent.replace("\$attr", componantImport)

            return "${textWithReplacedAttr}\n${userImports}\n\n"
        } catch (e: Exception) {
                e.printStackTrace()
                Messages.showErrorDialog("Failed to read the snippet file: ${e.localizedMessage}", "Error")
        }

        return  "${userImports}\n\n"
    }

    private fun getWrapperCreator(componentName: String): String {
        return """
        vi.mock('@/folder/file', () => {
            return {
                default: vi.fn(),
            }
        })
            
        vi.mock('@/folder/Name', async () => {
            const actual = await vi.importActual('@/folder/file')
            return {
                ...actual,
                name: {
                    functionName: async () => Promise.resolve(),
                },
            }
        })
            
        const mocks = {}
        
        const props: MyPropsType = {}
        
        // Replace props type MyPropsType here and above to fit with real model
        const createWrapper = (testprops: Partial<MyPropsType> = {}): VueWrapper => {
            const finalProps = {
                ...props,
                ...testprops,
            }

            return shallowMount(${componentName}, {
                props: finalProps,
                global: {
                    plugins: [],
                    mocks,
                },
            })
        }

        let wrapper: VueWrapper<${componentName}> = createWrapper()
        
        """.trimIndent()
    }

    private fun addQuote(str: String): String {
        return "'" + str + "'"
    }

    private fun getSelector(project: Project, attr: String): String {
        val selector = """'[data-test="$attr"]'"""
        val userSelector = getUserSelector(project)

        if (userSelector === "") {
            return selector
        }

        return userSelector.replace("\$attr", addQuote(attr))
    }

    private fun getCheck(dataTest: String, name: String): String {
        return """
            it('should render $name attribute', () => {
            const element = wrapper.find($dataTest)
            
            expect(element.exists()).to.be.true
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
        val imports = getImports(project, componentName, virtualFile)
        val wrapper = getWrapperCreator(componentNameWithCapitalLetter)

        val checks = dataTestAttributes.joinToString("\n") { attr ->
            getCheck(getSelector(project, attr), attr)
        }

        val describe = getDescribe(componentNameWithCapitalLetter, checks)

        return imports + wrapper + describe
    }
}