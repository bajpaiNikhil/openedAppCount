package com.example.openedappcount

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WellbeingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UsageStatsRepository(application)

    var unlockCount by mutableIntStateOf(0)
        private set

    var appUsage by mutableStateOf<List<AppUsageInfo>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    init {
        refresh(showLoading = true)
    }

    fun refresh(showLoading: Boolean = false) {
        if (showLoading) isLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            val unlocks = try { repository.getUnlockCount() } catch (e: SecurityException) { 0 }
            val usage = try { repository.getAppUsageStats() } catch (e: SecurityException) { emptyList() }
            withContext(Dispatchers.Main) {
                unlockCount = unlocks
                appUsage = usage
                isLoading = false
            }
        }
    }
}
