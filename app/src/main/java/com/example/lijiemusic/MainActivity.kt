package com.example.lijiemusic

import android.graphics.Color
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.base.BaseActivity
import com.example.base.LocalPlaylistManager
import com.example.base.MediaControllerHelper
import com.example.base.PlayerManager
import com.example.comment.ui.CommentFragment
import com.example.lijiemusic.databinding.ActivityMainBinding
import com.example.lijiemusic.databinding.HeadLayoutBinding
import com.example.home.HomeFragment
import com.example.login.network.LoginApi
import com.example.login.ui.fragment.LoginFragment
import com.example.login.ui.fragment.MailFragment
import com.example.login.ui.fragment.ScanFragment
import com.example.model.UserManager
import com.example.net.CookieManager
import com.example.net.RetrofitClient
import com.example.player.PlayerViewModel
import com.example.player.fragment.PlaylistBottomSheet
import com.example.player.fragment.PlayerContainerFragment
import com.example.player.fragment.PlayerFragment
import com.example.profile.ui.fragment.ProfileFragment
import com.example.searchpage.SearchPageFragment
import com.example.therouter.NavigationFragmentUtil
import com.example.therouter.RoutePath
import com.example.util.DrawerUtil
import com.example.video.fragment.VideoFragment
import com.therouter.TheRouter
import com.therouter.router.Route
import kotlinx.coroutines.launch

