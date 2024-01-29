package com.newagedevs.smartvpn.view.dialog

import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.TextView
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.interfaces.action.AnimAction
import com.newagedevs.smartvpn.aop.SingleClick

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/AndroidProject-Kotlin
 *    time   : 2019/03/20
 *    desc   : 升级对话框
 */
class AboutDialog {

    class Builder(context: Context) : BaseDialog.Builder<Builder>(context) {

        private val titleView: TextView? by lazy { findViewById(R.id.tv_title) }
        private val versionView: TextView? by lazy { findViewById(R.id.tv_version) }
        private val descriptionView: TextView? by lazy { findViewById(R.id.tv_description) }
        private val actionView: TextView? by lazy { findViewById(R.id.tv_action) }

        private var listener: OnListener? = null

        init {
            setContentView(R.layout.about_dialog)
            setAnimStyle(AnimAction.ANIM_IOS)
            setCancelable(false)
            setOnClickListener(actionView)

            descriptionView?.movementMethod = ScrollingMovementMethod()
        }


        fun setTitle(title: CharSequence?): Builder = apply {
            titleView?.text = title
        }

        fun setVersionName(name: CharSequence?): Builder = apply {
            versionView?.text = name
        }

        fun setDescription(text: CharSequence?): Builder = apply {
            descriptionView?.text = text
            descriptionView?.visibility = if (text == null) View.GONE else View.VISIBLE
        }

        fun setActionText(text: CharSequence?): Builder = apply {
            actionView?.text = text
        }

        fun setListener(listener: OnListener?): Builder = apply {
            this.listener = listener
        }

        @SingleClick
        override fun onClick(view: View) {
            if (view === actionView) {
                dismiss()
                listener?.onClick(getDialog())
            }
        }
    }

    interface OnListener {
        fun onClick(dialog: BaseDialog?)
    }
}