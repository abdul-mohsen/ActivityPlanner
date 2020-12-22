package com.bignerdranch.android.activityplanner.model

data class AutoComplete (
    var categories: List<String> = emptyList(),
    var businesses: List<String> = emptyList(),
    var terms: List<String> = emptyList()
)
