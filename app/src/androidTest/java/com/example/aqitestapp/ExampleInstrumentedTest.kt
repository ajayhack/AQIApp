package com.example.aqitestapp

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.aqitestapp", appContext.packageName)
    }

    //region==========Check In Range in Kotlin and Return Desired Result from it:-
    @Test
    fun getAQIBasedBarColor(aqi: Float){
        var aqiColor = "101"
        when {
            aqi.toInt() ?: 0 in 0..50 -> {
                aqiColor = "#008000"
            }
            aqi.toInt() ?: 0 in 51..100 -> {
                aqiColor = "#6B8E23"
            }
            aqi.toInt() ?: 0 in 101..200 -> {
                aqiColor = "#FFFF00"
            }
            aqi.toInt() ?: 0 in 201..300 -> {
                aqiColor = "#FF8C00"
            }
            aqi.toInt() ?: 0 in 301..400 -> {
                aqiColor = "#FF0000"
            }
            aqi.toInt() ?: 0 in 401..500 -> {
                aqiColor = "#8B0000"
            }
        }
         println("The Color Code:- $aqiColor")
    }
    //endregion
}