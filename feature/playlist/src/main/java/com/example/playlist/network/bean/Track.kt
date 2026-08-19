package com.example.playlist.network.bean

import com.google.gson.annotations.SerializedName

data class Track(
    @SerializedName("al")
    val album: Album?,
    @SerializedName("ar")
    val artists: List<Artist>?,
    @SerializedName("dt")
    val duration: Int,
    val id: Long,
    val name: String,
    val fee: Int
)

data class MySimpleSong(
    val id: Long,
    val name: String,
    val firstArtistName: String
)