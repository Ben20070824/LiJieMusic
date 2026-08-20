package com.example.dynamics.ui.fragment

import androidx.compose.runtime.Composable
import androidx.fragment.app.viewModels
import com.example.base.BaseComposeFragment
import com.example.dynamics.ui.viewmodel.DynamicsViewModel
import com.example.dynamics.ui.screen.DynamicScreen
import com.example.dynamics.ui.theme.LiJieMusicTheme
import com.example.therouter.RoutePath
import com.therouter.router.Route
import kotlin.getValue

@Route(path = RoutePath.DYNAMICS_MAIN)
class DynamicsFragment : BaseComposeFragment() {
    private val viewModel: DynamicsViewModel by viewModels()

    @Composable
    override fun Content() {
        LiJieMusicTheme{
            DynamicScreen(viewModel)
        }
    }
}