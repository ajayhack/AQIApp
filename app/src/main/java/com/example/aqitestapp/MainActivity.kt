package com.example.aqitestapp

import android.os.Bundle
import com.example.aqitestapp.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {

    var binding : ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        binding?.toolbarView?.toolBarTV?.text = getString(R.string.app_name)

        transactFragment(DashboardFragment())
    }

    override fun onBackPressed() {
        when (supportFragmentManager.findFragmentById(R.id.home_fragment)) {
            is DashboardFragment -> finishAffinity()
            else -> super.onBackPressed()
        }
    }
}