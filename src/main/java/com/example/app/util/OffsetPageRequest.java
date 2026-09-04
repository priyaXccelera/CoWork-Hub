package com.example.app.util;

import java.io.Serializable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class OffsetPageRequest implements Pageable, Serializable {

  /** Maximum page size accepted across all list endpoints, to avoid unbounded result sets. */
  public static final int MAX_LIMIT = 100;

  private final int offset;
  private final int limit;
  private final Sort sort;

  public OffsetPageRequest(int offset, int limit, Sort sort) {
    if (offset < 0) {
      throw new IllegalArgumentException("offset must not be less than zero");
    }
    if (limit < 1) {
      throw new IllegalArgumentException("limit must not be less than one");
    }
    if (limit > MAX_LIMIT) {
      throw new IllegalArgumentException("limit must not exceed " + MAX_LIMIT);
    }
    this.offset = offset;
    this.limit = limit;
    this.sort = sort == null ? Sort.unsorted() : sort;
  }

  public static OffsetPageRequest of(int offset, int limit) {
    return new OffsetPageRequest(offset, limit, Sort.unsorted());
  }

  public static OffsetPageRequest of(int offset, int limit, Sort sort) {
    return new OffsetPageRequest(offset, limit, sort);
  }

  @Override
  public int getPageNumber() {
    return offset / limit;
  }

  @Override
  public int getPageSize() {
    return limit;
  }

  @Override
  public long getOffset() {
    return offset;
  }

  @Override
  public Sort getSort() {
    return sort;
  }

  @Override
  public Pageable next() {
    return new OffsetPageRequest(offset + limit, limit, sort);
  }

  @Override
  public Pageable previousOrFirst() {
    return new OffsetPageRequest(Math.max(offset - limit, 0), limit, sort);
  }

  @Override
  public Pageable first() {
    return new OffsetPageRequest(0, limit, sort);
  }

  @Override
  public Pageable withPage(int pageNumber) {
    return new OffsetPageRequest(pageNumber * limit, limit, sort);
  }

  @Override
  public boolean hasPrevious() {
    return offset > 0;
  }
}
