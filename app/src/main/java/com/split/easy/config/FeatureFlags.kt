package com.split.easy.config

/** Switches for features that exist in code but aren't ready to show yet. */
object FeatureFlags {
    /**
     * The offline banner, "Not synced" badges and Profile's sync card (M2-Q5). Hidden until sync
     * exists in M4; before then every change would show as waiting forever.
     */
    const val SHOW_SYNC_STATUS = false
}
