package com.seif.spotifyclone.utils

import androidx.recyclerview.widget.DiffUtil
import com.seif.spotifyclone.data.entities.Song

class DiffUtilCallBack(
   private val oldList: List<Song>,
   private val newList: List<Song>,
): DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition]== newList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}