package com.specialtopic.sar.mysar

import android.Manifest
import android.annotation.TargetApi
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.ActionBarDrawerToggle
import android.view.MenuItem
import android.view.MotionEvent
import android.support.v7.widget.Toolbar
import android.view.View
import kotlinx.android.synthetic.main.activity_joystick_mode.*
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.*
import android.content.BroadcastReceiver
import android.os.Build
import android.util.Log
import android.widget.*
import com.specialtopic.sar.mysar.R.layout.devices_dialog


class JoystickModeActivity : AppCompatActivity(){

    private var mToggle : ActionBarDrawerToggle? = null
    private var requestCodeForEnabled = 1
    private var enableBtIntent : Intent? = null
    private var mBluetoothAdapter : BluetoothAdapter? = null
    private var mBTDevices : ArrayList<BluetoothDevice> = ArrayList()
    private var mDeviceListAdapter : ArrayAdapter<BluetoothDevice>? = null
    private var newDevices : ListView? = null

    // Create a BroadcastReceiver for ACTION_FOUND.
    private val mBroadcastReceiver1 = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED )//mBluetoothAdapter.ACTION_STATE_CHANGED )
            {
                val state : Int = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)

                when(state)
                {
                    BluetoothAdapter.STATE_OFF -> Log.d("TAG", "OnReceive: STATE_OFF")
                    BluetoothAdapter.STATE_TURNING_OFF -> Log.d("TAG", "mBroadcastReceiver1: STATE_TURNING_OFF")
                    BluetoothAdapter.STATE_ON -> Log.d("TAG", "mBroadcastReceiver1: STATE_ON")
                    BluetoothAdapter.STATE_TURNING_ON -> Log.d("TAG", "mBroadcastReceiver1: STATE_TURNING_ON")
                }
            }
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private val mBroadcastReceiver2 = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)//mBluetoothAdapter.ACTION_STATE_CHANGED )
            {
                val mode: Int = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR)

                when (mode) {
                // device is in discoverable mode
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE -> Log.d("TAG", "mBroadcastReceiver2: discoverability enabled")
                // device is in not discoverable mode
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE -> Log.d("TAG", "mBroadcastReceiver2: discoverability disabled. able to receive connections")
                    BluetoothAdapter.SCAN_MODE_NONE -> Log.d("TAG", "mBroadcastReceiver2: discoverability disabled. unable to receive connections")
                    BluetoothAdapter.STATE_CONNECTING -> Log.d("TAG", "mBroadcastReceiver2: Connecting...")
                    BluetoothAdapter.STATE_CONNECTED -> Log.d("TAG", "mBroadcastReceiver2: Connected")
                }
            }
        }
    }

    /**
     *  Broadcast receiver for listing devices that are not paired yet
     */
    private val mBroadcastReceiver3 = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            if (action == ACTION_FOUND)
            {
                val device : BluetoothDevice = intent.getParcelableExtra(EXTRA_DEVICE)
                mBTDevices.add(device)
                Log.d("TAG", "onReceive: " + device.name + ": " + device.address)

                mDeviceListAdapter = ArrayAdapter(context, devices_dialog, mBTDevices)
                newDevices!!.adapter = mDeviceListAdapter
            }
        }
    }

    private val mBroadcastReceiver4 = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            if (action == ACTION_BOND_STATE_CHANGED)
            {
                val device : BluetoothDevice = intent.getParcelableExtra(EXTRA_DEVICE)

                when(device.bondState)
                {
                    BOND_BONDED -> Log.d("TAG", "BroadcastReceiver: Bond already bonded")
                    BOND_BONDING -> Log.d("TAG", "BroadcastReceiver: Bonding...")
                    BOND_NONE -> Log.d("TAG", "BroadcastReceiver: Breaking bond")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_joystick_mode)

        btnConnect.text = getString(R.string.connect)
        btnConnect.setBackgroundResource(R.drawable.asset4)
        val mToolbar : Toolbar = findViewById(R.id.nav_action)
        setSupportActionBar(mToolbar)

        mToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.app_name)
        drawerLayout.addDrawerListener(mToggle!!)
        mToggle?.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        enableBtIntent = Intent(this, BluetoothAdapter.ACTION_REQUEST_ENABLE::class.java)
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // broadcasts when bond state changes (ie: pairing)
        val filter = IntentFilter(ACTION_BOND_STATE_CHANGED)
        registerReceiver(mBroadcastReceiver4, filter)

        buttonEvent()
        joystickEvent()

        // https://www.youtube.com/watch?v=y8R2C86BIUc&list=PLgCYzUzKIBE8KHMzpp6JITZ2JxTgWqDH2&index=1

        // pair with device
        /* newDevices!!.setOnItemClickListener { _: AdapterView<*>, _: View, i : Int, _: Long ->
            mBluetoothAdapter!!.cancelDiscovery()

            Log.d("TAG", "onItemClick: You clicked on an item")
            val deviceName = mBTDevices[i].name
            val deviceAddress = mBTDevices[i].address

            Log.d("TAG", "onItemClick: device name: " + deviceName)
            Log.d("TAG", "onItemClick: device address: " + deviceAddress)

            //create bond
            pairBT(i)
        }*/
    }

    private fun bluetoothEnableDisable()
    {
        if (mBluetoothAdapter == null)
        {
            Toast.makeText(this, "Device does not support bluetooth", Toast.LENGTH_LONG).show()
        }
        else
        {
            if (!mBluetoothAdapter!!.isEnabled)
            {
                enableBT()
                discoverDevices()

            }

            else
            {
                disableBT()
            }

        }
    }

    private fun disableBT()
    {
        Log.d("TAG", "isEnabled: disabling BT")
        mBluetoothAdapter!!.disable()

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(mBroadcastReceiver1, filter)
    }

    private fun enableBT()
    {
        // enable BT
        Log.d("TAG", "isEnabled: enabling BT")
        startActivity(enableBtIntent)

        val filterState = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(mBroadcastReceiver1, filterState)

        // discoverability device
        Log.d("TAG", "isEnabled: making device discoverable")
        val discoverability = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        discoverability.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)

        val filterDiscoverable = IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
        registerReceiver(mBroadcastReceiver2, filterDiscoverable)
    }

    private fun discoverDevices()
    {
        if (mBluetoothAdapter!!.isDiscovering)
        {
            mBluetoothAdapter!!.cancelDiscovery()

            // check BT permissions in manifest
            checkBTPermissions()

            mBluetoothAdapter!!.startDiscovery()
            val discoverDevicesIntent = IntentFilter(ACTION_FOUND)
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent)
        }
        else
        {
            // check BT permissions in manifest
            checkBTPermissions()

            mBluetoothAdapter!!.startDiscovery()
            val discoverDevicesIntent = IntentFilter(ACTION_FOUND)
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent)
        }

        showDialogListView()

    }

    private fun pairBT(index : Int)
    {

        Log.d("TAG", "Trying to pair")
        mBTDevices[index].createBond()

    }

    private fun showDialogListView()
    {
        val builder : AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setCancelable(true)
        builder.setPositiveButton("OK", null)
        builder.setView(newDevices)
        val dialog = builder.create()
        dialog.show()
    }

    /**
     * This method requires for all devices running  API23+
     * Android must programmatically check the permissions for bluetooth. Putting the proper permissions
     * in the manifest is not enough
     *
     * NOTE: This will only execute on versions > lollipop because it is not needed otherwise.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private fun checkBTPermissions()
    {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)
        {
            var permissionCheck : Int = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION")
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION")

            if (permissionCheck != 0)
            {
                this.requestPermissions(Array(2){ Manifest.permission.ACCESS_FINE_LOCATION; Manifest.permission.ACCESS_COARSE_LOCATION}, 1001)
            }
            else
            {
                Log.d("TAG", "checkBTPersmissions: No need to check permissions. SDK version < lollipop")
            }
        }
    }

    override fun onDestroy()
    {
        Log.d("TAG", "onDestroy(): called")
        super.onDestroy()
        unregisterReceiver(mBroadcastReceiver1)
        unregisterReceiver(mBroadcastReceiver2)
        unregisterReceiver(mBroadcastReceiver3)
        unregisterReceiver(mBroadcastReceiver4)
    }

    private fun buttonEvent()
    {
        btnConnect.setOnTouchListener{ _, _ ->
            Log.d("TAG", "onTouch: enabling/disabling bluetooth")
            if (btnConnect.text == getString(R.string.connect))
            {
                bluetoothEnableDisable()

                btnConnect.setBackgroundResource(R.drawable.asset6)
                btnConnect.text = getString(R.string.disconnect)

            }
            else if (btnConnect.text == getString(R.string.disconnect))
            {
                bluetoothEnableDisable()

                btnConnect.setBackgroundResource(R.drawable.asset4)
                btnConnect.text = getString(R.string.connect)
            }

            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        //super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCodeForEnabled)
        {
            if (resultCode == RESULT_OK)
            {
                // bluetooth is enabled
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show()

            }
            else if (resultCode == RESULT_CANCELED)
            {
                // bluetooth enabling is canceled
                Toast.makeText(this, "Bluetooth canceled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun joystickEvent()
    {
        val joystick = Joystick(applicationContext, layout_joystick, R.drawable.asset2)

        joystick.setStickSize(150, 150)
        joystick.setLayoutSize(500, 500)
        joystick.setLayoutAlpha(150)
        joystick.setStickAlpha(100)
        joystick.setOffset(90)
        joystick.setMinimumDistance(50)

        layout_joystick.setOnTouchListener { _, arg1 ->
            Log.d("TAG", "onTouch: joystick")
            joystick.drawStick(arg1)
            if (arg1.action == MotionEvent.ACTION_DOWN || arg1.action == MotionEvent.ACTION_MOVE) {
                txtX.text = getString(R.string.xJoystick, joystick.getX())
                txtY.text = getString(R.string.yJoystick, joystick.getY())
                txtAngle.text = getString(R.string.angleJoystick, joystick.getAngle())
                txtDistance.text = getString(R.string.disJoystick, joystick.getDistance())

                val direction = joystick.get8Direction()
                when (direction) {
                    POS.UP -> txtDirection.text = getString(R.string.dirUp)
                    POS.UPRIGHT -> txtDirection.text = getString(R.string.dirUpRight)
                    POS.RIGHT -> txtDirection.text = getString(R.string.dirRight)
                    POS.DOWNRIGHT -> txtDirection.text = getString(R.string.dirDownRight)
                    POS.DOWN -> txtDirection.text = getString(R.string.dirDown)
                    POS.DOWNLEFT -> txtDirection.text = getString(R.string.dirDownLeft)
                    POS.LEFT -> txtDirection.text = getString(R.string.dirLeft)
                    POS.UPLEFT -> txtDirection.text = getString(R.string.dirUpLeft)
                    POS.NONE -> txtDirection.text = getString(R.string.dirCenter)
                }

            } else if (arg1.action == MotionEvent.ACTION_UP) {
                txtX.text = getString(R.string.xJoystick, 0)
                txtY.text = getString(R.string.yJoystick, 0)
                txtAngle.text = getString(R.string.angleJoystick, 0)
                txtDistance.text = getString(R.string.disJoystick, 0)
                txtDirection.text = getString(R.string.dirCenter)
            }
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean
    {
        if (mToggle!!.onOptionsItemSelected(item))
        {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
