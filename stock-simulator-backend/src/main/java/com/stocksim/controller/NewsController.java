package com.stocksim.controller;

import com.stocksim.common.Result;
import com.stocksim.entity.News;
import com.stocksim.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping("/list")
    public Result<List<News>> list() {
        return Result.ok(newsService.listAll());
    }
}
