package com.example.login.ui.fragment

import androidx.compose.runtime.Composable
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.base.BaseComposeFragment
import com.example.login.ui.viewmodel.LoginViewModel
import com.example.login.R
import com.example.login.ui.screen.LoginScreen
import com.example.therouter.NavigationFragmentUtil
import com.example.therouter.RoutePath
import com.therouter.TheRouter
import com.therouter.router.Route

@Route(path = RoutePath.LOGIN_MAIN)
class LoginFragment : BaseComposeFragment() {
    private val viewModel: LoginViewModel by viewModels()

    @Composable
    override fun Content() {
        LoginScreen(
            viewModel = viewModel,
            onNavigateToMail = {
                val fragment =
                    TheRouter.build(RoutePath.LOGIN_MAIL).createFragment<Fragment>()
                    (activity as? NavigationFragmentUtil)?.addFragment(fragment)
            },
            onNavigateToScan = {
                viewModel.loginByScanInPhone()
                val fragment =
                    TheRouter.build(RoutePath.LOGIN_SCAN).createFragment<Fragment>()
                (activity as? NavigationFragmentUtil)?.addFragment(fragment)
            },
            onGuestLogin = { viewModel.loginByGuest() },
            onLoginSuccess = {
                (activity as? NavigationFragmentUtil)?.showMainContent()
            }
        )
    }
}
