package com.stocksim.mapper;

import com.stocksim.entity.News;

import java.util.List;

public interface NewsMapper {

    List<News> selectAllOrderByPublishTimeDesc();
}
