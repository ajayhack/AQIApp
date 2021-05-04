package com.example.aqitestapp

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aqitestapp.databinding.DashboardFragmentBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import java.net.URI
import java.net.URISyntaxException
import java.util.concurrent.TimeUnit


class DashboardFragment : Fragment() {
    private var binding: DashboardFragmentBinding? = null
    private val dataPointURL = "ws://city-ws.herokuapp.com/"
    private var firstAPIHit = false
    private var iDialog: IDialog? = null
    private var mWebSocketClient: WebSocketClient? = null
    private var dataMap: HashMap<String, Double>? = hashMapOf()
    private var dataList: MutableList<DataModal>? = mutableListOf()
    private var initialTimeInMillis : Long? = 0L
    private var finalTimeInMillis : Long? = 0L
    private val dashboardAdapter: DashboardAdapter by lazy { DashboardAdapter(
        dataList,
        activity,
        ::onItemClick
    ) }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IDialog) iDialog = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DashboardFragmentBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showRecyclerViewDataOnUI()
        iDialog?.showProgress(getString(R.string.loading_aqi_data))
        firstAPIHit = false
        GlobalScope.launch(Dispatchers.IO) {
            connectWebSocket()
        }
    }

    //region==============Inflate RecyclerView on UI:-
    private fun showRecyclerViewDataOnUI() {
        binding?.dashboardRV?.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            adapter = dashboardAdapter
        }
    }
    //endregion

    //region===============RecyclerView on Cell Click Handle Callback:-
    private fun onItemClick(position: Int) {
        Log.d("PositionClicked:- ", position.toString())
        if(position > -1){
             Log.d("CityName:- ", dataList?.get(position)?.cityNameData ?: "")
            mWebSocketClient?.close()
            "%.2f".format(dataList?.get(position)?.cityAQIData)
            (activity as MainActivity).transactFragment(DetailAQIGraphFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("cityData", dataList?.get(position))
                }
            }, isBackStackAdded = true)
        }
    }
    //endregion

    private fun connectWebSocket() {
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
                initialTimeInMillis = System.currentTimeMillis()
                Log.d("InitialTime:- ", initialTimeInMillis.toString())
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

    //region=======================Parse JSON Data and Update on UI:-
    private fun parseDataAndUpdateOnUI(data: String) {
        if (firstAPIHit) {
            iDialog?.showProgress(getString(R.string.updating_aqi_data))
        }

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

                //region===========Convert HashMap to MutableList:-
                if (dataMap != null) {
                    finalTimeInMillis = System.currentTimeMillis()
                    Log.d("FinalTime:- ", finalTimeInMillis.toString())
                    dataList?.clear()
                    for ((key, value) in dataMap?.entries!!) {
                        dataList?.add(
                            DataModal(
                                key,
                                value,
                                (finalTimeInMillis!! - initialTimeInMillis!!)
                            )
                        )
                    }
                }
                //endregion

                //region==========Mapping DataMap Values and Refresh AdapterList:-
                dashboardAdapter.refreshAdapterList(dataList)
                iDialog?.hideProgress()
                firstAPIHit = true
                //endregion
            }else{
                iDialog?.hideProgress()
            }
        }else{
            iDialog?.hideProgress()
        }
    }
    //endregion

    override fun onDetach() {
        super.onDetach()
        iDialog = null
        mWebSocketClient?.close()
    }
}

