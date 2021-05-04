package com.example.aqitestapp

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.Window
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

abstract class BaseActivity : AppCompatActivity(), IDialog {
    private var progressDialog: Dialog? = null
    private var progressTitleMsg: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setProgressDialog()
    }

    private fun setProgressDialog() {
        progressDialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.item_progress_dialog)
            setCancelable(false)
        }
        progressTitleMsg = progressDialog?.findViewById(R.id.msg_et)
    }

    override fun showProgress( progressMsg: String ) {
        if (null != progressDialog && !progressDialog?.isShowing!! && !(this as Activity).isFinishing) {
            progressTitleMsg?.text =progressMsg
            progressDialog?.show()
        }
    }

    override fun hideProgress() {
        if (null != progressDialog && progressDialog?.isShowing == true && !(this as Activity).isFinishing)
            progressDialog?.dismiss()
    }

    override fun setProgressTitle(title: String) {
        progressTitleMsg?.text = title
    }

    open fun transactFragment(fragment: Fragment, isBackStackAdded: Boolean = false): Boolean {
        val trans = supportFragmentManager.beginTransaction().apply {
            replace(R.id.home_fragment, fragment, fragment::class.java.simpleName)
            addToBackStack("")
        }
        if (isBackStackAdded) trans.addToBackStack(null)
        return trans.commitAllowingStateLoss() >= 0
    }

}

interface IDialog {
    fun setProgressTitle(title: String)
    fun showProgress(progressMsg: String = "Please Wait....")
    fun hideProgress()
}
