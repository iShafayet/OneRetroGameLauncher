package com.sayemshafayet.onereogamelauncher.domain

/** Synthetic library rows — negative IDs never collide with [SystemEntity] primary keys. */
enum class VirtualLibrarySystem(val id: Long, val displayName: String, val subtitle: String) {
    FAVORITES(-1L, "Favorites", "All favorites"),
    RECENT(-2L, "Recent", "Recently played"),
    ;

    companion object {
        fun fromId(id: Long): VirtualLibrarySystem? = entries.firstOrNull { it.id == id }

        fun isVirtual(id: Long): Boolean = id < 0L
    }
}
