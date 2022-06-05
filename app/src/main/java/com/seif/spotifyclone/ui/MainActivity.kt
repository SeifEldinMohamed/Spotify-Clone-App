package com.seif.spotifyclone.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.RequestManager
import com.google.android.material.snackbar.Snackbar
import com.seif.spotifyclone.R
import com.seif.spotifyclone.adapters.SwipeSongAdapter
import com.seif.spotifyclone.data.entities.Song
import com.seif.spotifyclone.databinding.ActivityMainBinding
import com.seif.spotifyclone.exoplayer.isPlaying
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

    private var playBackState: PlaybackStateCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        subscribeToObservers()
        binding.viewPagerSong.adapter = swipeSongAdapter

        binding.viewPagerSong.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) { // called each time we swipe in our viewPager
                super.onPageSelected(position)
                if (playBackState?.isPlaying == true)
                    mainViewModel.playOrToggleSong(swipeSongAdapter.songs[position])
                else // pause state (not playing)
                    currentPlayingSong = swipeSongAdapter.songs[position]

            }
        })

        binding.playPauseImageView.setOnClickListener {
            currentPlayingSong?.let {
                mainViewModel.playOrToggleSong(it, true)
            }
        }

        swipeSongAdapter.setOnItemClickListener {
            findNavController(R.id.nav_host_fragment).navigate(R.id.globalActionToSongFragment)
        }

        findNavController(R.id.nav_host_fragment).addOnDestinationChangedListener{ _, destination, _ ->

                when(destination.id){
                    R.id.songFragment -> hideBottomBar()
                    R.id.homeFragment -> showBottomBar()
                    else -> showBottomBar()
                }

        }
    }

    private fun hideBottomBar(){
        binding.currentSongImageView.isVisible = false
        binding.viewPagerSong.isVisible = false
        binding.playPauseImageView.isVisible = false
    }
    private fun showBottomBar(){
        binding.currentSongImageView.isVisible = true
        binding.viewPagerSong.isVisible = true
        binding.playPauseImageView.isVisible = true
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
                            switchViewPagerToCurrentSong(
                                currentPlayingSong ?: return@observe
                            ) // just display the current song by default
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
            Log.d("main", currentPlayingSong.toString() + "   - current")

        }
        // will be called every time playBackState changes (pause, play, prepared)
        mainViewModel.playbackState.observe(this) {
            playBackState = it
            // update image view according to state
            binding.playPauseImageView.setImageResource(
                if (playBackState?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play
            )
        }
        // used this Event Class to prevent showing our snackBar twice if the screen is rotated
        mainViewModel.isConnected.observe(this) {
            it?.getContentIfNotHandled()?.let { result ->
                when (result.status) {
                    Status.ERROR -> {
                        Snackbar.make(
                            binding.mainConstraintLayout,
                            result.message ?: "unknown error occurred!!",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    else -> Unit // do nothing
                }
            }
        }

        mainViewModel.networkError.observe(this) {
            it?.getContentIfNotHandled()?.let { result ->
                when (result.status) {
                    Status.ERROR -> {
                        Snackbar.make(
                            binding.mainConstraintLayout,
                            result.message ?: "unknown error occurred!!",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    else -> Unit // do nothing
                }
            }
        }
    }
}
