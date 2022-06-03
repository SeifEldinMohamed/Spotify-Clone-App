package com.seif.spotifyclone.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.seif.spotifyclone.adapters.SongAdapter

import com.seif.spotifyclone.databinding.FragmentHomeBinding
import com.seif.spotifyclone.utils.Status
import com.seif.spotifyclone.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment @Inject constructor(
    private val songAdapter: SongAdapter
) : Fragment() {
    lateinit var binding: FragmentHomeBinding
    lateinit var viewModel: MainViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRecyclerView()
        subscribeToObservers()

    }

    private fun setUpRecyclerView() = binding.rvAllSongs.apply {
        adapter = songAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    private fun subscribeToObservers(){
        viewModel.mediaItems.observe(viewLifecycleOwner) { result ->
            when (result.status) {
                Status.SUCCESS -> {
                    binding.allSongsProgressBar.visibility = View.INVISIBLE
                    result.data?.let { songs ->
                        songAdapter.addSongs(songs)
                    }
                }
                Status.ERROR -> Unit
                Status.LOADING -> {
                    binding.allSongsProgressBar.visibility = View.VISIBLE
                }
            }
        }
    }
}