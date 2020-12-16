package com.bignerdranch.android.activityplanner

import com.bignerdranch.android.activityplanner.flickrAPI.WebClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.lang.Exception

object Repository {
    private val webClient = WebClient
    private val dispatcher = Dispatchers.IO

    @FlowPreview
    suspend fun getBusinesses(
        latitude: Double,
        longitude: Double,
        pageCount: Int,
        pageSize: Int = 20
    ): Flow<List<String>> = (1..pageCount).asFlow().flatMapMerge(concurrency = 4) { page ->
        flow {
            val businessesSearchRespnse = webClient.yelpAPI.searchBusinesses(
                latitude = latitude,
                longitude = longitude,
                limit = pageSize,
                offset = page*pageSize
            )
            Timber.d(businessesSearchRespnse)
            emit(emptyList<String>())
        }
    }.retry(1) { e ->
        (e is Exception).also { if (it) delay(1000) }
    }.catch { e ->
        Timber.d(e.toString())
    }.flowOn(dispatcher)
}