package cz.milan_kostak.altitude.model

enum class SortType(val id: Int) {
    TIME(1), NAME(2), ALTITUDE(3);

    companion object {
        fun getById(newId: Int): SortType {
            return values().single { it.id == newId }
        }
    }
}