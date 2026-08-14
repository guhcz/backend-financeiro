package com.personal.backend_financeiro.repository;

import com.personal.backend_financeiro.dto.category.CategoryResponse;
import com.personal.backend_financeiro.dto.transaction.TransactionFilterRequest;
import com.personal.backend_financeiro.dto.transaction.TransactionResponse;
import com.personal.backend_financeiro.dto.transaction.TransactionType;
import com.personal.backend_financeiro.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Backs the read-only GET /api/v1/transactions view. There is no TransactionEntity: expenses
 * and incomes are unioned with a native UNION ALL query so pagination, sorting and
 * totalElements are computed by the database itself, instead of stitching together two
 * separately-paged Spring Data queries in Java (which would break both ordering and counts).
 */
@Repository
@RequiredArgsConstructor
public class TransactionRepository {

	private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("date", "amount");

	/**
	 * month/year filter the financial competence of each row (see CompetenceResolver: month
	 * after the date for CARD + CREDIT, the record's own date otherwise) -- additive to
	 * startDate/endDate, which keep meaning "real date" so existing callers are unaffected.
	 * Expenses have their competence pre-computed and stored (billing_month/billing_year);
	 * incomes always count towards their own income_date's month (no stored column needed), so
	 * the income block compares :year/:month directly against income_date.
	 */
	private static final String FILTERED_UNION = """
			SELECT e.id AS id, 'EXPENSE' AS type, e.description AS description, e.amount AS amount,
			       e.expense_date AS occurred_on, tm.name AS method,
			       (e.recurring_expense_id IS NOT NULL) AS recurring,
			       e.generated_automatically AS generated_automatically, e.notes AS notes,
			       e.created_at AS created_at,
			       c.id AS category_id, c.name AS category_name, c.color AS category_color,
			       c.icon AS category_icon, c.active AS category_active,
			       e.billing_month AS billing_month, e.billing_year AS billing_year
			FROM expenses e
			JOIN categories c ON c.id = e.category_id
			JOIN transaction_methods tm ON tm.id = e.transaction_method_id
			WHERE e.user_id = :userId AND e.active = true AND :includeExpense
			  AND (:categoryId IS NULL OR e.category_id = :categoryId)
			  AND (:startDate IS NULL OR e.expense_date >= :startDate)
			  AND (:endDate IS NULL OR e.expense_date <= :endDate)
			  AND (:year IS NULL OR :month IS NULL OR (e.billing_year = :year AND e.billing_month = :month))
			  AND (:descriptionPattern IS NULL OR LOWER(e.description) LIKE :descriptionPattern)
			  AND (:recurring IS NULL OR (e.recurring_expense_id IS NOT NULL) = :recurring)
			UNION ALL
			SELECT i.id, 'INCOME', i.description, i.amount,
			       i.income_date, i.receipt_method,
			       (i.recurring_income_id IS NOT NULL),
			       i.generated_automatically, i.notes, i.created_at,
			       c.id, c.name, c.color, c.icon, c.active,
			       CAST(NULL AS INTEGER), CAST(NULL AS INTEGER)
			FROM incomes i
			JOIN categories c ON c.id = i.category_id
			WHERE i.user_id = :userId AND i.active = true AND :includeIncome
			  AND (:categoryId IS NULL OR i.category_id = :categoryId)
			  AND (:startDate IS NULL OR i.income_date >= :startDate)
			  AND (:endDate IS NULL OR i.income_date <= :endDate)
			  AND (:year IS NULL OR :month IS NULL
			       OR (EXTRACT(YEAR FROM i.income_date) = :year AND EXTRACT(MONTH FROM i.income_date) = :month))
			  AND (:descriptionPattern IS NULL OR LOWER(i.description) LIKE :descriptionPattern)
			  AND (:recurring IS NULL OR (i.recurring_income_id IS NOT NULL) = :recurring)
			""";

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public Page<TransactionResponse> search(Long userId, TransactionFilterRequest filter, Pageable pageable) {
		MapSqlParameterSource params = buildParams(userId, filter);

		long total = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM (" + FILTERED_UNION + ") t", params, Long.class);

		String dataSql = "SELECT * FROM (" + FILTERED_UNION + ") t ORDER BY " + buildOrderClause(pageable.getSort())
				+ " LIMIT :limit OFFSET :offset";
		params.addValue("limit", pageable.getPageSize(), Types.INTEGER);
		params.addValue("offset", pageable.getOffset(), Types.BIGINT);

		List<TransactionResponse> content = jdbcTemplate.query(dataSql, params, (rs, rowNum) -> {
			String type = rs.getString("type");
			long id = rs.getLong("id");
			CategoryResponse category = new CategoryResponse(
					rs.getLong("category_id"),
					rs.getString("category_name"),
					rs.getString("category_color"),
					rs.getString("category_icon"),
					rs.getBoolean("category_active"));
			return new TransactionResponse(
					id,
					TransactionType.valueOf(type),
					type + "-" + id,
					rs.getString("description"),
					rs.getBigDecimal("amount"),
					rs.getObject("occurred_on", LocalDate.class),
					rs.getString("method"),
					rs.getBoolean("recurring"),
					rs.getBoolean("generated_automatically"),
					rs.getString("notes"),
					category,
					(Integer) rs.getObject("billing_month"),
					(Integer) rs.getObject("billing_year"));
		});

		return new PageImpl<>(content, pageable, total);
	}

	private MapSqlParameterSource buildParams(Long userId, TransactionFilterRequest filter) {
		TransactionType type = filter.type();
		boolean includeExpense = type == null || type == TransactionType.EXPENSE;
		boolean includeIncome = type == null || type == TransactionType.INCOME;
		String descriptionPattern = filter.description() == null || filter.description().isBlank()
				? null
				: "%" + filter.description().toLowerCase() + "%";

		return new MapSqlParameterSource()
				.addValue("userId", userId, Types.BIGINT)
				.addValue("includeExpense", includeExpense, Types.BOOLEAN)
				.addValue("includeIncome", includeIncome, Types.BOOLEAN)
				.addValue("categoryId", filter.categoryId(), Types.BIGINT)
				.addValue("startDate", filter.startDate(), Types.DATE)
				.addValue("endDate", filter.endDate(), Types.DATE)
				.addValue("month", filter.month(), Types.INTEGER)
				.addValue("year", filter.year(), Types.INTEGER)
				.addValue("descriptionPattern", descriptionPattern, Types.VARCHAR)
				.addValue("recurring", filter.recurring(), Types.BOOLEAN);
	}

	private String buildOrderClause(Sort sort) {
		StringBuilder clause = new StringBuilder();
		for (Sort.Order order : sort) {
			if (!ALLOWED_SORT_PROPERTIES.contains(order.getProperty())) {
				throw new InvalidRequestException(
						"Ordenação não suportada: " + order.getProperty() + ". Use 'date' ou 'amount'.");
			}
			String column = "date".equals(order.getProperty()) ? "occurred_on" : "amount";
			clause.append("t.").append(column).append(order.isAscending() ? " ASC" : " DESC").append(", ");
		}
		if (clause.isEmpty()) {
			clause.append("t.occurred_on DESC, ");
		}
		clause.append("t.created_at DESC, t.type ASC, t.id DESC");
		return clause.toString();
	}

}
