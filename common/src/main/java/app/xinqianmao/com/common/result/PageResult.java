/**
 * File: PageResult.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * Paginated response wrapper.
 */
@Data
public class PageResult<T> {

    private List<T> items;
    private long counts;
    private long page;
    private long pages;
    private long pageSize;

    private PageResult() {}

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> r = new PageResult<>();
        r.items = page.getRecords();
        r.counts = page.getTotal();
        r.page = page.getCurrent();
        r.pages = page.getPages();
        r.pageSize = page.getSize();
        return r;
    }

    public static <T> PageResult<T> of(List<T> items, long total, long current, long pages, long size) {
        PageResult<T> r = new PageResult<>();
        r.items = items;
        r.counts = total;
        r.page = current;
        r.pages = pages;
        r.pageSize = size;
        return r;
    }
}
