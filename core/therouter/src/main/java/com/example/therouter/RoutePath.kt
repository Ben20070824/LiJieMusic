package com.example.therouter
/**
 * 应用页面路由表。
 *
 * 命名规则：
 * 1. 常量名：模块名_页面名
 * 2. 路由值：/模块名/页面名
 * 3. 路径全部使用小写
 * 4. 页面参数不拼接到路由中
 */
object RoutePath {
    //app模块
    const val APP_MAIN = "/app/main"

    //login模块
    const val LOGIN_MAIN = "/login/main"
    const val LOGIN_SCAN = "/login/scan"
    const val LOGIN_MAIL = "/login/mail"

    //profile模块
    const val PROFILE_MAIN = "/profile/main"

    //playlist模块
    const val PLAYLIST_MAIN = "/playlist/main"

    //dynamics模块
    const val DYNAMICS_MAIN = "/dynamics/main"

    //home模块
    const val HOME_MAIN = "/home/main"

    //search模块（搜索页：热搜/联想/结果）
    const val SEARCH_MAIN = "/search/main"

    //searchpage模块（搜索 Tab：推荐歌单）
    const val SEARCH_PAGE_MAIN = "/searchpage/main"

    //video 模块（MV Tab）
    const val VIDEO_MAIN = "/video/main"

    //player 模块
    const val PLAYER_MAIN = "/player/main"
    const val PLAYER_CONTAINER = "/player/container"

    //comment 模块
    const val COMMENT_MAIN = "/comment/main"

    //mv 播放页
    const val MV_PLAY = "/mv/play"
}
object RouteParams {

    /** 歌单模块参数 */
    object PlaylistParams{
        const val PLAYLIST_ID = "playlistId"
    }

    /** 评论模块参数 */
    object CommentParams {
        const val SONG_ID = "songId"
        const val SONG_NAME = "songName"
        const val COVER_URL = "coverUrl"
    }

    /** MV 播放参数 */
    object MvParams {
        const val MV_ID = "mvId"
    }
}
