package com.specialtopic.sar.mysar

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

/**
 * Created by Evyl on 11/5/2017.
 */

class DeviceListAdapter(context : Context, resourceId : Int, devices : ArrayList<BluetoothDevice>) : ArrayAdapter<BluetoothDevice>(context, resourceId, devices) {
    var mLayoutInflater : LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    var mDevices : ArrayList<BluetoothDevice> = devices
    var mViewResourceID : Int = resourceId

    override fun getView(position : Int, convertView : View, parent : ViewGroup) : View{
        var convertView = mLayoutInflater.inflate(mViewResourceID, null)

        var device : BluetoothDevice = mDevices[position]

        if (device != null)
        {
            // TODO: change this to find specifically robot BT module
            var deviceName : TextView = R.id.devicesDialog as TextView
            //var deviceName : TextView = R.id.aDeviceName as TextView
            //var deviceAddress : TextView = R.id.aDeviceAddress as TextView

            if (deviceName != null)
            {
                deviceName.text = device.name
            }
           /* if (deviceAddress != null)
            {
                deviceAddress.text = device.address
            }*/
        }

        return convertView
    }

}