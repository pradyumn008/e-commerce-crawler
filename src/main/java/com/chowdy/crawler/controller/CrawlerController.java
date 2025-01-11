package com.chowdy.crawler.controller;

import com.chowdy.crawler.service.CrawlerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/crawler")
public class CrawlerController {

    private final CrawlerService crawlerService;

    public CrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @PostMapping("/discover")
    public Map<String, List<String>> discoverProductUrls(@RequestBody List<String> domains) {
        return crawlerService.crawlDomains(domains);
    }
}
