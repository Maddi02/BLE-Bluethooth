/*
 * Copyright 2022 Punch Through Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.punchthrough.blestarterappandroid

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.utils.Easing
import com.github.mikephil.charting.animation.Easing.EaseInCubic
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.android.synthetic.main.activity_measument_screen.endTimeData

import kotlinx.android.synthetic.main.activity_measument_screen.getTheGraph
import kotlinx.android.synthetic.main.activity_measument_screen.humidityAvg
import kotlinx.android.synthetic.main.activity_measument_screen.humidityMax
import kotlinx.android.synthetic.main.activity_measument_screen.humidityMin
import kotlinx.android.synthetic.main.activity_measument_screen.startTimeData
import kotlinx.android.synthetic.main.activity_measument_screen.tempAvgData
import kotlinx.android.synthetic.main.activity_measument_screen.tempMaxData
import kotlinx.android.synthetic.main.activity_measument_screen.tempMinData
import kotlinx.android.synthetic.main.activity_measument_screen.totalTimeData
import kotlin.math.round


import kotlin.properties.Delegates

class MeasumentScreen : AppCompatActivity() {


    private lateinit var tempValueList : ArrayList<Double>
    private lateinit var humiValueList : ArrayList<Double>
    private lateinit var timeValueList : ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_measument_screen)
          tempValueList = (intent.getSerializableExtra("keyTempList") as ArrayList<Double>?)!!
          humiValueList  = (intent.getSerializableExtra("keyHumiList") as ArrayList<Double>?)!!
          timeValueList = (intent.getSerializableExtra("keyTimeList") as ArrayList<String>?)!!

        // on below line we are initializing
        // our variable with their ids.
        setLineChartData()
        setTempConclusion()
        setHumiConclusion()
        setTimeConclusion()
    }


    private fun setTempConclusion()
    {
        tempMinData.text =  tempValueList.toList().min().toString() + "°C"
        tempMaxData.text =  tempValueList.toList().max().toString() + "°C"
        tempAvgData.text =  (round(tempValueList.average() * 100) / 100).toString() + "°C"
    }

    private fun setHumiConclusion()
    {
        humidityMin.text =  humiValueList.toList().min().toString() + "%"
        humidityMax.text =  humiValueList.toList().max().toString() + "%"
        humidityAvg.text =  (round(humiValueList.average() * 100) / 100).toString() + "%"
    }

    private fun setTimeConclusion()
    {
        totalTimeData.text = timeValueList.size.toString() + " s"
        startTimeData.text = timeValueList[0]
        endTimeData.text = timeValueList[timeValueList.size -1]
    }


        private fun setLineChartData() {

            val linevalues = ArrayList<Entry>()
            val linevalues1 = ArrayList<Entry>()
            for((secondsCounter, temp) in tempValueList.withIndex())
            {
                linevalues.add(Entry( secondsCounter.toFloat(), temp.toFloat()))
            }

            for((secondsCounter, humi) in humiValueList.withIndex())
            {
                linevalues1.add(Entry( secondsCounter.toFloat(), humi.toFloat()))
            }





            getTheGraph.description.text = "x axis in seconds"
            val linedataset = LineDataSet(linevalues, "Temperatur")
            val linedataset1 = LineDataSet(linevalues1, "Humidity")
            //We add features to our chart


            //We connect our data to the UI Screen
            val data = LineData(linedataset, linedataset1)
            getTheGraph.data = data


        }
    }
