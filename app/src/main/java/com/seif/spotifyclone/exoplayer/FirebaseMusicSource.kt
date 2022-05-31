package com.seif.spotifyclone.exoplayer

import com.seif.spotifyclone.exoplayer.State.*

// in this class we will make sure to get all songs from our fireStore database
// and convert song format to a format that we need for our service

class FirebaseMusicSource {

    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>() // can schedule actions that we want to perform when that music source finished
    private var state: State = STATE_CREATED
    set(value) { // we will check here if our music source if set set it to initialized or error so we know that it's finished
        if (value == STATE_INITIALIZED || value == STATE_ERROR){
            synchronized(onReadyListeners){// what happened inside this block will only be accessed from same thread
                field = value   //  field : current value of the state
                // loop over each of these lambda functions
                onReadyListeners.forEach {  lisitener ->
                    lisitener(state == STATE_INITIALIZED)

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
    STATE_CREATED,
    STATE_INITIALIZING, // before downloading our song
    STATE_INITIALIZED, // after downloading
    STATE_ERROR
}
