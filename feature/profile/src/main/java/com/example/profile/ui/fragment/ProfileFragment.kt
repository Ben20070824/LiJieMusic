package com.example.profile.ui.fragment

import androidx.compose.runtime.Composable
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.base.BaseComposeFragment
import com.example.profile.ui.screen.ProfileScreen
import com.example.profile.ui.viewmodel.ProfileViewModel
import com.example.therouter.NavigationFragmentUtil
import com.example.therouter.RouteParams
import com.example.therouter.RoutePath
import com.therouter.TheRouter
import com.therouter.router.Route

@Route(path = RoutePath.PROFILE_MAIN)
class ProfileFragment : BaseComposeFragment() {
    private val viewModel: ProfileViewModel by viewModels()

    @Composable
    override fun Content() {
        ProfileScreen(viewModel){id->
            val fragment = TheRouter.build(RoutePath.PLAYLIST_MAIN)
                .withString(RouteParams.PlaylistParams.PLAYLIST_ID, id.toString())
                .createFragment<Fragment>()
            (activity as? NavigationFragmentUtil)?.addFragment(fragment)
        }
    }
}