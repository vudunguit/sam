package com.sam.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sam.databinding.ActivityMainBinding
import com.sam.R
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Setup observers and UI actions here
    }
}
