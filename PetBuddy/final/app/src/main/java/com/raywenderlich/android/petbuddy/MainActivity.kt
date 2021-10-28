/*
 * Copyright (c) 2021 Razeware LLC
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 * 
 * This project and source code may use libraries or frameworks that are
 * released under various Open-Source licenses. Use of those libraries and
 * frameworks are governed by their own individual licenses.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.android.petbuddy

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.raywenderlich.android.petbuddy.detectedactivity.DetectedActivityService
import com.raywenderlich.android.petbuddy.transitions.TRANSITIONS_RECEIVER_ACTION
import com.raywenderlich.android.petbuddy.transitions.TransitionsReceiver
import com.raywenderlich.android.petbuddy.transitions.removeActivityTransitionUpdates
import com.raywenderlich.android.petbuddy.transitions.requestActivityTransitionUpdates
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

  private var isTrackingStarted = false
    set(value) {
      resetBtn.visibility = if(value) View.VISIBLE else View.GONE
      field = value
    }

  private val transitionBroadcastReceiver: TransitionsReceiver = TransitionsReceiver().apply {
    action = { setDetectedActivity(it) }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    if (intent.hasExtra(SUPPORTED_ACTIVITY_KEY)) {
      val supportedActivity = intent.getSerializableExtra(
          SUPPORTED_ACTIVITY_KEY) as SupportedActivity
      setDetectedActivity(supportedActivity)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(R.style.AppTheme)

    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    startBtn.setOnClickListener {
      if (isPermissionGranted()) {
        startService(Intent(this, DetectedActivityService::class.java))
        requestActivityTransitionUpdates()
        isTrackingStarted = true
        Toast.makeText(this@MainActivity, "You've started activity tracking",
            Toast.LENGTH_SHORT).show()
      } else {
        requestPermission()
      }
    }
    stopBtn.setOnClickListener {
      stopService(Intent(this, DetectedActivityService::class.java))
      removeActivityTransitionUpdates()

      Toast.makeText(this, "You've stopped tracking your activity", Toast.LENGTH_SHORT).show()
    }
    resetBtn.setOnClickListener {
      resetTracking()
    }
  }

  private fun resetTracking() {
    isTrackingStarted = false
    setDetectedActivity(SupportedActivity.NOT_STARTED)
    removeActivityTransitionUpdates()
    stopService(Intent(this, DetectedActivityService::class.java))
  }

  override fun onResume() {
    super.onResume()
    registerReceiver(transitionBroadcastReceiver, IntentFilter(TRANSITIONS_RECEIVER_ACTION))
  }

  override fun onPause() {
    unregisterReceiver(transitionBroadcastReceiver)
    super.onPause()
  }

  override fun onDestroy() {
    removeActivityTransitionUpdates()
    stopService(Intent(this, DetectedActivityService::class.java))
    super.onDestroy()
  }

  private fun setDetectedActivity(supportedActivity: SupportedActivity) {
    activityImage.setImageDrawable(ContextCompat.getDrawable(this, supportedActivity.activityImage))
    activityTitle.text = getString(supportedActivity.activityText)
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
      grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
            Manifest.permission.ACTIVITY_RECOGNITION).not() &&
        grantResults.size == 1 &&
        grantResults[0] == PackageManager.PERMISSION_DENIED) {
      showSettingsDialog(this)
    } else if (requestCode == PERMISSION_REQUEST_ACTIVITY_RECOGNITION &&
        permissions.contains(Manifest.permission.ACTIVITY_RECOGNITION) &&
        grantResults.size == 1 &&
        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      Log.d("permission_result", "permission granted")
      startService(Intent(this, DetectedActivityService::class.java))
      requestActivityTransitionUpdates()
      isTrackingStarted = true
    }
  }
}