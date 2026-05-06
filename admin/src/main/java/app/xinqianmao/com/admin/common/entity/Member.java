/**
 * File: Member.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.entity;

import app.xinqianmao.com.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * t_member — mini-program user.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_member")
public class Member extends BaseEntity {
    private String account;
    private String mobile;
    private String avatar;
    private String nickname;
}
