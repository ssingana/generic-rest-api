package com.example.dynamicquery.service;

import com.example.dynamicquery.dto.GenericRequest;
import com.example.dynamicquery.dto.GenericRequest.SortSpec;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.*;

@Service
public class GenericQueryService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Map<String, Object> fetchData(GenericRequest request) throws ClassNotFoundException {

        Class<?> entityClass = Class.forName("com.example.dynamicquery.model." + request.getEntity());

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<?> root = cq.from(entityClass);

        Map<String, From<?, ?>> joins = new HashMap<>();

        // DISTINCT
        if (request.isDistinct()) {
            cq.distinct(true);
        }

        boolean hasFields = request.getFields() != null && !request.getFields().isEmpty();
        boolean singleFieldDistinct = request.isDistinct() && hasFields && request.getFields().size() == 1;

        // -----------------------------------------------------------------
        // Projections (using only multiselect)
        // -----------------------------------------------------------------
        if (singleFieldDistinct) {
            String f = request.getFields().get(0);
            Path<?> p = getPath(root, f, joins);
            cq.multiselect(p.alias(f));           // tuple of size 1

        } else if (!hasFields) {
            cq.multiselect(root);                 // tuple[0] = entity

        } else {
            List<Selection<?>> selections = new ArrayList<>();
            for (String field : request.getFields()) {
                Path<?> p = getPath(root, field, joins);
                selections.add(p.alias(field));
            }
            cq.multiselect(selections);
        }

