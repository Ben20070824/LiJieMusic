package com.example.searchpage

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.base.BaseFragment
import com.example.searchpage.databinding.FragmentSearchPageBinding
import com.example.searchpage.adapter.PlaylistAdapter
import com.example.therouter.NavigationFragmentUtil
import com.example.therouter.RouteParams
import com.example.therouter.RoutePath
import com.example.util.DrawerUtil
import com.therouter.TheRouter
import com.therouter.router.Route
import kotlinx.coroutines.launch
import kotlin.getValue

@Route(path = RoutePath.SEARCH_PAGE_MAIN)
class SearchPageFragment : BaseFragment<FragmentSearchPageBinding>(FragmentSearchPageBinding::inflate){

    private val viewModel: SearchPageViewmodel by viewModels()
    private val Adapter = PlaylistAdapter { playlistId ->
        navigateToPlaylist(playlistId)
    }

    override fun initView() {
        binding.rvgedan.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvgedan.adapter = Adapter
        viewModel.fetchRecommendPlaylists()
    }

    override fun initEvent() {
        super.initEvent()

        binding.tvFakeSearch.setOnClickListener {
            val fragment = TheRouter.build(RoutePath.SEARCH_MAIN).createFragment<Fragment>()
            (activity as? NavigationFragmentUtil)?.addFragment(fragment)
        }
        binding.btnDrawer.setOnClickListener {
            (activity as? DrawerUtil)?.openDrawer()
        }
        clickEvent()
    }

    override fun initObservers() {
        lifecycleScope.launch {
            viewModel.playListFlow.collect { playlists ->
                if (playlists.isNotEmpty()) {
                    Adapter.submitList(playlists)
                }
            }
        }
    }

    private fun navigateToPlaylist(playlistId: Long) {
        val fragment = TheRouter.build(RoutePath.PLAYLIST_MAIN)
            .withString(RouteParams.PlaylistParams.PLAYLIST_ID, playlistId.toString())
            .createFragment<Fragment>()
        (activity as? NavigationFragmentUtil)?.addFragment(fragment)
    }

    override fun onDestroyView() {
        _binding?.rvgedan?.adapter = null
        super.onDestroyView()
    }

    fun clickEvent(){
        binding.imgRank.setOnClickListener {
            val bottomSheet = WheelMenuBottomSheet(0)
            bottomSheet.show(childFragmentManager, "WheelMenu")
        }

        binding.imgSinger.setOnClickListener {
            val bottomSheet = WheelMenuBottomSheet(1)
            bottomSheet.show(childFragmentManager, "WheelMenu")
        }

        binding.imgGenre.setOnClickListener {
            val bottomSheet = WheelMenuBottomSheet(2)
            bottomSheet.show(childFragmentManager, "WheelMenu")
        }

        binding.imgAlbum.setOnClickListener {
            val bottomSheet = WheelMenuBottomSheet(3)
            bottomSheet.show(childFragmentManager, "WheelMenu")
        }
        binding.imgBook.setOnClickListener {
            val bottomSheet = WheelMenuBottomSheet(4)
            bottomSheet.show(childFragmentManager, "WheelMenu")
        }

    }
}