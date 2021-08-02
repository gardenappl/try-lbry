package ua.gardenapple.trylbry

data class Content(val type: ContentType, val originalUrl: String, var id: String? = null) {
}