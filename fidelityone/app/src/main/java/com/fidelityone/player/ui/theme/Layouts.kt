package com.fidelityone.player.ui.theme

import com.fidelityone.player.domain.model.ColorTheme
import com.fidelityone.player.domain.model.LayoutBase
import com.fidelityone.player.domain.model.VisualPreset

/**
 * Catálogo dos 20 presets visuais: 5 layouts-base × 4 temas de cor.
 * Exposto na tela "Temas" para o usuário escolher.
 */
object PresetCatalog {
    val layouts = LayoutBase.entries.toList()
    val themes = ColorTheme.entries.toList()

    /** Os 20 presets, ordenados por layout e depois cor. */
    val all: List<VisualPreset> = layouts.flatMap { l -> themes.map { t -> VisualPreset(l, t) } }

    fun find(id: String): VisualPreset? = all.firstOrNull { it.id == id }
}
