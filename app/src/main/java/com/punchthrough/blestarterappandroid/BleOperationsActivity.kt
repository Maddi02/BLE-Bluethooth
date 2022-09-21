/*
 * Copyright 2019 Punch Through Design LLC
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

//import kotlinx.android.synthetic.main.activity_ble_operations.log_scroll_view
//import kotlinx.android.synthetic.main.activity_ble_operations.log_text_view
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.punchthrough.blestarterappandroid.ble.ConnectionEventListener
import com.punchthrough.blestarterappandroid.ble.ConnectionManager
import com.punchthrough.blestarterappandroid.ble.isNotifiable
import com.punchthrough.blestarterappandroid.ble.isWritableWithoutResponse
import com.punchthrough.blestarterappandroid.ble.toHexString
import com.punchthrough.blestarterappandroid.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_ble_operations.avergeHumiData
import kotlinx.android.synthetic.main.activity_ble_operations.avergeTemp
import kotlinx.android.synthetic.main.activity_ble_operations.avergeTempData
import kotlinx.android.synthetic.main.activity_ble_operations.characteristics_recycler_view
import kotlinx.android.synthetic.main.activity_ble_operations.humiData
import kotlinx.android.synthetic.main.activity_ble_operations.resetButton
import kotlinx.android.synthetic.main.activity_ble_operations.showConclusionButton
import kotlinx.android.synthetic.main.activity_ble_operations.startButton
import kotlinx.android.synthetic.main.activity_ble_operations.tempData
import kotlinx.android.synthetic.main.activity_ble_operations.timeTV
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.selector
import org.jetbrains.anko.yesButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import kotlin.math.round

var temp : String = ""
var humi : String = ""

class BleOperationsActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    lateinit var dataHelper: DataHelper

    private val timer = Timer()

    private var timerStarted = false
    private lateinit var serviceIntent: Intent
    private var time = 0.0
    private  var test : MeasumentScreen = MeasumentScreen()
    private  var measurmentStart : Boolean = false
    private lateinit var tempValue : ArrayList<Double>
    private lateinit var humiValue : ArrayList<Double>
    private lateinit var timeValue : ArrayList<String>
    private lateinit var device: BluetoothDevice
    private val dateFormatter = SimpleDateFormat("MMM d, HH:mm:ss", Locale.US)
    private val characteristics by lazy {
        ConnectionManager.servicesOnDevice(device)?.flatMap { service ->
            service.characteristics ?: listOf()

        } ?: listOf()
    }
    private val characteristicProperties by lazy {
        characteristics.map { characteristic ->
            characteristic to mutableListOf<CharacteristicProperty>().apply {
                if (characteristic.isNotifiable()) add(CharacteristicProperty.Notifiable)
                if (characteristic.isWritableWithoutResponse()) {
                    add(CharacteristicProperty.WritableWithoutResponse)
                }
            }.toList()
        }.toMap()
    }
    private val characteristicAdapter: CharacteristicAdapter by lazy {
        CharacteristicAdapter(characteristics) { characteristic ->
            showCharacteristicOptions(characteristic)
        }
    }
    private var notifyingCharacteristics = mutableListOf<UUID>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ConnectionManager.registerListener(connectionEventListener)
        dataHelper = DataHelper(applicationContext)
        binding = ActivityMainBinding.inflate(layoutInflater)
         tempValue = ArrayList()
         humiValue = ArrayList()
         timeValue = ArrayList()



        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            ?: error("Missing BluetoothDevice from MainActivity!")


        setContentView(R.layout.activity_ble_operations)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            title = getString(R.string.ble_playground)
        }
       setupRecyclerView()



        showConclusionButton.isVisible = false
        startButton.setOnClickListener{ startStopAction() }
        resetButton.setOnClickListener{ resetAction() }
        showConclusionButton.setOnClickListener{
            val myIntent = Intent(this, MeasumentScreen::class.java)
          //  myIntent.putExtra("key", value) //Optional parameters

            startActivity(myIntent)
        }

        if(dataHelper.timerCounting())
        {
            startTimer()
        }
        else
        {
            stopTimer()
            if(dataHelper.startTime() != null && dataHelper.stopTime() != null)
            {
                val time = Date().time - calcRestartTime().time
                timeTV.text = timeStringFromLong(time)
            }
        }

        timer.scheduleAtFixedRate(TimeTask(), 0, 500)

    }


    override fun onDestroy() {
        ConnectionManager.unregisterListener(connectionEventListener)
        ConnectionManager.teardownConnection(device)
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setupRecyclerView() {
        characteristics_recycler_view.apply {
            adapter = characteristicAdapter
            println("Here" + characteristics.get(4).uuid)
            layoutManager = LinearLayoutManager(
                this@BleOperationsActivity,
                RecyclerView.VERTICAL,
                false
            )
            isNestedScrollingEnabled = false
        }

        val animator = characteristics_recycler_view.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }

    @SuppressLint("SetTextI18n")
    private fun log(message: String) {

        val formattedMessage = String.format(message)
        println(dateFormatter.format(Date()))

        runOnUiThread {
            println(getTemp(formattedMessage))
       //     MeasumentScreen.temp = getTemp(formattedMessage)

            if(measurmentStart)
            {
                println("Measurvalues")
                timeValue.add(dateFormatter.format(Date()))
                temp = getTemp(formattedMessage)
                humi = getHumi(formattedMessage)
                tempValue.add(temp.toDouble())
                humiValue.add(humi.toDouble())

            }

            tempData.text = "${getTemp(formattedMessage)}"
            humiData.text = "${getHumi(formattedMessage)}"
            avergeTempData.text = calculateTempAvg()
            avergeHumiData.text = calculateHumiAvg()
        }
    }

    companion object{

        fun getTemp(wholeText : String) : String
        {
          //  t = wholeText.split(",")[0] + "°C"
            return wholeText.split(",")[0]

        }
         fun getHumi(wholeText : String) : String
        {
            return wholeText.split(",")[1]
        }

    }

    private fun calculateTempAvg() : String
    {
        var sum = 0.0
        for (num in tempValue) {
            sum += num
        }
        val average = sum / tempValue.size

        return  (round(average * 100) / 100).toString()
    }


    private fun calculateHumiAvg() : String
    {
        var sum = 0.0
        for (num in humiValue) {
            sum += num
        }
        val average = sum / humiValue.size

        return  (round(average * 100) / 100).toString()
    }



    private fun showCharacteristicOptions(characteristic: BluetoothGattCharacteristic) {
        Log.d("MyLog" , characteristic.toString())
        characteristicProperties[characteristic]?.let { properties ->

            selector("Connection Successfull!!!!", properties.map { it.action }) { _, i ->
                Log.d("MyLog", properties[i].action)
                    when (properties[i]) {
                        CharacteristicProperty.Writable, CharacteristicProperty.WritableWithoutResponse -> {
                            // showWritePayloadDialog(characteristic)
                        }
                        CharacteristicProperty.Notifiable, CharacteristicProperty.Indicatable -> {
                            if (notifyingCharacteristics.contains(characteristic.uuid)) {
                            //    log("Disabling notifications on ${characteristic.uuid}")
                                ConnectionManager.disableNotifications(device, characteristic)
                            } else {
                              //  log("Enabling notifications on ${characteristic.uuid}")
                                ConnectionManager.enableNotifications(device, characteristic)
                            }
                        }

                        else -> {}
                    }
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun showWritePayloadDialog(characteristic: BluetoothGattCharacteristic) {
        val hexField = layoutInflater.inflate(R.layout.edittext_hex_payload, null) as EditText
        alert {
            customView = hexField
            isCancelable = false
            yesButton {
                with(hexField.text.toString()) {
                    if (isNotBlank() && isNotEmpty()) {
                        val bytes = hexToBytes()
                        log("Writing to ${characteristic.uuid}: ${bytes.toHexString()}")
                        ConnectionManager.writeCharacteristic(device, characteristic, bytes)
                    } else {
                        log("Please enter a hex payload to write to ${characteristic.uuid}")
                    }
                }
            }
            noButton {}
        }.show()
        hexField.showKeyboard()
    }

    private val connectionEventListener by lazy {


        ConnectionEventListener().apply {
            onDisconnect = {
                runOnUiThread {
                    alert {
                        title = "Disconnected"
                        message = "Disconnected from device."
                        positiveButton("OK") { onBackPressed() }
                    }.show()
                }
            }

            onCharacteristicChanged = { _, characteristic ->
                println("value" + String(characteristic.value, Charsets.UTF_8))
                log("${String(characteristic.value, Charsets.UTF_8)}")
            }

        }
    }

    private enum class CharacteristicProperty {
        Readable,
        Writable,
        WritableWithoutResponse,
        Notifiable,
        Indicatable;

        val action
            get() = when (this) {
                Readable -> "Read"
                Writable -> "Write"
                WritableWithoutResponse -> "Write Without Response"
                Notifiable -> "Get Values"
                Indicatable -> "Toggle Indications"
            }
    }

    private fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))

    }

    private fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun EditText.showKeyboard() {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        requestFocus()
        inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun String.hexToBytes() =
        this.chunked(2).map { it.toUpperCase(Locale.US).toInt(16).toByte() }.toByteArray()




    private inner class TimeTask: TimerTask()
    {
        override fun run()
        {

            if(dataHelper.timerCounting())
            {
                val time = Date().time - dataHelper.startTime()!!.time
               timeTV.text = timeStringFromLong(time)
            }
        }
    }

    private fun resetAction()
    {
        dataHelper.setStopTime(null)
        dataHelper.setStartTime(null)
        stopTimer()
        timeTV.text = timeStringFromLong(0)
        tempValue.clear()
        timeValue.clear()
        humiValue.clear()
        startButton.isEnabled = true
        startButton.isClickable = true
        showConclusionButton.isVisible = false
    }

    private fun stopTimer()
    {
        println("in stopTimer")
        dataHelper.setTimerCounting(false)
        println("measurment tooks " + timeTV.text)
        println("measurment starts at" + dataHelper.startTime())
        println("measurment end at" + dataHelper.stopTime())
        startButton.text = "Start"
        measurmentStart = false

        for (temp in tempValue) {
            Log.d("MyLog" , temp.toString())
        }

        for (time in timeValue) {
            Log.d("MyLog" , time)
        }

        Log.d("MyLog", tempValue.size.toString())
        Log.d("MyLog", timeValue.size.toString())
        Log.d("MyLog", humiValue.size.toString())


    }

    private fun startTimer()
    {
        println("in startTimer")
        dataHelper.setTimerCounting(true)
        startButton.text = "Stop"
        measurmentStart = true
    }

    private fun startStopAction()
    {
        println("in startStopAction")
        if(dataHelper.timerCounting())
        {
            dataHelper.setStopTime(Date())
            stopTimer()
            startButton.isEnabled = false
            startButton.isClickable = false
            showConclusionButton.isVisible = true
        }
        else
        {
            if(dataHelper.stopTime() != null)
            {
                dataHelper.setStartTime(calcRestartTime())
                dataHelper.setStopTime(null)
                Log.d("MyLog" , "Here could be a restart")
                startButton.isEnabled = false
                startButton.isClickable = false
                showConclusionButton.isVisible = true
            }
            else
            {
                dataHelper.setStartTime(Date())

            }
            startTimer()
        }
    }

    private fun calcRestartTime(): Date
    {
        println("in calcRestartTime")
        if(dataHelper == null)
        {
            println("Datahelper null")
        }
        var diff = 0
        if(dataHelper.startTime() != null && dataHelper.startTime() != null )
        {
             diff = (dataHelper.startTime()!!.time - dataHelper.stopTime()!!.time).toInt()
        }


        return Date(System.currentTimeMillis() + diff)
    }

    private fun timeStringFromLong(ms: Long): String
    {
        println("in timeStringFromLong")
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60) % 60)
        val hours = (ms / (1000 * 60 * 60) % 24)
        return makeTimeString(hours, minutes, seconds)
    }

    private fun makeTimeString(hours: Long, minutes: Long, seconds: Long): String
    {
        println("in makeTimeString")
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }


}
