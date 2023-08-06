package com.ad8.presentation.fragments.main_page

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ad8.presentation.base.BaseViewModel
import com.ad8.presentation.util.DispatchersProvider

import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainPageViewModel @Inject constructor(
     val dispatchers: DispatchersProvider,

    ) : BaseViewModel(dispatchers) {
    private val processing = MutableLiveData(false)
    private val progress = MutableLiveData<String>()
    private val result = MutableLiveData<String>()

}