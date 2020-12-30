package com.bignerdranch.android.activityplanner.ui.home

import com.bignerdranch.android.activityplanner.Repo.BusinessRepository
import com.bignerdranch.android.activityplanner.Repo.SearchHistoryRepository
import com.bignerdranch.android.activityplanner.Repo.WeatherRepository
import com.bignerdranch.android.activityplanner.database.SearchHistoryDao
import com.bignerdranch.android.activityplanner.model.*
import com.mapbox.mapboxsdk.geometry.LatLng
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.stub
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class HomeViewModelTest {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var searchHistoryDao: SearchHistoryDao
    private val mySearchHistoryList = listOf(SearchHistory(query = "new"))
    @ExperimentalCoroutinesApi
    private val dispatcher = TestCoroutineDispatcher()

    @ExperimentalCoroutinesApi
    @Before
    fun setUp() {
        mockkObject(BusinessRepository)
        mockkObject(WeatherRepository)
        mockkObject(SearchHistoryRepository)
        BusinessRepository.dispatcher = dispatcher
//        BusinessRepository.webClient = mock()
        WeatherRepository.dispatcher = dispatcher
//        WeatherRepository.webClient = mock()
        searchHistoryDao = mock()
        searchHistoryDao.stub {
            onBlocking { getALL() } doReturn flow { emit(mySearchHistoryList) }
        }
        SearchHistoryRepository.searchHistoryDao = searchHistoryDao
        homeViewModel = HomeViewModel(dispatcher, dispatcher)
    }

    @After
    fun tearDown() {
    }

    @Test
    fun getSearchHistoryList() {
        homeViewModel.searchHistoryList shouldBeEqualTo mySearchHistoryList.map { it.query }
    }

    @FlowPreview
    @Test
    fun `No word has been provided for the autocomplete text view`() {
        homeViewModel.autoComplete("")
        homeViewModel.autoCompleteFlow.value shouldBeEqualTo homeViewModel.searchHistoryList.toSet().toList()
    }

    @ExperimentalCoroutinesApi
    @FlowPreview
    @Test
    fun `A word has been provided for the autocomplete text view`(): Unit = runBlocking {
        val input = AutoComplete(
            categories = listOf("game", "fun", "fun"),
            businesses = listOf("Mohsen Business", "Google", "ssda", "Google"),
            terms = listOf("game", "fun", "joy")
        )
        coEvery { BusinessRepository.getAutoComplete(any(), any(), any()) } returns flow { emit(input) }

        val output = listOf("game", "fun", "Mohsen Business", "Google", "ssda", "joy")

        homeViewModel.autoComplete("new")

        homeViewModel.autoCompleteFlow.value shouldBeEqualTo output

    }

    @Test
    fun `searchAPI find no businesses`(): Unit = runBlocking {
        coEvery { BusinessRepository.getBusinesses(any(), any(), any()) } returns flow { emit(emptyList<Business>()) }

        homeViewModel.searchAPI("new")
        homeViewModel.dataState.value shouldBeEqualTo DataState.NoBusinessMatch
    }

    @Test
    fun `searchAPI find businesses`(): Unit = runBlocking {
        val businesses = listOf(Business(
            "id",
            true,
            true,
            10,
            4f,
            weather = null
        ))
        coEvery { BusinessRepository.getBusinesses(any(), any(), any()) } returns flow { emit(businesses) }
        coEvery { WeatherRepository.allWeatherByBusinessIdAndDate(any(), any(), any()) } returns flow{emit(
            emptyList<Weather>())}

        homeViewModel.searchAPI("new")
        homeViewModel.businessList.value shouldBeEqualTo businesses
    }

    @Test
    fun `updateLocation with big difference in location but no businesses`(): Unit = runBlocking {
        coEvery { BusinessRepository.allBusinessByLatLon(any(), any()) } returns flow { emit(emptyList<Business>()) }

        homeViewModel.updateLocation(LatLng(90.0,90.0))

        verify(exactly = 1) { BusinessRepository.allBusinessByLatLon(any(), any()) }

        homeViewModel.dataState.value shouldBeEqualTo DataState.NoBusinessMatch
    }

    @Test
    fun `updateLocation with no big difference in location`() {
        homeViewModel.updateLocation(LatLng(0.0,0.0))

        verify(exactly = 0) { BusinessRepository.allBusinessByLatLon(any(), any()) }
    }

    @Test
    fun `updateLocation with big difference in location and businesses`(): Unit = runBlocking {
        val businesses = listOf(Business(
            "id",
            true,
            true,
            10,
            4f,
            weather = null
        ))
        coEvery { BusinessRepository.allBusinessByLatLon(any(), any()) } returns flow { emit(businesses) }
        coEvery { WeatherRepository.allWeatherByBusinessIdAndDate(any(), any(), any()) } returns flow{emit(
            emptyList<Weather>())}

        homeViewModel.updateLocation(LatLng(90.0,90.0))

        verify(exactly = 1) { BusinessRepository.allBusinessByLatLon(any(), any()) }

        homeViewModel.businessList.value shouldBeEqualTo businesses
    }

    @Test
    fun `empty list of businesses want to updateDate()`() {
        val weatherList = listOf(Weather(
            0,
            0f,
            0,
            0,
            0,
        ))
        coEvery { WeatherRepository.allWeatherByBusinessIdAndDate(any(), any(), any()) } returns flow{
            weatherList
        }

        homeViewModel.updateDate(businesses = mutableListOf())
        verify(exactly = 0 ) {WeatherRepository.allWeatherByBusinessIdAndDate(any(), any(), any())}

    }

    @Test
    fun `a list of business without weather has been submitted to the updateDate()`(): Unit = runBlocking {
        val businesses = mutableListOf(Business(
            "id",
            true,
            true,
            10,
            4f,
            weather = null
        ))
        coEvery { WeatherRepository.allWeatherByBusinessIdAndDate(any(), any(), any()) } returns flow{
            emit(emptyList<Weather>())
        }

        homeViewModel.updateDate(businesses = businesses)
        verify(exactly = 1 ) {WeatherRepository.allWeatherByBusinessIdAndDate(businesses, any(), any())}
        homeViewModel.dataState.value shouldBeEqualTo DataState.NoWeatherData

    }

    @Test
    fun `a list of business with weather has been submitted to the updateDate()`(): Unit = runBlocking {
        val weather = Weather(
            0,
            0f,
            0,
            0,
            0,
        )
        weather.businessId = "id"
        val businesses = mutableListOf(Business(
            "id",
            true,
            true,
            10,
            4f,
            weather = null
        ))
        coEvery { WeatherRepository.allWeatherByBusinessIdAndDate(any(), any(), any()) } returns flow{
            emit(listOf(weather))
        }

        homeViewModel.updateDate(businesses = businesses)
        verify(exactly = 1 ) {WeatherRepository.allWeatherByBusinessIdAndDate(businesses, any(), any())}
        homeViewModel.businessList.value shouldBeEqualTo businesses.toList()
        homeViewModel.businessList.value.first().weather shouldBeEqualTo weather
        homeViewModel.dataState.value shouldBeEqualTo DataState.NewBusinessData

    }

    @Test
    fun addHoursToDate() {
        val hour = 10f
        val date = homeViewModel.addHoursToDate(hour)
        val cal = Calendar.getInstance()
        cal.time = date
        cal.get(Calendar.HOUR_OF_DAY) shouldBeEqualTo hour.toInt()
    }

    @Test
    fun testAddHoursToDate() {
        val localDate = LocalDate.parse("2020-12-29", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val date = Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
        val addedDate = homeViewModel.addHoursToDate(date).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        addedDate.toString() shouldBeEqualTo localDate.toString()
    }
}