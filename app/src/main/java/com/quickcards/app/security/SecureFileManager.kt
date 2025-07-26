package com.quickcards.app.security

import android.util.Base64
import com.google.gson.Gson
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class SecureFileManager private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: SecureFileManager? = null
        
        fun getInstance(): SecureFileManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecureFileManager().also { INSTANCE = it }
            }
        }
        
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
        private const val KEY_LENGTH = 256
        private const val FILE_HEADER = "QUICKCARDS_ENCRYPTED_V1"
        private const val FILE_EXTENSION = ".qcx"
    }
    
    private val encryptionHelper = EncryptionHelper.getInstance()
    
    /**
     * Encrypts export data using AES-GCM encryption
     */
    fun encryptExportData(jsonData: String): ByteArray {
        try {
            // Generate a random key for this export
            val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
            keyGenerator.init(KEY_LENGTH)
            val secretKey = keyGenerator.generateKey()
            
            // Generate random IV
            val iv = ByteArray(GCM_IV_LENGTH)
            SecureRandom().nextBytes(iv)
            
            // Encrypt the JSON data
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
            val encryptedData = cipher.doFinal(jsonData.toByteArray(Charsets.UTF_8))
            
            // Encrypt the key using the app's master key (from EncryptionHelper)
            val encryptedKey = encryptionHelper.encrypt(Base64.encodeToString(secretKey.encoded, Base64.DEFAULT))
            
            // Create the file structure
            val fileData = createEncryptedFileStructure(encryptedKey, iv, encryptedData)
            
            return fileData
        } catch (e: Exception) {
            throw SecurityException("Failed to encrypt export data: ${e.message}", e)
        }
    }
    
    /**
     * Decrypts import data
     */
    fun decryptImportData(encryptedFileData: ByteArray): String {
        try {
            // Parse the file structure
            val fileStructure = parseEncryptedFileStructure(encryptedFileData)
            
            // Decrypt the key using the app's master key
            val keyBytes = Base64.decode(
                encryptionHelper.decrypt(fileStructure.encryptedKey), 
                Base64.DEFAULT
            )
            val secretKey = SecretKeySpec(keyBytes, ALGORITHM)
            
            // Decrypt the data
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, fileStructure.iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)
            val decryptedData = cipher.doFinal(fileStructure.encryptedData)
            
            return String(decryptedData, Charsets.UTF_8)
        } catch (e: Exception) {
            throw SecurityException("Failed to decrypt import data: ${e.message}", e)
        }
    }
    
    /**
     * Validates if the file is a valid encrypted QuickCards file
     */
    fun isValidEncryptedFile(fileData: ByteArray): Boolean {
        return try {
            val headerBytes = FILE_HEADER.toByteArray(Charsets.UTF_8)
            if (fileData.size < headerBytes.size) return false
            
            // Check header
            val fileHeader = fileData.copyOfRange(0, headerBytes.size)
            fileHeader.contentEquals(headerBytes)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Gets file format information
     */
    fun getFileFormatInfo(): String {
        return "QuickCards Encrypted Export Format v1.0"
    }
    
    /**
     * Gets the secure file extension
     */
    fun getFileExtension(): String {
        return FILE_EXTENSION
    }
    
    private fun createEncryptedFileStructure(
        encryptedKey: String, 
        iv: ByteArray, 
        encryptedData: ByteArray
    ): ByteArray {
        val header = FILE_HEADER.toByteArray(Charsets.UTF_8)
        val keyBytes = encryptedKey.toByteArray(Charsets.UTF_8)
        val keyLength = keyBytes.size
        
        // File structure: [HEADER][KEY_LENGTH][KEY][IV][ENCRYPTED_DATA]
        val totalSize = header.size + 4 + keyLength + iv.size + encryptedData.size
        val fileData = ByteArray(totalSize)
        
        var offset = 0
        
        // Write header
        System.arraycopy(header, 0, fileData, offset, header.size)
        offset += header.size
        
        // Write key length (4 bytes)
        fileData[offset++] = (keyLength shr 24).toByte()
        fileData[offset++] = (keyLength shr 16).toByte()
        fileData[offset++] = (keyLength shr 8).toByte()
        fileData[offset++] = keyLength.toByte()
        
        // Write encrypted key
        System.arraycopy(keyBytes, 0, fileData, offset, keyLength)
        offset += keyLength
        
        // Write IV
        System.arraycopy(iv, 0, fileData, offset, iv.size)
        offset += iv.size
        
        // Write encrypted data
        System.arraycopy(encryptedData, 0, fileData, offset, encryptedData.size)
        
        return fileData
    }
    
    private fun parseEncryptedFileStructure(fileData: ByteArray): EncryptedFileStructure {
        val headerBytes = FILE_HEADER.toByteArray(Charsets.UTF_8)
        var offset = headerBytes.size
        
        // Read key length
        val keyLength = ((fileData[offset].toInt() and 0xFF) shl 24) or
                       ((fileData[offset + 1].toInt() and 0xFF) shl 16) or
                       ((fileData[offset + 2].toInt() and 0xFF) shl 8) or
                       (fileData[offset + 3].toInt() and 0xFF)
        offset += 4
        
        // Read encrypted key
        val encryptedKey = String(fileData.copyOfRange(offset, offset + keyLength), Charsets.UTF_8)
        offset += keyLength
        
        // Read IV
        val iv = fileData.copyOfRange(offset, offset + GCM_IV_LENGTH)
        offset += GCM_IV_LENGTH
        
        // Read encrypted data
        val encryptedData = fileData.copyOfRange(offset, fileData.size)
        
        return EncryptedFileStructure(encryptedKey, iv, encryptedData)
    }
    
    private data class EncryptedFileStructure(
        val encryptedKey: String,
        val iv: ByteArray,
        val encryptedData: ByteArray
    )
}
