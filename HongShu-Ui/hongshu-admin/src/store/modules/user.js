
import { defineStore } from 'pinia'
import { login, logout, getInfo } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { resolveAvatarUrl } from '@/utils/avatar'

const useUserStore = defineStore(
  'user',
  {
    state: () => ({
      token: getToken(),
      id: '',
      name: '',
      avatar: '',
      roles: [],
      permissions: []
    }),
    actions: {
      // 登录
      login(userInfo) {
        const username = userInfo.username.trim()
        const password = userInfo.password
        const code = userInfo.code
        const uuid = userInfo.uuid
        return login(username, password, code, uuid).then(res => {
          setToken(res.token)
          this.token = res.token
        })
      },
      // 获取用户信息
      getInfo() {
        return getInfo().then(res => {
            const user = res.user
            const avatar = resolveAvatarUrl(user.avatar);

            if (res.roles && res.roles.length > 0) { // 验证返回的roles是否是一个非空数组
              this.roles = res.roles
              this.permissions = res.permissions
            } else {
              this.roles = ['ROLE_DEFAULT']
              this.permissions = []
            }
            this.id = user.userId
            this.name = user.userName
            this.avatar = avatar
            return res
        })
      },
      // 本地会话清理由 store 统一管理，不依赖已失效的远端凭证。
      clearSession() {
        this.token = ''
        this.id = ''
        this.name = ''
        this.avatar = ''
        this.roles = []
        this.permissions = []
        removeToken()
      },
      // 退出系统
      logOut() {
        return logout(this.token).catch(() => {
          // 请求层已提示远端错误，退出操作仍应结束本地会话。
        }).finally(() => this.clearSession())
      }
    }
  })

export default useUserStore
