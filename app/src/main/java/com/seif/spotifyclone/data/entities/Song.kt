package com.seif.spotifyclone.data.entities

data class Song(
    val mediaId: String = "",
    val title: String = "",
    val singer: String = "",
//    val album: String = "",
//    val type: String = "",
//    val releasedYear: String = "",
    val songUrl: String = "",
    val imageUrl: String = "",
   // val album: String = ""
)
// variables names must be the same as in the fireStore, to avoid error when parsing these variables
