type UserAutherization = {
    // 主键
    userid: number
    // 登录凭证
    token: string
}

export type User = UserAutherization & {
    // 用户名
    username: string
    // 电话号码
    mobile: string
    // 昵称（可以为空）
    nickname?: string
}

// 用户信息
export type UserProfile = {
  username: string
  nickname: string
  mobile: string
  avatar: string
  jobNumber?: string
}
// 登录返回的数据类型
export type LoginResult = {
  token: string
  profile: UserProfile
}

// 授权状态
export type UserState = {
  token: string
  profile: UserProfile
}
