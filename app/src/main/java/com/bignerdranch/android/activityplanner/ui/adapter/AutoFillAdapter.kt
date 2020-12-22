package com.bignerdranch.android.activityplanner.ui.adapter

import android.media.Image
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bignerdranch.android.activityplanner.databinding.AutoFillItemBinding

class AutoFillAdapter: ListAdapter<String, AutoFillAdapter.AutoFillViewHolder>(AutoFillDiffCallback()) {
    private lateinit var binding: AutoFillItemBinding
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AutoFillViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        binding = AutoFillItemBinding.inflate(layoutInflater, parent, false)
        return AutoFillViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AutoFillViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AutoFillViewHolder(private val bindingHolder: AutoFillItemBinding):
        RecyclerView.ViewHolder(bindingHolder.root) {

        fun bind(option: String) {
            bindingHolder.autoFillOption.text = option
        }
        }

    class AutoFillDiffCallback: DiffUtil.ItemCallback<String>(){
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean =
            oldItem == newItem

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
            oldItem == newItem


    }




}
