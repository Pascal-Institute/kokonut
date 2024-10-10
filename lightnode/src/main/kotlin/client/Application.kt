package client

import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.JTextField

fun main() {
    val frame = JFrame()
    frame.title = "Kokonut Lightnode"
    frame.setSize(800, 600)

    val panel = JPanel()
    val layout = GridLayout(2,3)
    layout.hgap = 3
    layout.vgap = 3
    panel.layout = layout
    panel.add(JLabel("Public Key "))

    val publicKeyPathTF = JTextField("")
    publicKeyPathTF.isEditable = false
    panel.add(publicKeyPathTF)

    val publickKeyFileLoadBT = JButton("Load...")
    publickKeyFileLoadBT.addActionListener {
        val fileChooser = JFileChooser()

        fileChooser.dialogTitle = "Select Public Key File"

        val result = fileChooser.showOpenDialog(null)

        if (result == JFileChooser.APPROVE_OPTION) {
            val selectedFile = fileChooser.selectedFile
            val filePath = selectedFile.absolutePath
            publicKeyPathTF.text = filePath
        }
    }

    panel.add(publickKeyFileLoadBT)

    panel.add(JLabel(" "))

    panel.add(JLabel("Private Key "))

    val privateKeyPathTF = JTextField("")
    privateKeyPathTF.isEditable = false
    panel.add(privateKeyPathTF)

    val privatekKeyFileLoadBT = JButton("Load...")
    privatekKeyFileLoadBT.addActionListener {
        val fileChooser = JFileChooser()

        fileChooser.dialogTitle = "Select Private Key File"

        val result = fileChooser.showOpenDialog(null)

        if (result == JFileChooser.APPROVE_OPTION) {
            val selectedFile = fileChooser.selectedFile
            val filePath = selectedFile.absolutePath
            privateKeyPathTF.text = filePath
        }
    }
    panel.add(privatekKeyFileLoadBT)

    frame.add(panel)
    frame.isVisible = true
}