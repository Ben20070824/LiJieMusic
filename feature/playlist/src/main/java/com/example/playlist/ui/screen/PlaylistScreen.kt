package com.example.playlist.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.playlist.R
import com.example.playlist.network.bean.Track
import com.example.playlist.ui.viewmodel.PlaylistViewModel
import com.example.playlist.ui.theme.AccentGreen
import com.example.playlist.ui.theme.LiJieMusicTheme
import com.example.util.ToastUtil
import kotlin.math.roundToInt

@Composable
fun PlaylistScreen(
    viewModel: PlaylistViewModel,
    playlistId: String,
    onBackClick: () -> Unit,
    onCoverClick: () -> Unit,
    onSongPlayClick: (id: String, songName: String, artistName: String) -> Unit,
    onSongNextPlayClick: (id: String, songName: String, artistName: String) -> Unit,
    onRemoveSong: (pid: Long, ids: String) -> Unit,
    onToggleSong: (pid: Long, ids: String) -> Unit,
) {
    val rvList by viewModel.rvList.collectAsStateWithLifecycle()
    val coverUrl by viewModel.coverUrl.collectAsStateWithLifecycle()
    val name by viewModel.name.collectAsStateWithLifecycle()
    val songCounts by viewModel.songCounts.collectAsStateWithLifecycle()
    val toastMsg by viewModel.toastMsg.collectAsStateWithLifecycle()

    val context = LocalContext.current
    LaunchedEffect(toastMsg) {
        toastMsg?.let { ToastUtil.popToast(it, context) }
    }

    // 本地列表：用于左滑删除 / 长按拖拽排序的乐观更新
    var tracks by remember(rvList) { mutableStateOf(rvList ?: emptyList()) }

    // 拖拽排序状态（提升到父组件，让所有 row 统一计算位移）
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var itemHeightPx by remember { mutableFloatStateOf(0f) }

    // 当前拖拽目标位（拖拽过程中列表顺序不变，仅做视觉让位）
    val dragTargetIndex = if (draggingId != null && itemHeightPx > 0f) {
        (dragStartIndex + (dragOffsetY / itemHeightPx).roundToInt()).coerceIn(0, tracks.size - 1)
    } else {
        -1
    }

    LiJieMusicTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
            if (rvList == null) {
                // 加载态
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = AccentGreen,
                    strokeWidth = 3.dp,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        HeaderContent(
                            coverUrl = coverUrl,
                            name = name,
                            songCounts = songCounts,
                            onBackClick = onBackClick,
                            onCoverClick = onCoverClick,
                            onPlayAll = {
                                tracks.firstOrNull()?.let { first ->
                                    val firstArtist =
                                        first.artists?.joinToString("|") { it.name } ?: "未知歌手"
                                    onSongPlayClick(
                                        first.id.toString(),
                                        first.name,
                                        firstArtist,
                                    )
                                }
                            },
                        )
                    }
                    if (tracks.isEmpty()) {
                        item {
                            EmptyContent()
                        }
                    } else {
                        itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                            val artistName =
                                track.artists?.joinToString("|") { it.name } ?: "未知歌手"
                            val isDragging = track.id == draggingId

                            // 非拖拽项的让位偏移：位于起止位置之间的项，朝相反方向平移一个身位
                            val shiftOffset = when {
                                draggingId == null || isDragging -> 0f
                                dragTargetIndex > dragStartIndex && index in (dragStartIndex + 1)..dragTargetIndex -> -itemHeightPx
                                dragTargetIndex < dragStartIndex && index in dragTargetIndex until dragStartIndex -> itemHeightPx
                                else -> 0f
                            }
                            val animatedShift by animateFloatAsState(
                                targetValue = shiftOffset,
                                animationSpec = tween(durationMillis = 120),
                                label = "shift",
                            )

                            SongRow(
                                track = track,
                                index = index,
                                artistName = artistName,
                                playlistId = playlistId,
                                isDragging = isDragging,
                                translationY = if (isDragging) dragOffsetY else animatedShift,
                                onMeasureHeight = { h -> if (itemHeightPx == 0f) itemHeightPx = h },
                                onDragStart = {
                                    draggingId = track.id
                                    dragStartIndex = index
                                    dragOffsetY = 0f
                                },
                                onDrag = { delta -> dragOffsetY += delta },
                                onDragEnd = {
                                    if (draggingId != null && dragTargetIndex >= 0 && dragTargetIndex != dragStartIndex) {
                                        val mutable = tracks.toMutableList()
                                        val moved = mutable.removeAt(dragStartIndex)
                                        mutable.add(dragTargetIndex, moved)
                                        tracks = mutable
                                        val ids = mutable.joinToString(
                                            separator = ",",
                                            prefix = "[",
                                            postfix = "]",
                                        ) { it.id.toString() }
                                        onToggleSong(playlistId.toLongOrNull() ?: 0L, ids)
                                    }
                                    draggingId = null
                                    dragOffsetY = 0f
                                    dragStartIndex = -1
                                },
                                onDragCancel = {
                                    draggingId = null
                                    dragOffsetY = 0f
                                    dragStartIndex = -1
                                },
                                onRemoveSong = { pid, ids ->
                                    tracks = tracks.filter { it.id != track.id }
                                    onRemoveSong(pid, ids)
                                },
                                onSongPlayClick = onSongPlayClick,
                                onSongNextPlayClick = onSongNextPlayClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderContent(
    coverUrl: String?,
    name: String?,
    songCounts: String?,
    onBackClick: () -> Unit,
    onCoverClick: () -> Unit,
    onPlayAll: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 顶部：返回按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "返回",
                    tint = MaterialTheme.colors.onBackground,
                )
            }
        }

        // 封面 + 信息
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = coverUrl,
                contentDescription = "歌单封面",
                modifier = Modifier
                    .size(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colors.surface)
                    .clickable(onClick = onCoverClick),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_play_next),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name ?: "",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = songCounts ?: "",
                    fontSize = 13.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            }
        }

        // 播放全部按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            TextButton(
                onClick = onPlayAll,
                shape = RoundedCornerShape(50),
                modifier = Modifier.height(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "播放全部",
                    fontSize = 13.sp,
                    color = MaterialTheme.colors.onBackground,
                )
            }
        }

        Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
    }
}

