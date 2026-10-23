enum class LevelWajah {
    PENUH, KEDIP, TENANG, SIDIK_JARI;
    companion object {
        fun dari(n: Int) = values().getOrElse(n - 1) { PENUH }
    }
}
