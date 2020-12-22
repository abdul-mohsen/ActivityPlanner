package com.bignerdranch.android.activityplanner.Repo

import com.bignerdranch.android.activityplanner.APIs.WebClient
import com.bignerdranch.android.activityplanner.database.BusinessDao
import com.bignerdranch.android.activityplanner.database.BusinessCategoriesDao
import com.bignerdranch.android.activityplanner.database.BusinessWeathersDao
import com.bignerdranch.android.activityplanner.database.CategoryDao
import com.bignerdranch.android.activityplanner.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.lang.Exception

object BusinessRepository {
    private val webClient = WebClient.yelpAPI
    lateinit var businessDao: BusinessDao
    lateinit var categoryDao: CategoryDao
    lateinit var businessCategoriesDao: BusinessCategoriesDao
    private val dispatcher = Dispatchers.IO

    val allBusiness2 by lazy { businessDao.getAll() }
//    val allBusiness by lazy { flow{
//        businessDao.getAll().collect {list ->
//            emit( list.map { business ->
//                Timber.d("$business")
//                getFullBusinessInfo(businessCategoriesDao.getByBusinessId(business.id))
//            })
//        }
//    }.flowOn(dispatcher) }

    suspend fun getFullBusinessInfo(businessWithCategories: BusinessWithCategories): Business =
        businessWithCategories.business.also { business ->
            Timber.d("$businessWithCategories")
            Timber.d("$business  __  ${businessWithCategories.categories}")
            business.categories = categoryDao.getById(
                *businessWithCategories.categories.map { it.categoryId }
            )
        }

    private suspend fun insertWithCategories(businesses: List<Business>) {
        val businessCategory: MutableSet<BusinessCategory> = mutableSetOf()
        val categories: MutableSet<Category> = mutableSetOf()
        businesses.forEach { business ->
            business.categories.forEach { category ->
                categories.add(category)
            }
        }
        categoryDao.insert(*categories.toTypedArray())
        val uniqueCategories = categoryDao.getAll()

        businesses.forEach { business ->
            business.categories.forEach { category ->
                uniqueCategories.first { it.name == category.name }.also {
                    businessCategory.add(BusinessCategory(
                        businessId = business.id,
                        categoryId = it.id
                    ))
                }
            }
        }
        businessCategoriesDao.insert(*businessCategory.toTypedArray())
    }

    suspend fun insert(business: List<Business>) {
        businessDao.insert(*business.toTypedArray())
        insertWithCategories(business)
    }

    suspend fun deleteAll() {
        businessDao.deleteAll()
    }

    suspend fun delete(vararg business: Business) {
        businessDao.delete(*business)
    }

    suspend fun update(vararg business: Business) {
        businessDao.update(*business)
    }

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

    @FlowPreview
    suspend fun autoComplete(
        text: String,
        latitude: Double,
        longitude: Double,
    ): Flow<AutoComplete> = flow {
        val autoComplete = webClient.autoComplete(
            text = text,
            latitude = latitude,
            longitude = longitude
        )
        emit(autoComplete)
    }.retry(1) { e ->
        (e is Exception).also { if (it) delay(1000) }
    }.catch { e ->
        Timber.d(e.toString())
    }.flowOn(dispatcher)

}
