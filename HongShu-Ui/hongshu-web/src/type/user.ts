export interface UserLogin {
  phone: string;
  email: string;
  code: string;
  username: string;
  password: string;
}

export interface User {
  id: string;
  hsId: string | number;
  username: string;
  avatar: string;
  gender: string | number;
  phone?: string;
  email?: string;
  status?: string;
  createTime?: string;
  loginDate?: string;
  loginIp?: string;
  description: string;
  birthday: string;
  address: string;
  trendCount: number;
  followerCount: number;
  fanCount: number;
}
