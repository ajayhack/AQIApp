# AQIApp
This is an AQI City Wise Showing App.

#Tech Stack Used :-
1. Android + Kotlin

#Connection used for Data Sets :-
1. WebSocket (For Regular AQI Data Updates)

#Library used :-
1. MPAndroidChart Library - MPAndroidChart Library is used for Showing Graphical Realtime Bar Chart of 30seconds Interval to the user. 
MPAndroidChart Library has been in development mode from a long time and it's more efficient and reliable compare to other third party charts lib for providing
better user experience.
2. I Have used MPAndroidChart Bar Chart for Displaying regular 30seconds interval AQI Data but we can also used Line Chart in it.

#Application Architecture :-
This is a Single Activity Application in which i have used fragments to display City Wise AQI Data List and Graphical RealTime View of Selected City AQI,
I have used ViewBinding for quick accessing UI Views and bind up with fragments.

#Core Logic of Application:-
1.Regular Refreshed Data Display on AQI Data List Fragment by using RecyclerView , Adapter and HashMap for containing and supplying data to Adapter,
HashMap is used to remove Duplicates value and only store the updated value of particular City.
2.I have also changed the condition wise background color of AQI Number.
3.Last Updated Time is been calculated by the [data received from websocket system tim in milliseconds - initial websocket connection system time in milliseconds]
and then convert it in Minutes to check whether it updated in last few seconds , A minute ago or an hour ago.
4.Android Core Component Handler is used to show regular 30seconds interval updates in AQI RealTime Graphical Views.

#Test Cases:-
I have done a method Test in Android ExampleInstrumentedTest to check method logic output.

#Note:- It took me 8-9 Hours to Build Features , UI , Some Method Test Case and Full App Testing for this App.

That's it Please feel free to give your valuable feedback on this App.
