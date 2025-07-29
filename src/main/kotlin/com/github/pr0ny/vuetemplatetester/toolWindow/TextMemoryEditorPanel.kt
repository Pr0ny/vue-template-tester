package com.github.pr0ny.vuetemplatetester.toolWindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBCheckBox
import java.awt.BorderLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

data class EditorMemoryContent(
    val imports: String,
    val dataTest: String,
    val selector: String,
    val localPath: Boolean
)

class TextMemoryEditorPanel(
    project: Project,
    initialImports: String = "",
    initialDataTest: String = "",
    initialSelector: String = "",
    initialConditional: Boolean = false,
    private val onSave: (EditorMemoryContent) -> Unit,
    private val onCancel: () -> Unit
) : JBPanel<TextMemoryEditorPanel>(BorderLayout()) {

    private val importsField = EditorTextField(initialImports)
    private val dataTestField = EditorTextField(initialDataTest)
    private val selectorField = EditorTextField(initialSelector)
    private val checkBox = JBCheckBox("Local path to component", initialConditional)

    init {
        val form = JPanel(GridBagLayout())
        form.border = javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20)
        val c = GridBagConstraints()
        c.insets = Insets(4, 4, 4, 4)
        c.gridx = 0
        c.gridwidth = 2
        c.fill = GridBagConstraints.HORIZONTAL
        c.weightx = 1.0

        // Imports
        c.gridy = 0
        val importTitlePanel = JPanel()
        importTitlePanel.layout = BoxLayout(importTitlePanel, BoxLayout.X_AXIS)

        val importTitle = JLabel("Imports:")
        importTitle.font = importTitle.font.deriveFont(Font.BOLD)

        val infoLabelImport = JLabel(AllIcons.General.Information)
        infoLabelImport.toolTipText = "Add here your imports imports:\n" +
                "import { foo } from '@vue/bar'\n" +
                "import MyCoolFunction from '@/utils/MyCoolFunction.ts'"

        importTitlePanel.add(infoLabelImport)
        importTitlePanel.add(Box.createHorizontalStrut(5))
        importTitlePanel.add(importTitle)


        form.add(importTitlePanel, c)
        c.gridy = 1
        importsField.minimumSize = importsField.preferredSize
        importsField.preferredSize = java.awt.Dimension(0, importsField.font.size * 10 + 16)
        form.add(importsField, c)

        // data-test
//        c.gridy = 2
//        val dataTestTitle = JLabel("data-test:")
//        dataTestTitle.font = dataTestTitle.font.deriveFont(Font.BOLD)
//        form.add(dataTestTitle, c)
//        c.gridy = 3
//        dataTestField.minimumSize = dataTestField.preferredSize
//        dataTestField.preferredSize = java.awt.Dimension(0, dataTestField.font.size * 10 + 16)
//        form.add(dataTestField, c)

        // selector
        c.gridy = 2
        val selectorTitlePanel = JPanel()
        selectorTitlePanel.layout = BoxLayout(selectorTitlePanel, BoxLayout.X_AXIS)

        val selectorTitle = JLabel("Selector:")
        selectorTitle.font = selectorTitle.font.deriveFont(Font.BOLD)

        val infoLabelSelector = JLabel(AllIcons.General.Information)
        infoLabelSelector.toolTipText = "Add here the way you want to select your data-test (\$attr will be replaced by the real data-test during the generation):\n" +
                "MyCoolFunction(\$attr)\n"

        selectorTitlePanel.add(infoLabelSelector)
        selectorTitlePanel.add(Box.createHorizontalStrut(5))
        selectorTitlePanel.add(selectorTitle)

        form.add(selectorTitlePanel, c)



        c.gridy = 3
        selectorField.minimumSize = selectorField.preferredSize
        selectorField.preferredSize = java.awt.Dimension(0, selectorField.font.size * 10 + 16)
        form.add(selectorField, c)

        // Checkbox
        c.gridy = 6
        c.gridwidth = 2
        c.fill = GridBagConstraints.NONE
        c.anchor = GridBagConstraints.LINE_START
        form.add(checkBox, c)

        add(form, BorderLayout.CENTER)
        add(form, BorderLayout.NORTH)

        // Panel des boutons
        val buttonsPanel = JPanel()
        val saveButton = JButton("Save").apply {
            addActionListener {
                onSave(EditorMemoryContent(
                    imports = importsField.text,
                    dataTest = dataTestField.text,
                    selector = selectorField.text,
                    localPath = checkBox.isSelected
                ))
            }
        }
        val cancelButton = JButton("Cancel").apply {
            addActionListener { onCancel() }
        }
        buttonsPanel.add(cancelButton)
        buttonsPanel.add(saveButton)
        add(buttonsPanel, BorderLayout.SOUTH)
    }

    // Accesseurs/Mutateurs si besoin
    fun setContent(content: EditorMemoryContent) {
        importsField.text = content.imports
        dataTestField.text = content.dataTest
        selectorField.text = content.selector
        checkBox.isSelected = content.localPath
    }

    fun getContent(): EditorMemoryContent = EditorMemoryContent(
        imports = importsField.text,
        dataTest = dataTestField.text,
        selector = selectorField.text,
        localPath = checkBox.isSelected
    )
}