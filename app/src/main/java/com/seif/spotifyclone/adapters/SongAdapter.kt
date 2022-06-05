package com.seif.spotifyclone.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.seif.spotifyclone.data.entities.Song
import com.seif.spotifyclone.databinding.SongItemRowBinding
import com.seif.spotifyclone.utils.DiffUtilCallBack
import javax.inject.Inject

class SongAdapter @Inject constructor(
    private val glide: RequestManager
) : RecyclerView.Adapter<SongAdapter.MyViewHolder>() {

    private var songs = emptyList<Song>()

    inner class MyViewHolder(private val binding: SongItemRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(song: Song) {
            binding.txtTitle.text = song.title
            binding.txtSubtitle.text = song.singer
            glide.load(song.imageUrl).into(binding.songImageView)

            binding.constraintLayout.setOnClickListener {
                onItemClickListener?.let {
                    it(song)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            SongItemRowBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(songs[position])
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    private var onItemClickListener: ((Song) -> Unit)? = null // will takes song as a parameter and will not return anything

    fun setOnItemClickListener(listener: (Song) -> Unit) { // will takes song as a parameter and will not return anything
        onItemClickListener = listener
    }

    fun addSongs(newSongs: List<Song>) {
        val diffUtilCallBack = DiffUtilCallBack(this.songs, newSongs)
        val result = DiffUtil.calculateDiff(diffUtilCallBack)
        this.songs = newSongs
        result.dispatchUpdatesTo(this)
    }

}