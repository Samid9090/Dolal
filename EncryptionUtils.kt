package com.parentkidsapp.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64
import android.content.Context

class EncryptionUtils {
    
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "ParentKidsAppKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
        
        fun generateSecretKey(): SecretKey {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            return keyGenerator.generateKey()
        }
        
        fun getSecretKey(): SecretKey? {
            return try {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
                keyStore.load(null)
                keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            } catch (e: Exception) {
                null
            }
        }
        
        fun encrypt(data: String): String? {
            return try {
                val secretKey = getSecretKey() ?: generateSecretKey()
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                
                val iv = cipher.iv
                val encryptedData = cipher.doFinal(data.toByteArray())
                
                // Combine IV and encrypted data
                val combined = ByteArray(iv.size + encryptedData.size)
                System.arraycopy(iv, 0, combined, 0, iv.size)
                System.arraycopy(encryptedData, 0, combined, iv.size, encryptedData.size)
                
                Base64.encodeToString(combined, Base64.DEFAULT)
            } catch (e: Exception) {
                null
            }
        }
        
        fun decrypt(encryptedData: String): String? {
            return try {
                val secretKey = getSecretKey() ?: return null
                val combined = Base64.decode(encryptedData, Base64.DEFAULT)
                
                // Extract IV and encrypted data
                val iv = ByteArray(GCM_IV_LENGTH)
                val encrypted = ByteArray(combined.size - GCM_IV_LENGTH)
                System.arraycopy(combined, 0, iv, 0, iv.size)
                System.arraycopy(combined, iv.size, encrypted, 0, encrypted.size)
                
                val cipher = Cipher.getInstance(TRANSFORMATION)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                
                String(cipher.doFinal(encrypted))
            } catch (e: Exception) {
                null
            }
        }
        
        fun createEncryptedSharedPreferences(context: Context, fileName: String): android.content.SharedPreferences? {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                
                EncryptedSharedPreferences.create(
                    context,
                    fileName,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                null
            }
        }
        
        fun hashString(input: String): String {
            return try {
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                val hashBytes = digest.digest(input.toByteArray())
                hashBytes.joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                input
            }
        }
        
        fun generateRandomToken(): String {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
            return (1..32)
                .map { chars.random() }
                .joinToString("")
        }
    }
}

