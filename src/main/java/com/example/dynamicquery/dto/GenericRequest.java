package com.example.dynamicquery.dto;

import java.util.*;

public class GenericRequest {
    private String entity;
    private List<String> fields;
    private Map<String, Object> filters = new HashMap<>();
    private int page = 0;
    private int size = 20;
    private String sort;            // legacy "field,asc"
    private List<SortSpec> sorts;   // multi-column sort
    private boolean distinct;
    private boolean export = false;

    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }

    public List<String> getFields() { return fields; }
    public void setFields(List<String> fields) { this.fields = fields; }

    public Map<String, Object> getFilters() { return filters; }
    public void setFilters(Map<String, Object> filters) { this.filters = filters; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }

    public List<SortSpec> getSorts() { return sorts; }
    public void setSorts(List<SortSpec> sorts) { this.sorts = sorts; }

    public boolean isDistinct() { return distinct; }
    public void setDistinct(boolean distinct) { this.distinct = distinct; }

    public boolean isExport() { return export; }
    public void setExport(boolean export) { this.export = export; }

    public static class SortSpec {
        private String field;
        private String direction;

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }

        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }
    }
}
