package com.example.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object SlideshowManager {
    private val _isPlaying = MutableStateFlow(true)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _currentFolderPath = MutableStateFlow("")
    val currentFolderPath: StateFlow<String> = _currentFolderPath.asStateFlow()

    private val _imageFiles = MutableStateFlow<List<File>>(emptyList())
    val imageFiles: StateFlow<List<File>> = _imageFiles.asStateFlow()

    fun setFolderPath(path: String) {
        if (_currentFolderPath.value == path) return
        _currentFolderPath.value = path
        val dir = File(path)
        val files = if (dir.exists() && dir.isDirectory) {
            dir.listFiles { _, name ->
                val lower = name.lowercase()
                lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp")
            }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
        _imageFiles.value = files
        _currentIndex.value = 0
    }

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }

    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun next() {
        val files = _imageFiles.value
        if (files.isNotEmpty()) {
            _currentIndex.value = (_currentIndex.value + 1) % files.size
        }
    }

    fun prev() {
        val files = _imageFiles.value
        if (files.isNotEmpty()) {
            _currentIndex.value = (_currentIndex.value - 1 + files.size) % files.size
        }
    }

    fun setCurrentIndex(index: Int) {
        val files = _imageFiles.value
        if (files.isNotEmpty() && index in files.indices) {
            _currentIndex.value = index
        }
    }
}
