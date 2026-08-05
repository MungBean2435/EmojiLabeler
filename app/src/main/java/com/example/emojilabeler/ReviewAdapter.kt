package com.example.emojilabeler

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.emojilabeler.data.ImageItem

class ReviewAdapter(
    private val items: List<ImageItem>,
    private val onClick: (ImageItem) -> Unit
) : RecyclerView.Adapter<ReviewAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.thumb)
        val labels: TextView = view.findViewById(R.id.thumbLabels)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val it = items[position]
        holder.img.load(Uri.parse(it.source)) { crossfade(true) }
        holder.labels.text = it.labels.joinToString("")
        holder.itemView.setOnClickListener {
            val p = holder.bindingAdapterPosition
            if (p != RecyclerView.NO_POSITION) onClick(items[p])
        }
    }
}