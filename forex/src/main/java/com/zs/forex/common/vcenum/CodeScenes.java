package com.zs.forex.common.vcenum;

/**
 * 验证码场景
 */

public enum CodeScenes {

    register("register_", "90001"),

    login("login_", "90001"),

    admin_login("admin_login_", "90001"),

    key("VkqaabvqZgdqjcm6ytseRw==", "-1"),

    USD("USD", "-1"),

    email("email", "1"),

    phone("phone", "0"),

    key_gog("REUMBDDU4MB3ACEROGRGXGXHLY4XUIHW", "-1"),

    reset_pwd("reset_pwd_", "90001"),

    trade_lock("trade_lock_", "90002"),

    normal_cancel_order("normal_cancel_order", "90003"),

    pair("{pair}", "-1"),

    close("close", "-1");

    private final String pev;

    private final String code;

    CodeScenes(String pev, String code) {
        this.pev = pev;
        this.code = code;
    }

    public String getPev() {
        return pev;
    }

    public String getCode() {
        return code;
    }
}
