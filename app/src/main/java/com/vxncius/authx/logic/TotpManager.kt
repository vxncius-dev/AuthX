package com.vxncius.authx.logic
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordGenerator
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordConfig
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import org.apache.commons.codec.binary.Base32
import java.util.concurrent.TimeUnit
object TotpManager {
    fun generateCode(secret: String, algorithm: String = "SHA1", digits: Int = 6, period: Int = 30): String {
        return try {
            val base32 = Base32()
            val secretBytes = base32.decode(secret.uppercase())
            val hmacAlgo = when (algorithm.uppercase()) {
                "SHA256" -> HmacAlgorithm.SHA256
                "SHA512" -> HmacAlgorithm.SHA512
                else -> HmacAlgorithm.SHA1
            }
            val config = TimeBasedOneTimePasswordConfig(
                codeDigits = digits,
                timeStep = period.toLong(),
                timeStepUnit = TimeUnit.SECONDS,
                hmacAlgorithm = hmacAlgo
            )
            val generator = TimeBasedOneTimePasswordGenerator(secretBytes, config)
            generator.generate(System.currentTimeMillis())
        } catch (e: Exception) {
            "Error"
        }
    }
    fun getTimeRemaining(period: Int = 30): Long {
        val currentTime = System.currentTimeMillis() / 1000
        return period - (currentTime % period)
    }
}

