package com.bos.wms.mlkit.app.bosApp.models

    data class Result(
        val _id: String,
        val author: String,
        val authorSlug: String,
        val content: String,
        val dateAdded: String,
        val dateModified: String,
        val length: Int,
        val tags: List<String>
    )


