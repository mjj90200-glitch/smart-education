export interface UserInfo {
  id: number
  username: string
  realName: string
  avatar?: string
  role: 'STUDENT' | 'TEACHER' | 'ADMIN'
  grade?: string
  major?: string
  className?: string
  department?: string
  title?: string
}

export interface TokenInfo {
  token: string
  tokenType: string
  expiresIn: number
  userInfo: UserInfo
}
