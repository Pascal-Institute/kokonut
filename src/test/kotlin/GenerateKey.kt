import kokonut.util.Wallet
import java.security.KeyPair

fun main() {
    val keyPair : KeyPair = Wallet.generateKey()
    Wallet.saveKeyPairToFile(keyPair, "C:\\Users\\public\\fullnode1\\private_key.pem", "C:\\Users\\public\\fullnode1\\public_key.pem")

    println("===================================================")
    println("private : ${keyPair.private}")
    println("public : ${keyPair.public}")
}