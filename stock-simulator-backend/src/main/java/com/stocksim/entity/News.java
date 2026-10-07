package com.stocksim.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class News {

    private Long id;
    private String title;
    private String content;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
}
