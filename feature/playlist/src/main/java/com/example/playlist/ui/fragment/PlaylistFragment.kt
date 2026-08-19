package com.example.playlist.ui.fragment

import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.base.Album
import com.example.base.Artist
import com.example.base.BaseComposeFragment
import com.example.base.PlayerManager
import com.example.base.SongDetail
import com.example.playlist.ui.screen.PlaylistScreen
import com.example.playlist.ui.viewmodel.PlaylistViewModel
import com.example.util.ToastUtil

class PlaylistFragment : BaseComposeFragment() {
    private val viewModel: PlaylistViewModel by viewModels()

    private val playlistId: String by lazy {
        arguments?.getString("playlistId") ?: ""
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent() // 系统级 API：隐式 Intent 调用系统相册
    ) { uri ->
        uri?.let { viewModel.uploadCover(playlistId.toLong(), it, requireActivity().contentResolver) }
    }

    @Composable
    override fun Content() {
        // 收集原始歌曲列表，映射为 PlayerManager 需要的精简对象，用于"播放全部"上下文
        val song by viewModel.song.collectAsStateWithLifecycle()
        val currentPlaylistSongs by rememberUpdatedState(
            song?.map { track ->
                val artistName = track.artists?.firstOrNull()?.name ?: "未知歌手"
                SongDetail(
                    id = track.id,
                    name = track.name,
                    ar = listOf(Artist(id = 0L, name = artistName)),
                    al = Album(id = 0L, name = "", picUrl = ""),
                    dt = 0,
                    fee = 0,
                )
            } ?: emptyList()
        )

        PlaylistScreen(
            viewModel = viewModel,
            playlistId = playlistId,
            onBackClick = { requireActivity().onBackPressedDispatcher.onBackPressed() },
            onCoverClick = { pickImageLauncher.launch("image/*") },
            onSongPlayClick = { id, songName, artistName ->
                // 清除旧歌单
                PlayerManager.clearPlaylist()
                // 赋值新歌单
                PlayerManager.updatePlaylist(currentPlaylistSongs)
                // 播放歌曲
                PlayerManager.playSong(id, songName, artistName)
            },
            onSongNextPlayClick = { id, songName, artistName ->
                PlayerManager.addSongToPlaylist(id, songName, artistName)
                ToastUtil.popToast("已添加至列表，下一首播放", requireActivity())
            },
            onRemoveSong = { pid, ids -> viewModel.removeSong(pid, ids) },
            onToggleSong = { pid, ids -> viewModel.toggleSong(pid, ids) },
        )
    }

    override fun initEvent() {
        viewModel.init(playlistId)
    }
}