@Composable
private fun EmptyContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "暂无歌曲",
            fontSize = 15.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.45f),
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SongRow(
    track: Track,
    index: Int,
    artistName: String,
    playlistId: String,
    isDragging: Boolean,
    translationY: Float,
    onMeasureHeight: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onRemoveSong: (pid: Long, ids: String) -> Unit,
    onSongPlayClick: (id: String, songName: String, artistName: String) -> Unit,
    onSongNextPlayClick: (id: String, songName: String, artistName: String) -> Unit,
) {
    val pid = playlistId.toLongOrNull() ?: 0L

    // 左滑删除
    val dismissState = rememberDismissState(confirmStateChange = { dismissValue ->
        if (dismissValue == DismissValue.DismissedToStart) {
            onRemoveSong(pid, track.id.toString())
            true
        } else {
            false
        }
    })

    // 是否正在执行左滑删除
    val isDismissing = dismissState.targetValue == DismissValue.DismissedToStart

    // 整个 item（含背景）一起平移，避免移位时露出下层背景
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.translationY = translationY
                if (isDragging) {
                    alpha = 0.9f
                    scaleX = 1.02f
                    scaleY = 1.02f
                }
            }
            .zIndex(if (isDragging) 10f else 0f)
            .background(MaterialTheme.colors.background)
    ) {
        SwipeToDismiss(
            state = dismissState,
            directions = setOf(DismissDirection.EndToStart),
            background = {
                // 仅在真正删除时显示红色背景，其余时候透明（外层 Box 提供背景色）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isDismissing) Color(0xFFE53935) else Color.Transparent)
                        .padding(end = 24.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    if (isDismissing) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "删除",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            },
            dismissContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned {
                            if (it.size.height > 0f) onMeasureHeight(it.size.height.toFloat())
                        }
                        .pointerInput(track.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { onDragStart() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onDrag(dragAmount.y)
                                },
                                onDragEnd = onDragEnd,
                                onDragCancel = onDragCancel,
                            )
                        }
                ) {
                    SongItem(
                        track = track,
                        index = index,
                        artistName = artistName,
                        onPlayClick = {
                            onSongPlayClick(track.id.toString(), track.name, artistName)
                        },
                        onNextPlayClick = {
                            onSongNextPlayClick(track.id.toString(), track.name, artistName)
                        },
                    )
                }
            },
        )
    }
}

@Composable
private fun SongItem(
    track: Track,
    index: Int,
    artistName: String,
    onPlayClick: () -> Unit,
    onNextPlayClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 序号
        Text(
            text = "${index + 1}",
            fontSize = 14.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        // 封面：点击 → 下一首播放
        AsyncImage(
            model = track.album?.picUrl,
            contentDescription = "歌曲封面",
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colors.surface)
                .clickable(onClick = onNextPlayClick),
            contentScale = ContentScale.Crop,
        )
        // 歌曲信息
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (track.fee == 1) {
                    Icon(
                        painter = painterResource(R.drawable.ic_vip_tag),
                        contentDescription = "VIP",
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(20.dp),
                        tint = Color.Unspecified,
                    )
                }
                if (track.fee == 8) {
                    Icon(
                        painter = painterResource(R.drawable.ic_fee),
                        contentDescription = "付费",
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(20.dp),
                        tint = Color.Unspecified,
                    )
                }
            }
            Text(
                text = artistName,
                fontSize = 12.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        // 播放按钮
        IconButton(
            onClick = onPlayClick,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_play_next),
                contentDescription = "播放",
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp),
            )
        }
        // 更多按钮（原版未绑定事件，保留占位）
        IconButton(
            onClick = { },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_more),
                contentDescription = "更多",
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
