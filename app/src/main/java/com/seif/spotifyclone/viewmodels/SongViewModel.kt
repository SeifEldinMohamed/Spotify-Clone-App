package com.seif.spotifyclone.viewmodels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seif.spotifyclone.exoplayer.MusicService
import com.seif.spotifyclone.exoplayer.MusicServiceConnection
import com.seif.spotifyclone.exoplayer.currentPlaybackPosition
import com.seif.spotifyclone.utils.Constants.UPDATE_PLAYER_POSITION_INTERVAL
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SongViewModel @Inject constructor(
    musicServiceConnection: MusicServiceConnection
) : ViewModel() {

    private val playbackState = musicServiceConnection.playbackState

    private val _curSongDuration = MutableLiveData<Long>()
    val curSongDuration: LiveData<Long> = _curSongDuration // song duration in millisecond

    private val _curPlayerPosition = MutableLiveData<Long>() // at which millisecond the player is actually currently playing
    val curPlayerPosition: LiveData<Long> = _curPlayerPosition

    // we will run a coroutine that bound to this song's writing model's lifecycle and the coroutine will just continously update the values of two variable

    init {
        updateCurrentPlayerPosition()
    }

    private fun updateCurrentPlayerPosition() {
        viewModelScope.launch {
            while(true) { // not infinite loop bec the this coroutine will be cancelled when viewModel ended
                val pos = playbackState.value?.currentPlaybackPosition
                if(curPlayerPosition.value != pos) {
                    _curSongDuration.postValue(MusicService.currentSongDuration)
                    _curPlayerPosition.postValue(pos!!)

                }
                // important for cancellation of coroutine ( update seekbar by 10 times a second)
                delay(UPDATE_PLAYER_POSITION_INTERVAL) // delay 100L
            }
        }
    }
}