@Route(path = RoutePath.APP_MAIN)
class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate), DrawerUtil,
    NavigationFragmentUtil {
    private val viewModel: PlayerViewModel by viewModels()
    private var _headBinding: HeadLayoutBinding? = null
    private val headBinding get() = _headBinding!!
    private val originalConstraintSet by lazy {
        ConstraintSet().apply {
            clone(binding.main)
        }
    }
    private var mediaControllerHelper: MediaControllerHelper? = null
    private val containerId = R.id.fl_container
    /** 防止底部导航 programmatic 选中触发监听器造成重入 */
    private var isTabSelectionProgrammatic = false

    override fun initView() {
        super.initView()
        val headerView = binding.navDrawer.getHeaderView(0)
        _headBinding = HeadLayoutBinding.bind(headerView)

        lifecycleScope.launch {
            UserManager.profile.collect { profile ->
                profile?.apply {
                    Glide.with(this@MainActivity).load(profile.avatarUrl)
                        .into(headBinding.ivDrawerAvatar)
                    headBinding.tvDrawerUsername.text = profile.nickname
                }
            }
        }
        val savedPlaylist = LocalPlaylistManager.getPlaylist(this)
        PlayerManager.updatePlaylist(savedPlaylist)
        binding.drawerlayout.setStatusBarBackgroundColor(Color.TRANSPARENT)
    }

    override fun initEvent() {
        super.initEvent()

        // ===== 启动页逻辑（合并自 LaunchActivity）=====
        binding.ivSplashCover.visibility = View.VISIBLE
        lifecycleScope.launch {
            val loggedIn = checkLoginStatus()
            binding.ivSplashCover.visibility = View.GONE
            if (loggedIn) showMainContent() else showLogin()
        }

        // ===== 抽屉菜单 =====
        binding.navDrawer.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_dynamics -> {
                    val fragment =
                        TheRouter.build(RoutePath.DYNAMICS_MAIN).createFragment<Fragment>()
                    addFragment(fragment)
                }

                R.id.menu_logout -> {
                    showLogoutDialog()
                }
            }
            binding.drawerlayout.closeDrawers()
            true
        }

        // ===== 底部导航 =====
        binding.bottomNavView.setOnItemSelectedListener { menuItem ->
            if (isTabSelectionProgrammatic) return@setOnItemSelectedListener true
            when (menuItem.itemId) {
                R.id.fragment_home -> switchTab(RoutePath.HOME_MAIN)
                R.id.fragment_search_page -> switchTab(RoutePath.SEARCH_PAGE_MAIN)
                R.id.fragment_video -> switchTab(RoutePath.VIDEO_MAIN)
                R.id.fragment_profile -> switchTab(RoutePath.PROFILE_MAIN)
            }
            true
        }

        // 监听回退栈变化，同步 UI（底部导航/迷你播放器显隐）
        supportFragmentManager.addOnBackStackChangedListener {
            val current = supportFragmentManager.findFragmentById(containerId)
            if (current != null) updateUIForFragment(current)
        }

        initNav()
    }

    // ===== 启动登录状态校验（原 LaunchActivity 逻辑）=====
    private suspend fun checkLoginStatus(): Boolean {
        if (!CookieManager.hasCookie()) return false
        val api = RetrofitClient.createApi(LoginApi::class.java)
        return try {
            val loginStatus = api.getLoginStatus()
            if (loginStatus.data.code == 200) {
                UserManager.account.value = loginStatus.data.account
                UserManager.profile.value = loginStatus.data.profile
                // 刷新 cookie
                try {
                    val refresh = api.refreshLoginStatus()
                    val musicU = extractMusicU(refresh.cookie)
                    if (musicU != null) {
                        CookieManager.injectCookie(musicU)
                        Log.d("MUSIC_U", musicU)
                    }
                } catch (_: Exception) {
                    Log.d("ljh", "cookie刷新失败，下次启动再试")
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.d("ljh", "我了个雷，初始化出问题了捏 ${e.message}")
            false
        }
    }

    private fun extractMusicU(cookieString: String): String? {
        // 匹配 MUSIC_U 及其所有 cookie 属性（path, max-age, domain 等），
        // 遇到下一个大写开头的 cookie 名或字符串结束就停
        val regex = Regex("MUSIC_U=[^;]+(; [a-z-]+(=[^;]*)?)*")
        return regex.find(cookieString)?.value
    }

    // ===== 底部导航 Tab 切换（替换内容，不入栈）=====
    private fun switchTab(route: String) {
        val fragment = TheRouter.build(route).createFragment<Fragment>() ?: return
        // 切换 Tab 时清空回退栈，保证 Tab 页为根
        supportFragmentManager.popBackStackImmediate(
            null,
            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
        supportFragmentManager.beginTransaction()
            .replace(containerId, fragment, fragment.javaClass.simpleName)
            .commit()
        updateUIForFragment(fragment)
    }

    // ===== NavigationFragmentUtil 实现 =====

    /** 登录成功：展示主页内容 */
    override fun showMainContent() {
        binding.bottomNavView.visibility = View.VISIBLE
        switchTab(RoutePath.HOME_MAIN)
        isTabSelectionProgrammatic = true
        binding.bottomNavView.selectedItemId = R.id.fragment_home
        isTabSelectionProgrammatic = false
    }

    /** 退出登录：展示登录页 */
    override fun showLogin() {
        supportFragmentManager.popBackStackImmediate(
            null,
            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
        val fragment = TheRouter.build(RoutePath.LOGIN_MAIN).createFragment<Fragment>() ?: return
        supportFragmentManager.beginTransaction()
            .replace(containerId, fragment, fragment.javaClass.simpleName)
            .commit()
        // 登录页隐藏底部导航和迷你播放器
        binding.bottomNavView.visibility = View.GONE
        binding.layoutMiniPlayer.visibility = View.GONE
    }

    /** 推入一个新页面（入栈，可返回）*/
    override fun addFragment(fragment: Fragment?) {
        if (fragment == null) return

        // 确保 TheRouter 的 @Autowired 参数在 fragment 初始化前注入
        TheRouter.inject(fragment)

        val tag = fragment.javaClass.simpleName

        supportFragmentManager.beginTransaction()
            .replace(containerId, fragment, tag)
            .addToBackStack(tag)
            .commit()
        updateUIForFragment(fragment)
    }

    // ===== 迷你播放器 =====

    private fun initNav() {
        binding.ivMiniCover.setOnClickListener {
            val fragment = TheRouter.build(RoutePath.PLAYER_CONTAINER).createFragment<Fragment>()
            addFragment(fragment)
        }

        binding.ivMiniPlaylist.setOnClickListener {
            val playlistDialog = PlaylistBottomSheet()
            playlistDialog.show(supportFragmentManager, "PlaylistDialogTag")
        }
        binding.ivMiniPlay.setOnClickListener {
            PlayerManager.togglePlayPause()
        }

        initMiniPlayer()
        PlayerManager.initPlayer(this)
    }

    private fun initMiniPlayer() {
        //初始化 Controller 并连接服务
        mediaControllerHelper =
            MediaControllerHelper(this, object : MediaControllerHelper.MediaControllerListener {
                override fun onConnected() {}
                override fun onPlayingStateChanged(isPlaying: Boolean) {}
                override fun onDurationChanged(duration: Long) {}
                override fun onPositionChanged(position: Long) {}
                override fun onMediaItemChanged(mediaItem: MediaItem) {}
                override fun onPlaybackEnded() {}
            })
        mediaControllerHelper?.connect()

        // ===== UI 收集器：生命周期重启时重新收集无副作用，保留在 repeatOnLifecycle 里 =====
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    PlayerManager.isPlaying.collect { isPlaying ->
                        if (isPlaying) {
                            binding.ivMiniPlay.setImageResource(R.drawable.play)
                        } else {
                            binding.ivMiniPlay.setImageResource(R.drawable.pause)
                        }
                    }
                }
                launch {
                    viewModel.songName.collect { name ->
                        binding.tvMiniSong.text = name
                    }
                }
                launch {
                    viewModel.coverUrl.collect { cover ->
                        Glide.with(this@MainActivity)
                            .load(cover)
                            .transform(RoundedCorners(16))
                            .into(binding.ivMiniCover)
                    }
                }
            }
        }

        lifecycleScope.launch {
            launch {
                PlayerManager.currentSong.collect { song ->
                    if (song != null) {
                        Log.d("hyj", "【全局】大管家切歌了！指派ViewModel去请求！ID: ${song.id}")
                        viewModel.fetchMusicUrl(song.id.toString())
                        viewModel.fetchSongDetail(song.id.toString())
                        viewModel.fetchLyric(song.id.toString())
                        viewModel.checkSongIsLiked(song.id.toString())
                    }
                }
            }
            launch {
                viewModel.currentSong.collect { songData ->
                    if (songData != null && !songData.url.isNullOrEmpty()) {
                        val url = songData.url
                        Log.d("hyj", "【全局】拿到歌曲URL，准备出声: ${songData.url}")
                        PlayerManager.startPlayEngine(songData.id.toString(), url.toString())
                    }
                }
            }
        }
    }

    //新增：页面销毁时，断开连接，防止内存泄漏
    override fun onDestroy() {
        super.onDestroy()
        mediaControllerHelper?.disconnect()
        mediaControllerHelper = null
    }

    override fun openDrawer() {
        binding.drawerlayout.openDrawer(binding.navDrawer)
    }

    override fun closeDrawer() {
        binding.drawerlayout.closeDrawer(binding.navDrawer)
    }

    private fun showLogoutDialog() {
        LogoutDialog().show(supportFragmentManager, "logout")
    }

    override fun onStop() {
        super.onStop()

        Log.d("hyj_debug", "==== 触发了 onStop 生命周期 ====")
        val currentPlaylist = PlayerManager.playlist.value

        Log.d("hyj_debug", "准备保存，当前列表歌曲数量: ${currentPlaylist.size}")

        if (currentPlaylist.isNotEmpty()) {
            LocalPlaylistManager.savePlaylist(this, currentPlaylist)
            Log.d("hyj_debug", "==== 保存成功！ ====")
        } else {
            Log.d("hyj_debug", "==== 列表是空的，放弃保存！ ====")
        }
    }

    // ============ 核心 UI 更新逻辑 ============
    private fun updateUIForFragment(fragment: Fragment) {
        // 判断当前 Fragment 是否需要隐藏底部导航和小播放器
        val shouldHideBottomBar = fragment is PlayerFragment
                || fragment is PlayerContainerFragment
                || fragment is CommentFragment
                || fragment is LoginFragment
                || fragment is MailFragment
                || fragment is ScanFragment

        if (shouldHideBottomBar) {
            // 隐藏底部导航和小播放器
            binding.bottomNavView.visibility = View.GONE
            binding.layoutMiniPlayer.visibility = View.GONE
        } else {
            // 显示底部导航和小播放器
            binding.bottomNavView.visibility = View.VISIBLE
            binding.layoutMiniPlayer.visibility = View.VISIBLE
        }

        // 判断是否需要调整 mini player 的位置（让它在底部导航上方）
        val isMainPage = fragment is HomeFragment ||
                fragment is SearchPageFragment ||
                fragment is VideoFragment ||
                fragment is ProfileFragment

        if (isMainPage) {
            // 主页面：使用原来的约束（mini player 在底部导航上方）
            originalConstraintSet.applyTo(binding.main)
        } else {
            // 非主页面（没有底部导航）：mini player 直接贴在底部
            val constraintSet = ConstraintSet()
            constraintSet.clone(binding.main)
            constraintSet.connect(
                R.id.layout_mini_player,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM,
                5
            )
            constraintSet.applyTo(binding.main)
        }
    }
}
