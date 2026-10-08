package com.example.dto;

import java.util.List;

public class PageResult<T> {
    private final List<T> content;
    private final int pageNumber;
    private final int pageSize;
    private final long totalElements;
    private final int totalPages;

    public PageResult(List<T> content, int pageNumber, int pageSize, long totalElements) {
        this.content = content;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = pageSize > 0 ? (int) Math.ceil((double) totalElements / pageSize) : 0;
    }

    public List<T> getContent() {
        return content;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean hasNext() {
        return pageNumber < totalPages;
    }

    public boolean hasPrevious() {
        return pageNumber > 1;
    }

    @Override
    public String toString() {
        return "PageResult{" +
                "page=" + pageNumber + "/" + totalPages +
                ", pageSize=" + pageSize +
                ", totalElements=" + totalElements +
                ", contentSize=" + content.size() +
                '}';
    }
}
