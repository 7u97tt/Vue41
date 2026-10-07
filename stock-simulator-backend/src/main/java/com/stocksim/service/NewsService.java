package com.stocksim.service;

import com.stocksim.entity.News;
import com.stocksim.mapper.NewsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsMapper newsMapper;

    public List<News> listAll() {
        return newsMapper.selectAllOrderByPublishTimeDesc();
    }
}
