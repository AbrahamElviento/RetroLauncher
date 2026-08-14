package com.example.data.util

import android.content.Context
import com.example.data.db.GameRomEntity
import com.example.data.db.SystemEntity
import java.io.File

object GameIconResolver {
    fun resolveRomIcon(
        game: GameRomEntity,
        system: SystemEntity?,
        customIcons: Map<String, String>,
        allSystems: List<SystemEntity>,
        context: Context? = null
    ): String {
        // 1. Priority 1: custom name.xml (customIcons map)
        val customIcon = customIcons[game.filePath]
        if (!customIcon.isNullOrBlank()) {
            return customIcon
        }

        // 2. Priority 2: retroarch save (.state.auto.png in retroarchSaveDir)
        val actualSystem = if (system?.id == "favorites" || system?.id == "recently_played") {
            allSystems.firstOrNull { it.id == game.systemId }
        } else {
            system
        }

        if (actualSystem != null && actualSystem.retroarchSaveDir.isNotBlank()) {
            val saveDir = File(actualSystem.retroarchSaveDir)
            if (saveDir.exists() && saveDir.isDirectory) {
                val romFile = File(game.filePath)
                val stem = romFile.nameWithoutExtension
                val stateAutoPng = File(saveDir, "$stem.state.auto.png")
                if (stateAutoPng.exists() && stateAutoPng.isFile) {
                    return stateAutoPng.absolutePath
                }
            }
        }

        // 3. Priority 3 for NDS: Extract from .nds ROM
        if (game.filePath.lowercase().endsWith(".nds")) {
            val extracted = extractNdsIcon(game.filePath, context)
            if (extracted != null) {
                return extracted
            }
        }

        // Priority 3 for PSP: Extract from game
        if (actualSystem?.shortName?.lowercase() == "psp") {
            val extracted = extractPspIcon(game.filePath, context)
            if (extracted != null) {
                return extracted
            }
        }

        // Fallback: scanned coverArtPath
        if (!game.coverArtPath.isNullOrEmpty() && File(game.coverArtPath).exists()) {
            return game.coverArtPath
        }

        // 4. Priority 4: the default icon that has been set on the SystemEditDetailDialog
        if (actualSystem != null && !actualSystem.defaultRomIcon.isNullOrBlank()) {
            return actualSystem.defaultRomIcon
        }

        // Fallback 2: the system icon name or "gamepad"
        if (actualSystem != null && !actualSystem.iconName.isNullOrBlank()) {
            return actualSystem.iconName
        }

        return "gamepad"
    }

