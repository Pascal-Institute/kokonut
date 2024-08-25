import kokonut.Wallet
import java.security.KeyPair

fun main() {
    val keyPair : KeyPair = Wallet.generateKey()
    Wallet.saveKeyPairToFile(keyPair, "C:\\Users\\public\\private_key.pem", "C:\\Users\\public\\public_key.pem")

    println("===================================================")
    println("private : ${keyPair.private}")
    println("public : ${keyPair.public}")
}