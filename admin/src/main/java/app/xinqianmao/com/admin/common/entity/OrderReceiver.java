package app.xinqianmao.com.admin.common.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_order_receiver")
public class OrderReceiver {
    @TableId("order_no")
    private String orderNo;
    private String receiver;
    private String contact;
    private String provinceCode;
    private String cityCode;
    private String countyCode;
    private String address;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
}
