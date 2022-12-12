package com.example.bleoffers.utils

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class PermissionRequester(activity: AppCompatActivity, private val permission: String, onDenied: () -> Unit = {}, onShowRationale: () -> Unit = {}) {

    private var onGranted: () -> Unit = {}

    private val launcher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        when {
            isGranted -> onGranted()
            activity.shouldShowRequestPermissionRationale(permission) -> onShowRationale()
            else -> onDenied()
        }
    }

    fun runWithPermission(onGranted: () -> Unit) {
        this.onGranted = onGranted
        launcher.launch(permission)
    }
}