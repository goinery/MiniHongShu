package com.hongshu.web.auth;

import com.hongshu.web.domain.entity.WebUser;
import org.springframework.beans.BeanUtils;

public final class WebUserViews {
    private WebUserViews() {
    }

    public static WebUser publicProfile(WebUser user) {
        if (user == null) {
            return null;
        }
        WebUser view = new WebUser();
        BeanUtils.copyProperties(user, view);
        view.setPassword(null);
        view.setPhone(null);
        view.setEmail(null);
        view.setLoginIp(null);
        view.setLoginDate(null);
        view.setRemark(null);
        return view;
    }

    /** 仅用于当前登录会员本人的资料响应。 */
    public static WebUser selfProfile(WebUser user) {
        WebUser view = publicProfile(user);
        if (view != null) {
            view.setPhone(user.getPhone());
            view.setEmail(user.getEmail());
            view.setLoginIp(user.getLoginIp());
            view.setLoginDate(user.getLoginDate());
        }
        return view;
    }
}
