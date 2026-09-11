package com.inweb.browser.privacy.lists

/**
 * One subscribable filter-list source (EasyList-family).
 *
 * `id` is the stable key used for cache files and status reports; it is
 * restricted to `[a-z0-9-]` so it can never escape the cache directory.
 */
data class FilterListSource(
    val id: String,
    val title: String,
    val downloadUrl: String,
    val enabled: Boolean = true,
) {
    init {
        require(id.matches(ID_PATTERN)) { "list id must match [a-z0-9][a-z0-9-]{0,63}: '$id'" }
        require(title.isNotBlank()) { "list title must not be blank" }
        require(downloadUrl.startsWith("https://") || downloadUrl.startsWith("http://")) {
            "download url must be http(s): '$downloadUrl'"
        }
    }

    companion object {
        private val ID_PATTERN = Regex("[a-z0-9][a-z0-9-]{0,63}")

        /** Canonical, subscription-free community lists (§10 defaults). */
        val EASYLIST = FilterListSource(
            id = "easylist",
            title = "EasyList",
            downloadUrl = "https://easylist.to/easylist/easylist.txt",
        )

        val EASYPRIVACY = FilterListSource(
            id = "easyprivacy",
            title = "EasyPrivacy",
            downloadUrl = "https://easylist.to/easylist/easyprivacy.txt",
        )

        val DEFAULTS = listOf(EASYLIST, EASYPRIVACY)
    }
}
