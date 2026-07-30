package com.example.data.model

enum class RomListStyle {
    GRID,
    LIST,
    TEXT_ONLY
}

enum class TextAlignmentOption {
    START,
    CENTER,
    END
}

data class RomListSettings(
    val listStyle: RomListStyle = RomListStyle.GRID,
    val textSizeSp: Int = 16,
    val marginDp: Int = 8,
    val textAlignment: TextAlignmentOption = TextAlignmentOption.START,
    val showArtworkInTextOnly: Boolean = true,
    val gridScalePercent: Int = 100
)
