/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.share

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.airbnb.mvrx.viewModel
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.app.features.MainActivity
import im.vector.app.features.start.StartAppViewModel
import im.vector.lib.core.utils.compat.getParcelableArrayListExtraCompat
import im.vector.lib.core.utils.compat.getParcelableExtraCompat

@AndroidEntryPoint
class IncomingShareActivity : VectorBaseActivity<ActivitySimpleBinding>() {

    private val startAppViewModel: StartAppViewModel by viewModel()

    private val launcher = registerStartForActivityResult {
        if (it.resultCode == RESULT_OK) {
            handleAppStarted()
        } else {
            // User has pressed back on the MainActivity, so finish also this one.
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent?.action
        if ((action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) || intent?.type == null) {
            finish()
            return
        }
        val streamUri = intent?.getParcelableExtraCompat<Uri>(Intent.EXTRA_STREAM)
        val streamUris = intent?.getParcelableArrayListExtraCompat<Uri>(Intent.EXTRA_STREAM)
        if ((streamUri != null && streamUri.scheme != ContentResolver.SCHEME_CONTENT) ||
                streamUris?.any { it.scheme != ContentResolver.SCHEME_CONTENT } == true) {
            finish()
            return
        }

        if (startAppViewModel.shouldStartApp()) {
            launcher.launch(MainActivity.getIntentToInitSession(this))
        } else {
            handleAppStarted()
        }
    }

    override fun getBinding() = ActivitySimpleBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    override val rootView: View
        get() = views.coordinatorLayout

    private fun handleAppStarted() {
        // If we are not logged in, stop the sharing process and open login screen.
        // In the future, we might want to relaunch the sharing process after login.
        if (!activeSessionHolder.hasActiveSession()) {
            startLoginActivity()
        } else {
            if (isFirstCreation()) {
                addFragment(views.simpleFragmentContainer, IncomingShareFragment::class.java)
            }
        }
    }

    private fun startLoginActivity() {
        navigator.openLogin(
                context = this,
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        )
        finish()
    }
}
