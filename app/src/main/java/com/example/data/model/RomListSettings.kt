package com.example.data.model

enum class RomListStyle {
    GRID,
    LIST,
    TEXT_ONLY,
    CAROUSEL
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
    val gridScalePercent: Int = 100,
    val showDetails: Boolean = true,
    val carouselIconSizePercent: Int = 100,
    val enableCarouselAnimation: Boolean = true,
    val enableCarouselUnlimited: Boolean = false
)
