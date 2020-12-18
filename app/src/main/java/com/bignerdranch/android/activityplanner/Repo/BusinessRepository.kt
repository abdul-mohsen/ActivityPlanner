package com.bignerdranch.android.activityplanner.Repo

import com.bignerdranch.android.activityplanner.APIs.WebClient
import com.bignerdranch.android.activityplanner.model.Business
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.lang.Exception

object BusinessRepository {
    private val webClient = WebClient.yelpAPI
    private val dispatcher = Dispatchers.IO

    @FlowPreview
    suspend fun getBusinesses(
        term: String,
        latitude: Double,
        longitude: Double,
        pageCount: Int = 1,
        pageSize: Int = 20
    ): Flow<List<Business>> = (0 until pageCount).asFlow().flatMapMerge(concurrency = 4) { page ->
        flow {
            val businessList = webClient.searchBusinesses(
                term = term,
                latitude = latitude,
                longitude = longitude,
                limit = pageSize,
                offset = page*pageSize
            ).toList()
            Timber.d("Got a response with a list of size ${businessList.size}")
            emit(businessList)
        }
    }.retry(1) { e ->
        (e is Exception).also { if (it) delay(1000) }
    }.catch { e ->
        Timber.d(e.toString())
    }.flowOn(dispatcher)
}
