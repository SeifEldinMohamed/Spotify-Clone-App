package com.seif.spotifyclone.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.seif.spotifyclone.data.entities.Song
import com.seif.spotifyclone.utils.Constants.SONG_COLLECTION
import kotlinx.coroutines.tasks.await

class MusicDatabase {
    private val fireStore = FirebaseFirestore.getInstance()
    private val songCollection = fireStore.collection(SONG_COLLECTION)

    suspend fun getAllSongs(): List<Song> {
        return try {
            songCollection.get().await().toObjects(Song::class.java)
        } catch (e: Exception) { // if there is an error return an empty list
            emptyList()
        }
    }
}

// await() : gives us an object of type any
// Awaits the completion of the task without blocking a thread.
// This suspending function is cancellable. If the Job of the current coroutine is cancelled
// or completed while this suspending function is waiting, this function stops waiting for
// the completion stage and immediately resumes with CancellationException.

// toObjects(): specify the class of a specific item in our list