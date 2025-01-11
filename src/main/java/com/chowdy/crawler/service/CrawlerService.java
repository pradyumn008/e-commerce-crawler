package com.chowdy.crawler.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

@Service
public class CrawlerService {

    private static final Pattern PRODUCT_URL_PATTERN = Pattern.compile(".*(/product/|/item/|/p/).*", Pattern.CASE_INSENSITIVE);

    @Value("${crawler.max-depth}")
    private int maxDepth;

    @Value("${crawler.max-threads}")
    private int maxThreads;

    @Value("${crawler.timeout}")
    private int timeout;

    public Map<String, List<String>> crawlDomains(List<String> domains) {
        Map<String, List<String>> domainToUrlsMap = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(maxThreads);

        for (String domain : domains) {
            executor.submit(() -> {
                Set<String> productUrls = new ConcurrentSkipListSet<>();
                crawlDomain(domain, domain, 0, productUrls);
                domainToUrlsMap.put(domain, new ArrayList<>(productUrls));
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return domainToUrlsMap;
    }

    private void crawlDomain(String baseDomain, String currentUrl, int depth, Set<String> productUrls) {
        if (depth > maxDepth || productUrls.size() > 1000) return;

        try {
            Document doc = Jsoup.connect(currentUrl).timeout(timeout).get();
            Elements links = doc.select("a[href]");


            for (Element link : links) {

                String href = link.absUrl("href");

                if (isValidUrl(href) && isProductUrl(href)) {
                    productUrls.add(href);

                } else if (isSameDomain(baseDomain, href)) {
                    crawlDomain(baseDomain, href, depth + 1, productUrls);
                }
            }
        } catch (IOException e) {
            System.err.println("Error fetching URL: " + currentUrl);
        }
    }

    private boolean isProductUrl(String url) {
        return PRODUCT_URL_PATTERN.matcher(url).matches();
    }

    private boolean isSameDomain(String baseDomain, String url) {
        return url.startsWith(baseDomain);
    }

    private boolean isValidUrl(String url) {
        // Filter out invalid URLs
        return url != null && !url.isEmpty() &&
                !url.startsWith("javascript:") &&
                !url.contains("void") &&
                !url.equals("#");
    }
}

