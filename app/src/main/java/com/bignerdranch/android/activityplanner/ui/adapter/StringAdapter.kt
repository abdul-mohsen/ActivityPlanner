package com.bignerdranch.android.activityplanner.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bignerdranch.android.activityplanner.databinding.AutoFillItemBinding
import timber.log.Timber

class StringAdapter: ListAdapter<String, StringAdapter.MyViewHolder>(AutoFillDiffCallback()) {
    private lateinit var binding: AutoFillItemBinding
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        binding = AutoFillItemBinding.inflate(layoutInflater, parent, false)
        Timber.d("There is a create")
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind("test")
        Timber.d("I have been called")
    }

    inner class MyViewHolder(private val bindingHolder: AutoFillItemBinding):
        RecyclerView.ViewHolder(bindingHolder.root) {
        fun bind(option: String) { bindingHolder.autoFillOption.text = option }
    }

    class AutoFillDiffCallback: DiffUtil.ItemCallback<String>(){
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean =
            oldItem == newItem


        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
            oldItem == newItem
    }
}