internal class DashboardAdapter(
    private var dataList: MutableList<DataModal>?,
    private var context: Context?,
    private val onItemClickCB: (Int) -> Unit
) :
    RecyclerView.Adapter<DashboardAdapter.DashboardViewHolder>() {

    companion object {
        val TAG = DashboardAdapter::class.java.simpleName
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): DashboardViewHolder {
        val inflater = LayoutInflater.from(p0.context).inflate(
            R.layout.item_dashboard_adapter,
            p0,
            false
        )
        return DashboardViewHolder(inflater)
    }

    override fun getItemCount(): Int {
        return dataList?.size ?: 0
    }

    override fun onBindViewHolder(p0: DashboardViewHolder, p1: Int) {
        val modal = dataList?.get(p1)
        p0.cityName.text = modal?.cityNameData ?: ""
        p0.aqiNumber.text = "%.2f".format(modal?.cityAQIData) ?: ""
        //p0.lastUpdateTime.text = dataList?.get(p1)?.cityNameData ?:""

        //region=========Changing Color of Text According to AQI Value logic:-
        var textColor = ""
        when {
            modal?.cityAQIData?.toInt()?:0 in 0..50 -> {
                textColor = "#008000"
                p0.aqiNumber.setTextColor(Color.parseColor("#FFFFFF"))
            }
            modal?.cityAQIData?.toInt()?:0 in 51..100 -> {
                textColor = "#6B8E23"
                p0.aqiNumber.setTextColor(Color.parseColor("#FFFFFF"))
            }
            modal?.cityAQIData?.toInt()?:0 in 101..200 -> {
                textColor = "#FFFF00"
                p0.aqiNumber.setTextColor(Color.parseColor("#000000"))
            }
            modal?.cityAQIData?.toInt()?:0 in 201..300 -> {
                textColor = "#FF8C00"
                p0.aqiNumber.setTextColor(Color.parseColor("#FFFFFF"))
            }
            modal?.cityAQIData?.toInt()?:0 in 301..400 -> {
                textColor = "#FF0000"
                p0.aqiNumber.setTextColor(Color.parseColor("#FFFFFF"))
                p0.aqiNumber.startAnimation(AnimationUtils.loadAnimation(context, R.anim.blink))
            }
            modal?.cityAQIData?.toInt()?:0 in 401..500 -> {
                textColor = "#8B0000"
                p0.aqiNumber.setTextColor(Color.parseColor("FFFFFF"))
                p0.aqiNumber.startAnimation(AnimationUtils.loadAnimation(context, R.anim.blink))
            }
        }
        p0.aqiNumber.setBackgroundColor(Color.parseColor(textColor))
        //endregion

        //region============Logic to Show Last Updated Time on UI:-
        val seconds = (modal?.lastUpdatedTime?.div( 1000)?.rem(60))?.toInt()
        val minutes = (modal?.lastUpdatedTime?.div(1000)?.minus(seconds ?:0)?.rem(60))
        val time = minutes
        if (time != null) {
            when {
                time < 1 -> {
                    p0.lastUpdateTime.text = LastUpdatedTime.A_FEW_SECONDS_AGO.data
                }
                time >= 1 -> {
                    p0.lastUpdateTime.text = LastUpdatedTime.A_MINUTE_AGO.data
                }
                time >= 60 -> {
                    p0.lastUpdateTime.text = LastUpdatedTime.A_HOUR_AGO.data
                }
            }
        }
        //endregion
    }

    //region==========================Below Method is used to refresh Adapter New Data:-
    fun refreshAdapterList(refreshList: MutableList<DataModal>?) {
        this.dataList = refreshList
        notifyDataSetChanged()
    }
    //endregion

    inner class DashboardViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        val cityName = view.findViewById<TextView>(R.id.tvCityName)
        val aqiNumber = view.findViewById<TextView>(R.id.tvAQINumber)
        val lastUpdateTime = view.findViewById<TextView>(R.id.tvLastUpdated)
        val cardView = view.findViewById<CardView>(R.id.cardView)

        init {
            cardView.setOnClickListener { onItemClickCB(adapterPosition) }
        }
    }
}

//region===============Data Modal Class to Hold Data Coming from WebSockets:-
data class DataModal(var cityNameData: String?, var cityAQIData: Double?, var lastUpdatedTime: Long) : Serializable
//endregion

//region================Last Updated Time String ENUMS:-
enum class LastUpdatedTime(val data: String){
    A_FEW_SECONDS_AGO("A few seconds ago"),
    A_MINUTE_AGO("A minute ago"),
    A_HOUR_AGO("A hour ago")
}
//endregion
