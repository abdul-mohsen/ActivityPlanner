package com.bignerdranch.android.activityplanner.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.bignerdranch.android.activityplanner.R
import timber.log.Timber

class MyArrayAdapter(
    context: Context,
    @LayoutRes val layoutResource: Int,
    private var x: List<String> = emptyList()
): ArrayAdapter<String>(context, layoutResource) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView
            ?: LayoutInflater.from(parent.context).inflate(
                layoutResource, parent, false
            )
        val z = view.findViewById<TextView>(R.id.auto_fill_option)
        z.text = getItem(position)

        return view
    }


    fun submit(list: List<String>) {
        Timber.d("I am called 1 $list")
        x = list
        clear()
        addAll(x)
        Timber.d("$x")
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults =
                FilterResults().apply {
                    values = x
                    count = x.size
                }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                Timber.d("I am called 2")
            }

            override fun convertResultToString(resultValue: Any?): CharSequence =
                resultValue.toString()
        }
    }
    override fun getItem(position: Int): String = x[position]
}