        // -----------------------------------------------------------------
        // Filters
        // -----------------------------------------------------------------
        List<Predicate> predicates = buildPredicates(cb, root, joins, request.getFilters());
        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(new Predicate[0]));
        }

        // -----------------------------------------------------------------
        // Sorting
        // -----------------------------------------------------------------
        List<Order> orders = buildOrders(cb, root, joins, request);
        if (!orders.isEmpty()) {
            cq.orderBy(orders);
        }

        TypedQuery<Tuple> query = entityManager.createQuery(cq);

        if (!request.isExport()) {
            query.setFirstResult(request.getPage() * request.getSize());
            query.setMaxResults(request.getSize());
        }

        List<Tuple> tuples = query.getResultList();
        List<Map<String, Object>> data = new ArrayList<>();

        // -----------------------------------------------------------------
        // Map results
        // -----------------------------------------------------------------
        if (!hasFields) {
            for (Tuple t : tuples) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("entity", t.get(0));
                data.add(row);
            }
        } else if (singleFieldDistinct) {
            String f = request.getFields().get(0);
            for (Tuple t : tuples) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put(f, t.get(f));
                data.add(row);
            }
        } else {
            for (Tuple t : tuples) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (String f : request.getFields()) {
                    row.put(f, t.get(f));
                }
                data.add(row);
            }
        }

        // -----------------------------------------------------------------
        // Count total
        // -----------------------------------------------------------------
        Long total = countTotal(cb, entityClass, request);

        // -----------------------------------------------------------------
        // Export mode
        // -----------------------------------------------------------------
        if (request.isExport()) {
            return exportAsExcel(data, request.getEntity(), request.getFields());
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("content", data);
        resp.put("totalElements", total);
        resp.put("page", request.getPage());
        resp.put("size", request.getSize());
        resp.put("totalPages", (int) Math.ceil((double) total / request.getSize()));
        return resp;
    }

    // =====================================================================
    // Count query
    // =====================================================================
    private Long countTotal(CriteriaBuilder cb,
                            Class<?> entityClass,
                            GenericRequest request) throws ClassNotFoundException {

        CriteriaQuery<Long> countQ = cb.createQuery(Long.class);
        Root<?> countRoot = countQ.from(entityClass);
        Map<String, From<?, ?>> joins = new HashMap<>();

        List<Predicate> preds = buildPredicates(cb, countRoot, joins, request.getFilters());
        if (!preds.isEmpty()) {
            countQ.where(preds.toArray(new Predicate[0]));
        }

        if (request.isDistinct()
                && request.getFields() != null
                && request.getFields().size() == 1) {

            String f = request.getFields().get(0);
            Expression<?> expr = getPath(countRoot, f, joins);
            countQ.select(cb.countDistinct(expr));

        } else {
            countQ.select(cb.count(countRoot));
        }

        return entityManager.createQuery(countQ).getSingleResult();
    }

    // =====================================================================
    // Filters: equals, _like, _between, _inSubquery
    // =====================================================================
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Predicate> buildPredicates(CriteriaBuilder cb,
                                            Root<?> root,
                                            Map<String, From<?, ?>> joins,
                                            Map<String, Object> filters) throws ClassNotFoundException {

        List<Predicate> predicates = new ArrayList<>();
        if (filters == null) return predicates;

        for (Map.Entry<String, Object> e : filters.entrySet()) {
            String key = e.getKey();
            Object val = e.getValue();

            if (key.endsWith("_like")) {
                String field = key.substring(0, key.length() - "_like".length());
                Path<String> p = getPath(root, field, joins);
                predicates.add(
                        cb.like(cb.lower(p), "%" + val.toString().toLowerCase() + "%")
                );

            } else if (key.endsWith("_between")) {
                String field = key.substring(0, key.length() - "_between".length());
                List<?> range = (List<?>) val;
                if (range != null && range.size() >= 2) {
                    Comparable<?> from = normalizeComparable(range.get(0));
                    Comparable<?> to   = normalizeComparable(range.get(1));

                    Path<?> rawPath = getPath(root, field, joins);
                    Expression<Comparable> expr = (Expression<Comparable>) rawPath;

                    predicates.add(
                            cb.between(expr, (Comparable) from, (Comparable) to)
                    );
                }

            } else if (key.endsWith("_inSubquery")) {
                String field = key.substring(0, key.length() - "_inSubquery".length());
                Map<String, Object> subSpec = (Map<String, Object>) val;
                predicates.add(buildInSubqueryPredicate(cb, root, field, subSpec));

            } else {
                // equals
                Path<Object> p = getPath(root, key, joins);
                predicates.add(cb.equal(p, val));
            }
        }
        return predicates;
    }

    // =====================================================================
    // Sorting (multi + legacy single)
    // =====================================================================
    private List<Order> buildOrders(CriteriaBuilder cb,
                                    Root<?> root,
                                    Map<String, From<?, ?>> joins,
                                    GenericRequest request) {

        List<Order> orders = new ArrayList<>();

        if (request.getSorts() != null && !request.getSorts().isEmpty()) {
            for (SortSpec spec : request.getSorts()) {
                Path<?> p = getPath(root, spec.getField(), joins);
                if ("desc".equalsIgnoreCase(spec.getDirection())) {
                    orders.add(cb.desc(p));
                } else {
                    orders.add(cb.asc(p));
                }
            }
        } else if (request.getSort() != null && !request.getSort().isEmpty()) {
            String[] sp = request.getSort().split(",");
            Path<?> p = getPath(root, sp[0], joins);
            if (sp.length > 1 && "desc".equalsIgnoreCase(sp[1])) {
                orders.add(cb.desc(p));
            } else {
                orders.add(cb.asc(p));
            }
        }

        return orders;
    }

    // =====================================================================
    // Path / Join resolution (dot notation)
    // =====================================================================
    @SuppressWarnings("unchecked")
    private <T> Path<T> getPath(From<?, ?> root,
                                String field,
                                Map<String, From<?, ?>> joins) {
        if (!field.contains(".")) {
            return root.get(field);
        }

        String[] parts = field.split("\\.");
        From<?, ?> from = root;
        StringBuilder joinKey = new StringBuilder();

        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) {
                joinKey.append(".");
            }
            joinKey.append(parts[i]);
            String key = joinKey.toString();

            From<?, ?> existing = joins.get(key);
            if (existing == null) {
                existing = from.join(parts[i], JoinType.LEFT);
                joins.put(key, existing);
            }
            from = existing;
        }
        return (Path<T>) from.get(parts[parts.length - 1]);
    }

    private Comparable<?> normalizeComparable(Object o) {
        if (o == null) return null;
        if (o instanceof Comparable) return (Comparable<?>) o;
        return o.toString();
    }

    // =====================================================================
    // Subquery support (_inSubquery) – simplified & robust
    // =====================================================================
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Predicate buildInSubqueryPredicate(CriteriaBuilder cb,
                                               Root<?> root,
                                               String mainField,
                                               Map<String, Object> subSpec)
            throws ClassNotFoundException {

        String subEntity = (String) subSpec.get("entity");
        String subField = (String) subSpec.get("field");
        Map<String, Object> subFilters = (Map<String, Object>) subSpec.get("filters");

        if (subEntity == null || subField == null) {
            throw new IllegalArgumentException("Subquery spec must contain 'entity' and 'field'");
        }

        Class<?> subEntityClass = Class.forName("com.example.dynamicquery.model." + subEntity);

        // Use a simple, raw-typed subquery to avoid generic conflicts
        CriteriaQuery<?> outer = cb.createQuery();
        Subquery subquery = outer.subquery(Object.class);
        Root<?> subRoot = subquery.from(subEntityClass);
        Map<String, From<?, ?>> subJoins = new HashMap<>();

        // Apply filters on subquery entity (e.g. Department where name LIKE 'Sales')
        List<Predicate> subPreds = buildPredicates(
                cb,
                subRoot,
                subJoins,
                subFilters != null ? subFilters : Collections.emptyMap()
        );
        if (!subPreds.isEmpty()) {
            subquery.where(subPreds.toArray(new Predicate[0]));
        }

        Path<?> subPath = getPath(subRoot, subField, subJoins);
        subquery.select((Expression) subPath); // raw cast is acceptable here

        Path<?> mainPath = getPath(root, mainField, new HashMap<>());
        return ((Expression) mainPath).in(subquery);
    }

    // =====================================================================
    // Excel export
    // =====================================================================
    private Map<String, Object> exportAsExcel(List<Map<String, Object>> data,
                                              String entity,
                                              List<String> fields) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(entity != null ? entity : "export");
            CreationHelper ch = wb.getCreationHelper();

            List<String> headerFields =
                    (fields == null || fields.isEmpty())
                            ? deriveFieldsFromData(data)
                            : fields;

            // Header
            Row header = sheet.createRow(0);
            for (int i = 0; i < headerFields.size(); i++) {
                header.createCell(i).setCellValue(headerFields.get(i));
            }

            CellStyle dateStyle = wb.createCellStyle();
            short df = ch.createDataFormat().getFormat("yyyy-MM-dd");
            dateStyle.setDataFormat(df);

            // Rows
            for (int r = 0; r < data.size(); r++) {
                Row row = sheet.createRow(r + 1);
                Map<String, Object> rec = data.get(r);
                for (int c = 0; c < headerFields.size(); c++) {
                    String key = headerFields.get(c);
                    Object v = rec.get(key);
                    Cell cell = row.createCell(c);
                    if (v == null) {
                        cell.setBlank();
                    } else if (v instanceof Number) {
                        cell.setCellValue(((Number) v).doubleValue());
                    } else if (v instanceof LocalDate) {
                        cell.setCellValue((LocalDate) v);
                        cell.setCellStyle(dateStyle);
                    } else if (v instanceof Boolean) {
                        cell.setCellValue((Boolean) v);
                    } else {
                        cell.setCellValue(v.toString());
                    }
                }
            }

            for (int i = 0; i < headerFields.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            String fileName = System.getProperty("java.io.tmpdir") + "/"
                    + (entity != null ? entity : "export") + "_export.xlsx";

            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                wb.write(fos);
            }

            Map<String, Object> res = new HashMap<>();
            res.put("file", fileName);
            res.put("message", "Excel export successful");
            return res;
        } catch (Exception e) {
            throw new RuntimeException("Excel export failed: " + e.getMessage(), e);
        }
    }

    private List<String> deriveFieldsFromData(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) return Collections.emptyList();
        return new ArrayList<>(data.get(0).keySet());
    }
}
