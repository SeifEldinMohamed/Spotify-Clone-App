package com.seif.spotifyclone.ui.fragments

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.RequestManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.seif.spotifyclone.R
import com.seif.spotifyclone.data.entities.Song
import com.seif.spotifyclone.databinding.FragmentSongBinding
import com.seif.spotifyclone.exoplayer.isPlaying
import com.seif.spotifyclone.exoplayer.toSong
import com.seif.spotifyclone.utils.Status
import com.seif.spotifyclone.viewmodels.MainViewModel
import com.seif.spotifyclone.viewmodels.SongViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class PlayingSongFragment : BottomSheetDialogFragment() {

    @Inject
    lateinit var glide: RequestManager
    private lateinit var mainViewModel: MainViewModel
    private val songViewModel: SongViewModel by viewModels()
    private var currentPlayingSong: Song? = null
    lateinit var binding: FragmentSongBinding

    private var playbackState: PlaybackStateCompat? = null
    private var shouldUpdateSeekbar = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        Log.d("song","oncreteView")
        binding = FragmentSongBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        subscribeToObservers()
        Log.d("song","oncreteView")

        currentPlayingSong?.let {
            mainViewModel.playOrToggleSong(it, false)
        }



        binding.playPauseDetailImageView.setOnClickListener {
            currentPlayingSong?.let {
                mainViewModel.playOrToggleSong(it, true)
            }
        }

        binding.seekBar.setOnSeekBarChangeListener(object :SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser){
                    setCurPlayerTimeToTextView(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                shouldUpdateSeekbar = false
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    mainViewModel.seekTo(it.progress.toLong())
                    shouldUpdateSeekbar = true
                }
            }

        })
        binding.skipPreviousImageView.setOnClickListener {
            mainViewModel.skipToPreviousSong()
        }
        binding.skipNextImageView.setOnClickListener {
            mainViewModel.skipToNextSong()
        }

        binding.downArrowImageView.setOnClickListener {
            findNavController().navigateUp()
        }

    }

    private fun updateTitleAndSongImage(song: Song) {
        val title = song.title
        binding.txtSongName.text = title
        glide.load(song.imageUrl).into(binding.songDetailImageView)
    }

    private fun subscribeToObservers() {
        mainViewModel.mediaItems.observe(viewLifecycleOwner) {
            it?.let { result ->
                when (result.status) {
                    Status.SUCCESS -> {
                        result.data?.let { songs ->
                            if (currentPlayingSong == null && songs.isNotEmpty()) {
                                currentPlayingSong = songs[0]
                                updateTitleAndSongImage(songs[0])
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }

        mainViewModel.currentPlayingSong.observe(viewLifecycleOwner) {
            if (it == null) return@observe
            currentPlayingSong = it.toSong()
            updateTitleAndSongImage(currentPlayingSong!!)
        }

        // to consider when we pause the song from our notification so we want to update playPauseImageView
        mainViewModel.playbackState.observe(viewLifecycleOwner){
            playbackState = it
            binding.playPauseDetailImageView.setImageResource(
                if(playbackState?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play
            )
            binding.seekBar.progress = it?.position?.toInt() ?: 0
        }
       // for seekbar
        songViewModel.curPlayerPosition.observe(viewLifecycleOwner) {
            if(shouldUpdateSeekbar) {
                Log.d("song","crrentPlayerPosition ${it.toInt()}")
                setCurPlayerTimeToTextView(it)
                binding.seekBar.progress = it.toInt()
            }
        }
        songViewModel.curSongDuration.observe(viewLifecycleOwner) {
            Log.d("song","curSongDuration")

            binding.seekBar.max = it.toInt()
            val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
            binding.txtSongDuration.text = dateFormat.format(it)
        }
    }
    private fun setCurPlayerTimeToTextView(ms: Long) {
        val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
        binding.txtCurrentTime.text = dateFormat.format(ms)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("song", "destroyed")
    }

}