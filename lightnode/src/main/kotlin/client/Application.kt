package client

import java.awt.GridLayout
import javax.swing.JButton
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
    panel.add(publickKeyFileLoadBT)

    panel.add(JLabel(" "))

    panel.add(JLabel("Private Key "))

    val privateKeyPathTF = JTextField("")
    privateKeyPathTF.isEditable = false
    panel.add(privateKeyPathTF)

    val privatekKeyFileLoadBT = JButton("Load...")
    panel.add(privatekKeyFileLoadBT)

    frame.add(panel)
    frame.isVisible = true
}