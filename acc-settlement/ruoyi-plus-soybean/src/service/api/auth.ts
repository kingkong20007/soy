import { request } from '../request';

/** Get image code */
export function fetchCaptchaCode() {
  return request<Api.Auth.CaptchaCode>({
    url: '/auth/code',
    method: 'get'
  });
}

/**
 * Login
 *
 * @param username User name
 * @param password Password
 */
export function fetchLogin(data: Api.Auth.PwdLoginForm) {
  return request<Api.Auth.LoginToken>({
    url: '/auth/login',
    method: 'post',
    headers: {
      isToken: false,
      isEncrypt: true,
      repeatSubmit: false
    },
    data
  });
}

/** Register */
export function fetchRegister(data: Api.Auth.RegisterForm) {
  return request<Api.Auth.LoginToken>({
    url: '/auth/register',
    method: 'post',
    headers: {
      isToken: false,
      isEncrypt: true,
      repeatSubmit: false
    },
    data
  });
}

/** Get user info */
export function fetchGetUserInfo() {
  return request<Api.Auth.UserInfo>({ url: '/system/user/getInfo' });
}

/** Logout */
export function fetchLogout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  });
}
