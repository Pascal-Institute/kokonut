import kokonut.Utility
import java.security.KeyPair

fun main() {
    //very important! don't forgot it please!
    //val keyPair : KeyPair = Utility.generateKey()
    //It is using in kokonut!
    //val address = Utility.calculateHash(keyPair.public)

    //Utility.saveKeyPairToFile(keyPair, "C:\\Users\\public\\private_key.pem", "C:\\Users\\public\\public_key.pem")


    val address = Utility.calculateHash(Utility.loadPublicKey("C:\\Users\\public\\public_key.pem"))
    println("===================================================")
/*    println("private : ${keyPair.private}")
    println("public : ${keyPair.public}")*/
    println("address : $address")
}