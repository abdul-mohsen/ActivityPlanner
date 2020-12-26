package com.bignerdranch.android.activityplanner.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bignerdranch.android.activityplanner.R
import com.bignerdranch.android.activityplanner.databinding.AutoFillItemBinding
import com.bignerdranch.android.activityplanner.databinding.BusinessItemBinding
import com.bignerdranch.android.activityplanner.model.Business
import com.squareup.picasso.Picasso
import timber.log.Timber

class BusinessAdapter: ListAdapter<Business, BusinessAdapter.AutoFillViewHolder>(AutoFillDiffCallback()) {
    private lateinit var binding: BusinessItemBinding
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AutoFillViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        binding = BusinessItemBinding.inflate(layoutInflater, parent, false)
        Timber.d("There is a create")
        return AutoFillViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AutoFillViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(
            item.name,
            item.imageUrl,
            item.weather?.tempC.toString(),
            "(${item.reviewCount})",
            ratingToResourceId(item.rating),
            "https:${item.weather?.condition?.icon}"
        )
    }

    inner class AutoFillViewHolder(private val bindingHolder: BusinessItemBinding):
        RecyclerView.ViewHolder(bindingHolder.root) {

        fun bind(option: String, url: String, temp: String, reviewCount: String, imageResource: Int, weatherUrl: String) {
            bindingHolder.businessName.text = option
            bindingHolder.weatherTemp.text = "${temp}°C"
            bindingHolder.reviewCount.text = reviewCount
            bindingHolder.weatherImage
            Picasso.get().load(weatherUrl)
                .fit()
                .centerInside()
                .into(bindingHolder.weatherImage)
            bindingHolder.rateImage.setImageResource(imageResource)
            Picasso.get().load(url)
                .fit()
                .centerCrop()
                .into(bindingHolder.businessImage)
        }
        }

    class AutoFillDiffCallback: DiffUtil.ItemCallback<Business>(){
        override fun areItemsTheSame(oldItem: Business, newItem: Business): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Business, newItem: Business): Boolean =
            (oldItem == newItem).also { Timber.d("${oldItem.weather?.time}   ${newItem.weather?.time}") }
    }

    private fun ratingToResourceId(input: Float) = when(input){
        1.0f -> R.drawable.stars_small_0
        1.5f -> R.drawable.stars_small_1_half
        2.0f -> R.drawable.stars_small_2
        2.5f -> R.drawable.stars_small_2_half
        3.0f -> R.drawable.stars_small_3
        3.5f -> R.drawable.stars_small_3_half
        4.0f -> R.drawable.stars_small_4
        4.5f -> R.drawable.stars_small_4_half
        5.0f -> R.drawable.stars_small_4_half
        else -> R.drawable.stars_small_0
    }

}
