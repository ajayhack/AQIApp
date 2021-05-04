package com.example.aqitestapp

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.aqitestapp.databinding.DetailFragmentBinding
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.*
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URISyntaxException


class DetailAQIGraphFragment : Fragment() , OnChartValueSelectedListener {

    private var binding: DetailFragmentBinding? = null
    private var iDialog: IDialog? = null
    private var cityData: DataModal? = null
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null
    private var dataMap: HashMap<String, Double>? = hashMapOf()
    private val dataPointURL = "ws://city-ws.herokuapp.com/"
    private var colorLegendList: MutableList<Int>? = mutableListOf()
    private var legend: Legend? = null
    private var mWebSocketClient: WebSocketClient? = null
    private val entries = ArrayList<BarEntry>()
    private val labels = ArrayList<String>()
    private var dataList: MutableList<AQIIntervalData> = mutableListOf()
    private var intervalTimeOf30Sec = 0
    private var barIndex = 0
    private var barDataSet: BarDataSet? = null
    private var data: BarData? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IDialog) iDialog = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DetailFragmentBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        iDialog?.showProgress("Uploading AQI Graph View")
        cityData = runBlocking { arguments?.getSerializable("cityData") as? DataModal }
        Log.d("ExtraData:- ", cityData.toString())
        val city = "${cityData?.cityNameData} AQI Graph Representation"
        binding?.tvCityName?.text = city
        Toast.makeText(
            requireActivity(),
            "AQI Data will be Refreshed in Every 30 Seconds....",
            Toast.LENGTH_SHORT
        ).show()
        //Setting Back Arrow on Click to Move to Previous Fragment:-
        binding?.backImage?.setOnClickListener { fragmentManager?.popBackStackImmediate() }

        dataList.clear()
        entries.clear()
        labels.clear()
        inflateBarChartFromData(
            cityData?.cityAQIData?.toFloat() ?: 0F,
            intervalTimeOf30Sec.toString() + "sec",
            getAQIBasedBarColor(cityData?.cityAQIData?.toFloat() ?: 0F) ?: ""
        )
        iDialog?.hideProgress()
        binding?.barChart?.setOnChartValueSelectedListener(this)

        //Every 30 Seconds Interval AQI Refresh Method:-
        refreshAQIBarChart()
    }

    //region===============Inflate Bar Chart From Bundle Data for First Time on UI:-
    private fun inflateBarChartFromData(aqi: Float, interval: String, color: String) {
        dataList.add(AQIIntervalData(aqi, interval, color, barIndex))
        setBarChart()
    }
    //endregion

    //region=================Return Color According to AQI Range:-
    private fun getAQIBasedBarColor(aqi: Float): String {
        //region=========Changing Color of Bar Chart Bar According to AQI Value logic:-
        var aqiColor = ""
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
        return aqiColor
    }
    //endregion

    //region============================Service to Run After Every 30 Seconds to Refresh AQI Bar Chart UI:-
    private fun refreshAQIBarChart() {
        runnable = object : Runnable {
            override fun run() {
                try {
                    iDialog?.showProgress()
                    GlobalScope.launch(Dispatchers.IO) { getUpdatedAQIDataFromSocket() }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    //also call the same runnable to call it at regular interval
                    handler.postDelayed(this, 30000)
                }
            }
        }
        handler.post(runnable as Runnable)
    }
    //endregion

    //region===================Hit WebSocket and Get Updated Data AQI data:-
    private fun getUpdatedAQIDataFromSocket() {
        val uri: URI
        try {
            uri = URI(dataPointURL)
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            return
        }
        mWebSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(serverHandshake: ServerHandshake) {
                Log.i("WebSocket", "Opened")
            }

            override fun onMessage(s: String) {
                GlobalScope.launch(Dispatchers.Main) {
                    Log.d("Response Data:- ", s)
                    parseDataAndUpdateOnUI(s)
                }
            }

            override fun onClose(i: Int, s: String, b: Boolean) {
                GlobalScope.launch(Dispatchers.Main) {
                    iDialog?.hideProgress()
                    Log.i("WebSocket", "Closed $s")
                }
            }

            override fun onError(e: Exception) {
                GlobalScope.launch(Dispatchers.Main) {
                    iDialog?.hideProgress()
                    Log.i("WebSocket", "Error " + e.message)
                }
            }
        }
        mWebSocketClient?.connect()
    }
    //endregion

    //region=======================Parse JSON Data and Update on UI:-
    private fun parseDataAndUpdateOnUI(data: String) {
        //Parsing AQI JSON Array Data:-
        if (!TextUtils.isEmpty(data)) {
            val aqiDataArray = JSONArray(data)
            if (aqiDataArray.length() > 0) {
                for (j in 0 until aqiDataArray.length()) {
                    val obj: JSONObject = aqiDataArray.getJSONObject(j)
                    val cityName: String = obj.getString("city")
                    val aqiNumber: Double = obj.getDouble("aqi")
                    dataMap?.put(cityName, aqiNumber)
                }
                Log.d("AQI Data:- ", dataMap?.get(cityData?.cityNameData).toString())
                cityData?.cityAQIData = dataMap?.get(cityData?.cityNameData)
                cityData?.cityNameData = cityData?.cityNameData
                val aqiColor = getAQIBasedBarColor(cityData?.cityAQIData?.toFloat() ?: 0F)
                intervalTimeOf30Sec = intervalTimeOf30Sec.plus(30)
                barIndex += 1
                inflateBarChartFromData(
                    cityData?.cityAQIData?.toFloat() ?: 0F,
                    intervalTimeOf30Sec.toString() + "sec",
                    aqiColor ?: ""
                )
                mWebSocketClient?.close()
                iDialog?.hideProgress()
            } else {
                iDialog?.hideProgress()
            }
        } else {
            iDialog?.hideProgress()
        }
    }
    //endregion

    //region========================= Here the logic of Bar Chart Display on UI:-
    private fun setBarChart() {
        //Stubbing Bar Chart DataSets:-
        try {
            GlobalScope.launch(Dispatchers.IO) {
                //Loop Through DataList and Stubbing 30 Seconds Interval Bar Data:-
                labels.clear()
                entries.clear()
                for (i in 0 until dataList.size-1) {
                    entries.add(BarEntry(dataList[i].aqiData ?: 0f, dataList[i].barIndex ?: 0))
                    labels.add(dataList[i].intervalData ?: "")
                    colorLegendList?.add(Color.parseColor(dataList[i].legendColor))
                }

                barDataSet = BarDataSet(entries, "")
                barDataSet?.valueTextSize = 6f
                data = BarData(labels, barDataSet)
                binding?.barChart?.data = data // set the data and list of labels into chart
                Log.d("Bar Data:- ", binding?.barChart?.data?.toString() ?: "")
                Log.d("Entries:- ", entries.toString() ?: "")
                Log.d("Labels:- ", labels.toString() ?: "")
                binding?.barChart?.setDescription("AQI in ${cityData?.cityNameData}")  // set the description

                withContext(Dispatchers.Main) {
                    barDataSet?.colors = colorLegendList
                    barDataSet?.setDrawValues(true)
                    binding?.barChart?.setDescriptionTextSize(16f)

                    //setting Bar Chart Legend and Legend Cube Color :-
                    legend = binding?.barChart?.legend
                    legend?.isEnabled = false

                    //Invalidating BarChart for Quick Refresh:-
                    binding?.barChart?.invalidate()
                }
            }
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
            Toast.makeText(
                requireActivity(),
                "AQI Data will be Refreshed in Every 30 Seconds....",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    //endregion

    override fun onPause() {
        super.onPause()
    }

    override fun onDetach() {
        super.onDetach()
        iDialog = null
        runnable?.let { handler.removeCallbacks(it) }
        mWebSocketClient?.close()
    }

    //region=========================Bar Chart onClick Event CallBacks:-
    override fun onValueSelected(e: Entry?, dataSetIndex: Int, h: Highlight?) {
        Toast.makeText(requireActivity(), "${cityData?.cityNameData} Today's AQI is ${e.toString().split(":")[2]}",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onNothingSelected() {
        Log.d("Nothing Selected:- ", "No Entry Selected!!!!")
    }
    //endregion
}

//region==================Modal to Hold AQI Data of 30 Seconds Interval:-
data class AQIIntervalData(
    var aqiData: Float?,
    val intervalData: String?,
    var legendColor: String?,
    var barIndex: Int?
)
//endregion