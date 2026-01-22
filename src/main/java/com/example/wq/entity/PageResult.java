package com.example.wq.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 简化的分页响应对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页响应对象")
public class PageResult<T> {

    /**
     * 数据列表
     */
    @Schema(description = "数据列表")
    private List<T> content;

    /**
     * 总记录数
     */
    @Schema(description = "总记录数", example = "100")
    private long totalElements;

    /**
     * 总页数
     */
    @Schema(description = "总页数", example = "10")
    private int totalPages;

    /**
     * 当前页码（从0开始）
     */
    @Schema(description = "当前页码（从0开始）", example = "0")
    private int number;

    /**
     * 每页大小
     */
    @Schema(description = "每页大小", example = "10")
    private int size;

    /**
     * 当前页实际记录数
     */
    @Schema(description = "当前页实际记录数", example = "10")
    private int numberOfElements;

    /**
     * 是否第一页
     */
    @Schema(description = "是否第一页", example = "true")
    private boolean first;

    /**
     * 是否最后一页
     */
    @Schema(description = "是否最后一页", example = "false")
    private boolean last;

    /**
     * 是否为空
     */
    @Schema(description = "是否为空", example = "false")
    private boolean empty;

    /**
     * 从 Spring Data Page 对象转换
     */
    public static <T> PageResult<T> of(org.springframework.data.domain.Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setContent(page.getContent());
        result.setTotalElements(page.getTotalElements());
        result.setTotalPages(page.getTotalPages());
        result.setNumber(page.getNumber());
        result.setSize(page.getSize());
        result.setNumberOfElements(page.getNumberOfElements());
        result.setFirst(page.isFirst());
        result.setLast(page.isLast());
        result.setEmpty(page.isEmpty());
        return result;
    }
}
