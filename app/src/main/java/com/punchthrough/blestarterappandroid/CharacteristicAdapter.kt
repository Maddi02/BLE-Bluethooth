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

import android.bluetooth.BluetoothGattCharacteristic
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.punchthrough.blestarterappandroid.ble.printProperties
//import kotlinx.android.synthetic.main.row_characteristic.view.characteristic_properties
//import kotlinx.android.synthetic.main.row_characteristic.view.characteristic_uuid
import org.jetbrains.anko.layoutInflater
import java.util.UUID

class CharacteristicAdapter(
    private val items: List<BluetoothGattCharacteristic>,
    private val onClickListener: ((characteristic: BluetoothGattCharacteristic) -> Unit)
) : RecyclerView.Adapter<CharacteristicAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = parent.context.layoutInflater.inflate(
            R.layout.row_characteristic,
            parent,
            false
        )
        return ViewHolder(view, onClickListener)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(items[position].uuid == UUID.fromString("de108da1-e6d1-4cf7-b0a9-36417c10990c"))
        {
            val item = items[position]
            println("postion" + items[position].uuid)
            holder.bind(item)
        }
        else{

        }

    }

    class ViewHolder(
        private val view: View,
        private val onClickListener: ((characteristic: BluetoothGattCharacteristic) -> Unit)
    ) : RecyclerView.ViewHolder(view) {

        fun bind(characteristic: BluetoothGattCharacteristic) {
//                view.characteristic_uuid.text = characteristic.uuid.toString()
//                view.characteristic_properties.text = characteristic.printProperties()
                onClickListener.invoke(characteristic)
            }
        }
    }

