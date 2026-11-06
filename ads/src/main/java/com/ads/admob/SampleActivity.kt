package com.ads.admob

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.ads.admob.billing.factory.IapFactory

class SampleActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        try {
            IapFactory.initialize(application, arrayListOf(), false)
        } catch (ex: Exception){
            ex
        }
    }
}