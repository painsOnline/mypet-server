package app.xinqianmao.com.common;

public class GenPwd {
    public static void main(String[] args) throws Exception {
        app.xinqianmao.com.common.utils.PasswordUtil pu = new app.xinqianmao.com.common.utils.PasswordUtil();
        System.out.println(pu.encode(args[0]));
    }
}
