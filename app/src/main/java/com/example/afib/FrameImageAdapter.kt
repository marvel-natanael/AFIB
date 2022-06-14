package com.example.afib

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.afib.databinding.ItemFrameBinding

class FrameImageAdapter(private val listBitmap: ArrayList<Bitmap>) : RecyclerView.Adapter<FrameImageAdapter.ViewHolder>() {
    class ViewHolder(private val binding : ItemFrameBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(bitmap: Bitmap){
            binding.frame.setImageBitmap(bitmap)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemFrameBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listBitmap[position])
    }

    override fun getItemCount(): Int = listBitmap.size
}