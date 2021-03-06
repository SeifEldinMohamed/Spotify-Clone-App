package com.seif.spotifyclone.exoplayer

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.*
import androidx.core.net.toUri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.seif.spotifyclone.data.entities.Song
import com.seif.spotifyclone.data.remote.MusicDatabase
import com.seif.spotifyclone.exoplayer.State.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

// in this class we will make sure to get all songs from our fireStore database
// and convert song format to a format that we need for our service

class FirebaseMusicSource @Inject constructor(
    private val musicDatabase: MusicDatabase
) {

    var songs = emptyList<MediaMetadataCompat>() // MediaMetadataCompat: Contains metadata about an item, such as the title, artist, etc.
    // gets all of our songs objects from firebase
    suspend fun fetchMetaData() {
        state = STATE_INITIALIZING
        fetchData()
        state = STATE_INITIALIZED
    }
    //  that is because you call state = STATE_INITIALIZED when fetching data which
    //  causes the setter method to be called and fires the listener with the initialized state

    private suspend fun fetchData() = withContext(Dispatchers.IO){
        val allSongs: List<Song> = musicDatabase.getAllSongs()
        songs = allSongs.map { song ->
            MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_ARTIST, song.singer)
                .putString(METADATA_KEY_MEDIA_ID, song.mediaId)
                .putString(METADATA_KEY_TITLE, song.title)
                .putString(METADATA_KEY_DISPLAY_TITLE, song.title)
                .putString(METADATA_KEY_DISPLAY_ICON_URI, song.imageUrl)
                .putString(METADATA_KEY_MEDIA_URI, song.songUrl)
                .putString(METADATA_KEY_ALBUM_ART_URI, song.imageUrl) // Album image
                .putString(METADATA_KEY_DISPLAY_SUBTITLE, song.singer)
                .putString(METADATA_KEY_DISPLAY_DESCRIPTION, song.singer)
               // .putString(METADATA_KEY_ALBUM, song.album)
//                .putString(METADATA_KEY_YEAR, song.releasedYear)
//                .putString(METADATA_KEY_GENRE, song.type)
             //   .putString(METADATA_KEY_ARTIST, song.singer)
             //   .putString(METADATA_KEY_ALBUM_ARTIST, song.singer)
                .build()
        }
    }

    // we need to make a concatenating music source (list of several single music source)
    fun asMediaSource(dataSourceFactory: DefaultDataSource.Factory): ConcatenatingMediaSource{
        val concatenatingMediaSource = ConcatenatingMediaSource()
        songs.forEach { song ->
            val mediaSource: ProgressiveMediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(song.getString(METADATA_KEY_MEDIA_URI)))
            concatenatingMediaSource.addMediaSource(mediaSource)
        }
        return concatenatingMediaSource
    }

    fun asMediaItems() = songs.map { song ->
        val desc = MediaDescriptionCompat.Builder()
            .setMediaUri(song.getString(METADATA_KEY_MEDIA_URI).toUri())
            .setTitle(song.description.title)
            .setSubtitle(song.description.subtitle)
            .setMediaId(song.description.mediaId)
            .setIconUri(song.description.iconUri)
            .build()
        MediaBrowserCompat.MediaItem(desc, FLAG_PLAYABLE)   // can be item as a song or browsable like a album, recommended section, plylist
    }.toMutableList()


    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>() // can schedule actions that we want to perform when that music source finished
    private var state: State = STATE_CREATED
    set(value) { // whenever we set the value of our state to something else so we will trigger this setter
        if (value == STATE_INITIALIZED || value == STATE_ERROR){ // we will check here if our music source if set set it to initialized or error so we know that it's finished
            synchronized(onReadyListeners){// what happened inside this block will only be accessed from same thread
                field = value   //  field : current value of the state
                // loop over each of these lambda functions
                onReadyListeners.forEach {  listener ->
                    listener(state == STATE_INITIALIZED)
                }
            }
        }
        else{
            field = value
        }
    }

    fun whenReady(action: (Boolean) -> Unit): Boolean { // that is the action we want to perform when this particular music source is ready
        if (state == STATE_CREATED || state == STATE_INITIALIZING){
            onReadyListeners+= action
            return false // not ready
        }
        else{
            action(state == STATE_INITIALIZED)
            return true // ready
        }
    }

}
enum class State{
    STATE_CREATED, // initial state
    STATE_INITIALIZING, // before downloading our song
    STATE_INITIALIZED, // after downloading
    STATE_ERROR
}

// media source = single song