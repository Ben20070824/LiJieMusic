package com.example.therouter

import androidx.fragment.app.Fragment

/**
 * @author Ben
 * @date 2026/8/20
 * @description 用therouter跳转fragment式，将Fragment添加到容器的函数，由MainActivity实现
 * */
interface NavigationFragmentUtil  {
    fun addFragment(fragment: Fragment?)

    /** 登录成功后展示主页内容 */
    fun showMainContent()

    /** 退出登录后展示登录页 */
    fun showLogin()
}
