/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.firefly.common.core.queries;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Custom pagination request class that provides additional functionality
 * on top of Spring's PageRequest.
 */
public class PaginationRequest implements Pageable {

    private final PageRequest pageRequest;

    /**
     * Creates a new {@link PaginationRequest} with sort parameters applied.
     *
     * @param page zero-based page index, must not be negative
     * @param size the size of the page to be returned, must be greater than 0
     * @param direction the direction of the sort (e.g., "ASC" or "DESC")
     * @param properties the properties to sort by
     * @return a new {@link PaginationRequest}
     */
    public static PaginationRequest of(int page, int size, String direction, String... properties) {
        Sort sort = Sort.by(Sort.Direction.fromString(direction), properties);
        return new PaginationRequest(PageRequest.of(page, size, sort));
    }

    /**
     * Creates a new {@link PaginationRequest} with the given {@link PageRequest}.
     *
     * @param pageRequest the PageRequest to wrap
     */
    private PaginationRequest(PageRequest pageRequest) {
        this.pageRequest = pageRequest;
    }

    @Override
    public int getPageNumber() {
        return pageRequest.getPageNumber();
    }

    @Override
    public int getPageSize() {
        return pageRequest.getPageSize();
    }

    @Override
    public long getOffset() {
        return pageRequest.getOffset();
    }

    @Override
    public Sort getSort() {
        return pageRequest.getSort();
    }

    @Override
    public Pageable next() {
        return new PaginationRequest((PageRequest) pageRequest.next());
    }

    @Override
    public Pageable previousOrFirst() {
        return new PaginationRequest((PageRequest) pageRequest.previousOrFirst());
    }

    @Override
    public Pageable first() {
        return new PaginationRequest((PageRequest) pageRequest.first());
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new PaginationRequest((PageRequest) pageRequest.withPage(pageNumber));
    }

    @Override
    public boolean hasPrevious() {
        return pageRequest.hasPrevious();
    }
}
