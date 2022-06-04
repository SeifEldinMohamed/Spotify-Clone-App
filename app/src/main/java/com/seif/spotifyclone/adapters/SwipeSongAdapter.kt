package com.seif.spotifyclone.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.seif.spotifyclone.data.entities.Song
import com.seif.spotifyclone.databinding.SwipeRowItemBinding
import com.seif.spotifyclone.utils.DiffUtilCallBack

class SwipeSongAdapter : RecyclerView.Adapter<SwipeSongAdapter.MyViewHolder>() {

    var songs = emptyList<Song>()

    inner class MyViewHolder(private val binding: SwipeRowItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(song: Song) {
            binding.txtSongTitle.text = "${song.singer} - ${song.title}"

            binding.swipeConstraintLayout.setOnClickListener {
                onItemClickListener?.let {
                    it(song)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            SwipeRowItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(songs[position])
    }


    override fun getItemCount(): Int = songs.size

    private var onItemClickListener: ((Song) -> Unit)? =
        null // will takes song as a parameter and will not return anything

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