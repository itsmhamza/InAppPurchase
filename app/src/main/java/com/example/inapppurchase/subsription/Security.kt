package com.tahir.videodownloader.subsription

import android.text.TextUtils
import android.util.Base64
import android.util.Log
import java.io.IOException
import java.security.*
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec

/**
 * Security-related methods. For a secure implementation, all of this code should be implemented on
 * a server that communicates with the application on the device.
 */
object Security {
    private val TAG = "IABUtil/Security"
    private val KEY_FACTORY_ALGORITHM = "RSA"
    private val SIGNATURE_ALGORITHM = "SHA1withRSA"

    val BASE_64_ENCODED_PUBLIC_KEY = "" // write your key

    @Throws(IOException::class)
    fun verifyPurchaseKey(base64PublicKey: String, signedData: String, signature: String): Boolean {
        if ((TextUtils.isEmpty(signedData) || TextUtils.isEmpty(base64PublicKey)
                    || TextUtils.isEmpty(signature))
        ) {
            Log.w(TAG, "Purchase verification failed: missing data.")
            return false
        }
        val key =
            createPublicKey(
                base64PublicKey
            )
        return verifyKey(
            key,
            signedData,
            signature
        )
    }

    @Throws(IOException::class)
    private fun createPublicKey(encodedPublicKey: String): PublicKey {
        try {
            val decodedKey = Base64.decode(encodedPublicKey, Base64.DEFAULT)
            val keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
            return keyFactory.generatePublic(X509EncodedKeySpec(decodedKey))
        } catch (e: NoSuchAlgorithmException) {
            // "RSA" is guaranteed to be available.
            throw RuntimeException(e)
        } catch (e: InvalidKeySpecException) {
            val msg = "Invalid key specification: $e"
            Log.w(TAG, msg)
            throw IOException(msg)
        }
    }

    private fun verifyKey(publicKey: PublicKey, signedData: String, signature: String): Boolean {
        val signatureBytes: ByteArray
        try {
            signatureBytes = Base64.decode(signature, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Base64 decoding failed.")
            return false
        }
        try {
            val signatureAlgorithm = Signature.getInstance(SIGNATURE_ALGORITHM)
            signatureAlgorithm.initVerify(publicKey)
            signatureAlgorithm.update(signedData.toByteArray())
            if (!signatureAlgorithm.verify(signatureBytes)) {
                Log.w(TAG, "Signature verification failed...")
                return false
            }
            return true
        } catch (e: NoSuchAlgorithmException) {
            // "RSA" is guaranteed to be available.
            throw RuntimeException(e)
        } catch (e: InvalidKeyException) {
            Log.w(TAG, "Invalid key specification.")
        } catch (e: SignatureException) {
            Log.w(TAG, "Signature exception.")
        }
        return false
    }
}