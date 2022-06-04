package com.seif.spotifyclone.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import com.bumptech.glide.RequestManager
import com.seif.spotifyclone.adapters.SwipeSongAdapter
import com.seif.spotifyclone.data.entities.Song
import com.seif.spotifyclone.databinding.ActivityMainBinding
import com.seif.spotifyclone.exoplayer.toSong
import com.seif.spotifyclone.utils.Status
import com.seif.spotifyclone.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()

    @Inject
    lateinit var swipeSongAdapter: SwipeSongAdapter

    @Inject
    lateinit var glide: RequestManager
    private var currentPlayingSong: Song? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        subscribeToObservers()
        binding.viewPagerSong.adapter = swipeSongAdapter
    }


    private fun switchViewPagerToCurrentSong(song: Song) {
        val newItemIndex = swipeSongAdapter.songs.indexOf(song) // save index of passed song
        if (newItemIndex != -1) { // check if this index is in the songs list (as if it's not then it will return -1)
            binding.viewPagerSong.currentItem = newItemIndex
            Log.d("main", newItemIndex.toString())
            currentPlayingSong = song
        }
    }

    private fun subscribeToObservers() {
        mainViewModel.mediaItems.observe(this) {
            it?.let { result ->
                when (result.status) {
                    Status.SUCCESS -> {
                        result.data?.let { songs -> // if data not equal null
                            swipeSongAdapter.addSongs(songs)
                            if (songs.isNotEmpty()) {
                                glide.load((currentPlayingSong ?: songs[0]).imageUrl)
                                    .into(binding.currentSongImageView)
                            }
                            switchViewPagerToCurrentSong(currentPlayingSong ?: return@observe) // just display the current song by default
                            Log.d("main", currentPlayingSong.toString())

                        }
                    }
                    Status.ERROR -> Unit
                    Status.LOADING -> Unit
                }
            }
        }
        // every time we got new information about the current playing song(if the song switches this observe will trigger)
        mainViewModel.currentPlayingSong.observe(this) {
            if (it == null) return@observe

            currentPlayingSong = it.toSong()
            glide.load(currentPlayingSong?.imageUrl).into(binding.currentSongImageView)
            switchViewPagerToCurrentSong(currentPlayingSong ?: return@observe)
            Log.d("main", currentPlayingSong.toString()+"   - current")

        }
    }
}
