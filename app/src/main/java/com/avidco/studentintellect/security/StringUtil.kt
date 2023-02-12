package com.avidco.studentintellect.security

import android.util.Base64
import java.nio.charset.StandardCharsets.UTF_8
import java.security.KeyPairGenerator
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.jvm.internal.Intrinsics

object StringUtil {



    fun String?.encryptString(encryptionKey: String): String? {
        return if (isNullOrEmpty()) null else
            try {
                val encodedKey = encryptionKey.toByteArray(UTF_8)
                Intrinsics.checkNotNullExpressionValue(encodedKey, "decode(encryptionKey, Base64.DEFAULT)")
                val secretKeySpec = SecretKeySpec(encodedKey, "AES")
                val ivParameterSpec = IvParameterSpec(encodedKey)

                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)

                val encryptedValue = cipher.doFinal(this.toByteArray(UTF_8))
                Base64.encodeToString(encryptedValue, 0)
            } catch (e: Exception) {
                println(e.message)
                null
            }
    }

    fun String?.decryptString(encryptionKey: String) : String? {
        return if (isNullOrEmpty()) null else
            try {
                val encodedKey = encryptionKey.toByteArray(UTF_8)
                Intrinsics.checkNotNullExpressionValue(encodedKey, "decode(encryptionKey, Base64.DEFAULT)")
                val secretKeySpec = SecretKeySpec(encodedKey, "AES")
                val ivParameterSpec = IvParameterSpec(encodedKey)

                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)

                val encryptedValue = Base64.decode(this, 0)
                val decryptedValue = cipher.doFinal(encryptedValue)
                Intrinsics.checkNotNullExpressionValue(decryptedValue, "decryptedValue")
                String(decryptedValue, UTF_8)
            } catch (e: Exception) {
                println(e.message)
                null
            }
    }
}