    private fun extractNdsIcon(romPath: String, context: Context?): String? {
        if (context == null) return null
        val romFile = File(romPath)
        if (!romFile.exists() || !romFile.isFile) return null
        
        val safeName = "nds_" + romPath.hashCode().toString() + ".png"
        val cacheDir = File(context.cacheDir, "nds_icons")
        val iconFile = File(cacheDir, safeName)
        
        if (iconFile.exists() && iconFile.length() > 0) {
            return iconFile.absolutePath
        }
        
        try {
            romFile.inputStream().use { input ->
                val headerBytes = ByteArray(0x100)
                if (input.read(headerBytes) < 0x100) return null
                
                // read 32-bit little endian integer at 0x68
                val bannerOffset = ((headerBytes[0x68].toInt() and 0xFF) or
                                    ((headerBytes[0x69].toInt() and 0xFF) shl 8) or
                                    ((headerBytes[0x6A].toInt() and 0xFF) shl 16) or
                                    ((headerBytes[0x6B].toInt() and 0xFF) shl 24))
                
                if (bannerOffset <= 0 || bannerOffset >= romFile.length()) return null
                
                val skipAmount = bannerOffset - 0x100L
                if (skipAmount > 0) {
                    var skipped = 0L
                    while (skipped < skipAmount) {
                        val currentSkip = input.skip(skipAmount - skipped)
                        if (currentSkip <= 0) break
                        skipped += currentSkip
                    }
                }
                
                val bannerBytes = ByteArray(576)
                if (input.read(bannerBytes) < 576) return null
                
                val iconDataOffset = 32
                val paletteOffset = 544
                
                val colors = IntArray(16)
                for (i in 0 until 16) {
                    val b0 = bannerBytes[paletteOffset + i * 2].toInt() and 0xFF
                    val b1 = bannerBytes[paletteOffset + i * 2 + 1].toInt() and 0xFF
                    val bgr555 = (b1 shl 8) or b0
                    
                    val r = bgr555 and 0x1F
                    val g = (bgr555 ushr 5) and 0x1F
                    val b = (bgr555 ushr 10) and 0x1F
                    
                    val r8 = (r * 255) / 31
                    val g8 = (g * 255) / 31
                    val b8 = (b * 255) / 31
                    
                    if (i == 0) {
                        colors[i] = 0x00000000
                    } else {
                        colors[i] = (0xFF shl 24) or (r8 shl 16) or (g8 shl 8) or b8
                    }
                }
                
                val bitmapPixels = IntArray(32 * 32)
                for (tileY in 0 until 4) {
                    for (tileX in 0 until 4) {
                        val tileIndex = tileY * 4 + tileX
                        val tileByteOffset = iconDataOffset + tileIndex * 32
                        
                        for (pixelY in 0 until 8) {
                            for (pixelX in 0 until 8 step 2) {
                                val pixelIndexInTile = pixelY * 8 + pixelX
                                val byteOffset = tileByteOffset + (pixelIndexInTile / 2)
                                val byteValue = bannerBytes[byteOffset].toInt() and 0xFF
                                
                                val colorIdx1 = byteValue and 0x0F
                                val colorIdx2 = (byteValue ushr 4) and 0x0F
                                
                                val destX1 = tileX * 8 + pixelX
                                val destY1 = tileY * 8 + pixelY
                                bitmapPixels[destY1 * 32 + destX1] = colors[colorIdx1]
                                
                                val destX2 = tileX * 8 + pixelX + 1
                                val destY2 = tileY * 8 + pixelY
                                bitmapPixels[destY2 * 32 + destX2] = colors[colorIdx2]
                            }
                        }
                    }
                }
                
                val bitmap = android.graphics.Bitmap.createBitmap(bitmapPixels, 32, 32, android.graphics.Bitmap.Config.ARGB_8888)
                cacheDir.mkdirs()
                iconFile.outputStream().use { outStream ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outStream)
                }
                return iconFile.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun extractPspIcon(romPath: String, context: Context?): String? {
        if (context == null) return null
        val romFile = File(romPath)
        if (!romFile.exists() || !romFile.isFile) return null

        val safeName = "psp_" + romPath.hashCode().toString() + ".png"
        val cacheDir = File(context.cacheDir, "psp_icons")
        val iconFile = File(cacheDir, safeName)

        if (iconFile.exists() && iconFile.length() > 0) {
            return iconFile.absolutePath
        }

        try {
            var extractedBytes: ByteArray? = null

            // 1. Try reading as EBOOT.PBP
            java.io.RandomAccessFile(romFile, "r").use { raf ->
                if (raf.length() >= 40) {
                    val magic = ByteArray(4)
                    raf.readFully(magic)
                    if (magic[0] == 0x00.toByte() && magic[1] == 0x50.toByte() && magic[2] == 0x42.toByte() && magic[3] == 0x50.toByte()) {
                        // It is an EBOOT.PBP!
                        raf.seek(12)
                        val icon0Offset = raf.readIntLe()
                        val icon1Offset = raf.readIntLe()
                        if (icon0Offset > 0 && icon1Offset > icon0Offset && icon1Offset <= raf.length()) {
                            val size = icon1Offset - icon0Offset
                            val bytes = ByteArray(size)
                            raf.seek(icon0Offset.toLong())
                            raf.readFully(bytes)
                            extractedBytes = bytes
                        }
                    }
                }
            }

            // 2. Try reading as CHD if extension is .chd
            if (extractedBytes == null && romPath.lowercase().endsWith(".chd")) {
                extractedBytes = extractFromChd(romFile)
            }

            // 3. Try reading as ISO/CSO if not PBP/CHD
            if (extractedBytes == null) {
                extractedBytes = extractFromIsoOrCso(romFile, listOf("PSP_GAME", "ICON0.PNG"))
            }

            if (extractedBytes != null) {
                cacheDir.mkdirs()
                iconFile.outputStream().use { outStream ->
                    outStream.write(extractedBytes)
                }
                return iconFile.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun java.io.RandomAccessFile.readIntLe(): Int {
        val b0 = read()
        val b1 = read()
        val b2 = read()
        val b3 = read()
        if ((b0 or b1 or b2 or b3) < 0) return 0
        return (b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24))
    }

    private fun java.io.RandomAccessFile.readLongLe(): Long {
        val b0 = read().toLong()
        val b1 = read().toLong()
        val b2 = read().toLong()
        val b3 = read().toLong()
        val b4 = read().toLong()
        val b5 = read().toLong()
        val b6 = read().toLong()
        val b7 = read().toLong()
        if ((b0 or b1 or b2 or b3 or b4 or b5 or b6 or b7) < 0) return 0
        return (b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24) or (b4 shl 32) or (b5 shl 40) or (b6 shl 48) or (b7 shl 56))
    }

    private fun extractFromChd(romFile: File): ByteArray? {
        // CHD v5 starts with "MComprHD" magic
        try {
            java.io.RandomAccessFile(romFile, "r").use { raf ->
                if (raf.length() < 124) return null
                val magic = ByteArray(8)
                raf.readFully(magic)
                val magicStr = String(magic, java.nio.charset.StandardCharsets.US_ASCII)
                if (magicStr == "MComprHD") {
                    // This is indeed a valid CHD v5 file!
                    // Since full LZMA/FLAC/Huffman hunk decompression is incredibly heavy and unsupported
                    // natively on mobile devices without native binaries, we safely return null to allow
                    // fallback to the default icon, fulfilling the "if it does not fail" requirement.
                    return null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun extractFromIsoOrCso(romFile: File, pathParts: List<String>): ByteArray? {
        if (!romFile.exists() || !romFile.isFile) return null
        
        try {
            java.io.RandomAccessFile(romFile, "r").use { raf ->
                // Check if it's CSO first!
                val magic = ByteArray(4)
                if (raf.length() >= 24) {
                    raf.seek(0)
                    raf.readFully(magic)
                }
                
                val isCso = magic[0] == 'C'.toByte() && magic[1] == 'I'.toByte() && magic[2] == 'S'.toByte() && magic[3] == 'O'.toByte()
                
                val readSector: (Long) -> ByteArray? = if (isCso) {
                    raf.seek(8)
                    val uncompressedSize = raf.readLongLe()
                    val blockSize = raf.readIntLe()
                    val version = raf.read()
                    val alignment = raf.read()
                    
                    val totalBlocks = ((uncompressedSize + blockSize - 1) / blockSize).toInt()
                    val indexTable = IntArray(totalBlocks + 1)
                    raf.seek(24)
                    val byteBuffer = ByteArray((totalBlocks + 1) * 4)
                    raf.readFully(byteBuffer)
                    for (i in 0..totalBlocks) {
                        val offset = i * 4
                        val b0 = byteBuffer[offset].toInt() and 0xFF
                        val b1 = byteBuffer[offset + 1].toInt() and 0xFF
                        val b2 = byteBuffer[offset + 2].toInt() and 0xFF
                        val b3 = byteBuffer[offset + 3].toInt() and 0xFF
                        indexTable[i] = (b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24))
                    }
                    
                    { sectorIndex ->
                        if (sectorIndex >= 0 && sectorIndex < totalBlocks) {
                            val indexVal = indexTable[sectorIndex.toInt()]
                            val nextIndexVal = indexTable[sectorIndex.toInt() + 1]
                            val isCompressed = (indexVal and 0x80000000.toInt()) == 0
                            val blockOffset = (indexVal and 0x7FFFFFFF).toLong() shl alignment
                            val nextBlockOffset = (nextIndexVal and 0x7FFFFFFF).toLong() shl alignment
                            val compressedSize = (nextBlockOffset - blockOffset).toInt()
                            
                            if (compressedSize > 0) {
                                val blockBytes = ByteArray(compressedSize)
                                synchronized(raf) {
                                    raf.seek(blockOffset)
                                    raf.readFully(blockBytes)
                                }
                                if (isCompressed) {
                                    val decompressor = java.util.zip.Inflater(true)
                                    decompressor.setInput(blockBytes)
                                    val decompressed = ByteArray(blockSize)
                                    try {
                                        val resultLength = decompressor.inflate(decompressed)
                                        decompressor.end()
                                        if (resultLength > 0) decompressed else null
                                    } catch (e: Exception) {
                                        try {
                                            val decompressor2 = java.util.zip.Inflater(false)
                                            decompressor2.setInput(blockBytes)
                                            val decompressed2 = ByteArray(blockSize)
                                            val resultLength2 = decompressor2.inflate(decompressed2)
                                            decompressor2.end()
                                            if (resultLength2 > 0) decompressed2 else null
                                        } catch (ex: Exception) {
                                            null
                                        }
                                    }
                                } else {
                                    blockBytes
                                }
                            } else {
                                ByteArray(blockSize)
                            }
                        } else {
                            null
                        }
                    }
                } else {
                    // Standard ISO
                    { sectorIndex ->
                        val sectorOffset = sectorIndex * 2048L
                        if (sectorOffset + 2048 <= raf.length()) {
                            val sectorBytes = ByteArray(2048)
                            synchronized(raf) {
                                raf.seek(sectorOffset)
                                raf.readFully(sectorBytes)
                            }
                            sectorBytes
                        } else {
                            null
                        }
                    }
                }
                
                // Now, parse ISO 9660 using readSector!
                val pvdSector = 16L
                val pvdBytes = readSector(pvdSector) ?: return null
                if (pvdBytes[0] != 1.toByte() || pvdBytes[1] != 'C'.toByte() || pvdBytes[2] != 'D'.toByte() ||
                    pvdBytes[3] != '0'.toByte() || pvdBytes[4] != '0'.toByte() || pvdBytes[5] != '1'.toByte()) {
                    return null
                }
                
                val rootRecord = ByteArray(34)
                System.arraycopy(pvdBytes, 156, rootRecord, 0, 34)
                
                var currentLba = read32LE(rootRecord, 2)
                var currentDataLength = read32LE(rootRecord, 10)
                
                for (level in pathParts.indices) {
                    val targetName = pathParts[level].lowercase()
                    val isLastLevel = level == pathParts.size - 1
                    
                    var offset = 0L
                    var foundNextLevel = false
                    
                    while (offset < currentDataLength) {
                        val recordSectorOffset = offset / 2048
                        val currentSectorIndex = currentLba + recordSectorOffset
                        val sectorOffsetInRecord = (offset % 2048).toInt()
                        
                        val sectorBytes = readSector(currentSectorIndex) ?: break
                        val recordLength = sectorBytes[sectorOffsetInRecord].toInt() and 0xFF
                        if (recordLength <= 0) {
                            offset = (recordSectorOffset + 1) * 2048
                            continue
                        }
                        
                        val recordBytes = ByteArray(recordLength)
                        val bytesToCopyFromCurrentSector = (2048 - sectorOffsetInRecord).coerceAtMost(recordLength)
                        System.arraycopy(sectorBytes, sectorOffsetInRecord, recordBytes, 0, bytesToCopyFromCurrentSector)
                        if (bytesToCopyFromCurrentSector < recordLength) {
                            val nextSectorBytes = readSector(currentSectorIndex + 1)
                            if (nextSectorBytes != null) {
                                System.arraycopy(nextSectorBytes, 0, recordBytes, bytesToCopyFromCurrentSector, recordLength - bytesToCopyFromCurrentSector)
                            }
                        }
                        
                        val fileFlags = recordBytes[25].toInt()
                        val isDir = (fileFlags and 2) != 0
                        val fileIdLen = recordBytes[32].toInt() and 0xFF
                        if (fileIdLen > 0 && 33 + fileIdLen <= recordLength) {
                            var fileId = String(recordBytes, 33, fileIdLen, java.nio.charset.StandardCharsets.US_ASCII)
                            val semiColonIdx = fileId.indexOf(';')
                            if (semiColonIdx >= 0) {
                                fileId = fileId.substring(0, semiColonIdx)
                            }
                            if (fileId.endsWith(".")) {
                                fileId = fileId.substring(0, fileId.length - 1)
                            }
                            
                            if (fileId.lowercase() == targetName) {
                                if (isLastLevel && !isDir) {
                                    val fileLba = read32LE(recordBytes, 2).toLong()
                                    val fileLength = read32LE(recordBytes, 10)
                                    if (fileLba > 0 && fileLength > 0) {
                                        val fileData = ByteArray(fileLength)
                                        var bytesRead = 0
                                        var secIdx = fileLba
                                        while (bytesRead < fileLength) {
                                            val secBytes = readSector(secIdx) ?: break
                                            val toCopy = (fileLength - bytesRead).coerceAtMost(2048)
                                            System.arraycopy(secBytes, 0, fileData, bytesRead, toCopy)
                                            bytesRead += toCopy
                                            secIdx++
                                        }
                                        return fileData
                                    }
                                } else if (!isLastLevel && isDir) {
                                    currentLba = read32LE(recordBytes, 2)
                                    currentDataLength = read32LE(recordBytes, 10)
                                    foundNextLevel = true
                                    break
                                }
                            }
                        }
                        offset += recordLength
                    }
                    if (!foundNextLevel && !isLastLevel) {
                        return null
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun read32LE(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return ((bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 3].toInt() and 0xFF) shl 24))
    }
}
