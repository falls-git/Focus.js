package com.example.focusjs

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatusWidgetManager(private val context: Context) {

    fun getLiveStatus(): DeviceStatus {
        // Change pattern to "hh:mm a" (e.g., "08:26 PM") or "h:mm a" for no leading zero (e.g., "8:26 PM")
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEE MMM dd", Locale.getDefault())

        val now = Date()
        val currentTime = timeFormat.format(now)
        val currentDate = dateFormat.format(now)

        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        val batteryPct = if (level != -1 && scale != -1) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            100
        }

        return DeviceStatus(
            time = currentTime,
            date = currentDate,
            battery = "$batteryPct%"
        )
    }
}