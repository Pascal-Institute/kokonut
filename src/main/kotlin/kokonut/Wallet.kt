package kokonut

import kokonut.Utility.Companion.calculateHash
import kokonut.Utility.Companion.signData
import kokonut.Utility.Companion.verifySignature
import java.io.File

class Wallet(val privateKeyFile : File, val publicKeyFile : File) {

    private val publicKey = Utility.loadPublicKey(publicKeyFile.path)
    private val privateKey = Utility.loadPrivateKey(privateKeyFile.path)
    private var isValid = false

    var miner = "0000000000000000000000000000000000000000000000000000000000000000"

    init {
        val data = miner.toByteArray()
        val signature = signData(data, privateKey)

        isValid = verifySignature(data, signature, publicKey)

        if(isValid){
            miner = calculateHash(publicKey)
            println("Wallet is Valid")
            println("Miner : $miner")
        }else{
            println("Wallet is Invalid")
            println("Miner : $miner")
        }
    }

    fun isValid() : Boolean{
        return isValid
    }
}