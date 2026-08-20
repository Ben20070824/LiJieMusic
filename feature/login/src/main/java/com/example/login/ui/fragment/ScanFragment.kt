package com.example.login.ui.fragment

import androidx.compose.runtime.Composable
import androidx.fragment.app.viewModels
import com.example.base.BaseComposeFragment
import com.example.login.ui.viewmodel.LoginViewModel
import com.example.login.ui.screen.ScanScreen
import com.example.therouter.NavigationFragmentUtil
import com.example.therouter.RoutePath
import com.therouter.router.Route

@Route(path = RoutePath.LOGIN_SCAN)
class ScanFragment : BaseComposeFragment() {
    private val viewModel: LoginViewModel by viewModels()

    @Composable
    override fun Content() {
        ScanScreen(
            viewModel = viewModel,
            onLoginSuccess = {
                (activity as? NavigationFragmentUtil)?.showMainContent()
            }
        )
    }
}
