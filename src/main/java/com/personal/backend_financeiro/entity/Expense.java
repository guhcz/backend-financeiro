package com.personal.backend_financeiro.entity;

import com.personal.backend_financeiro.enums.CardTransactionMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "expenses")
@SQLDelete(sql = "UPDATE expenses SET active = false WHERE id = ?")
@SQLRestriction("active = true")
public class Expense extends Auditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@Column(nullable = false, length = 255)
	private String description;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(name = "expense_date", nullable = false)
	private LocalDate expenseDate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "transaction_method_id", nullable = false)
	private TransactionMethod transactionMethod;

	/**
	 * Only meaningful when transactionMethod.type == CARD (required in that case, null
	 * otherwise) -- see CompetenceResolver.validateCardTransactionMode.
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "card_transaction_mode", length = 10)
	private CardTransactionMode cardTransactionMode;

	@Column(length = 500)
	private String notes;

	@Builder.Default
	@Column(nullable = false)
	private boolean active = true;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recurring_expense_id")
	private RecurringExpense recurringExpense;

	@Builder.Default
	@Column(name = "generated_automatically", nullable = false)
	private boolean generatedAutomatically = false;

	@Column(name = "recurrence_reference_year")
	private Integer recurrenceReferenceYear;

	@Column(name = "recurrence_reference_month")
	private Integer recurrenceReferenceMonth;

	/**
	 * Financial competence: the month/year this expense actually impacts plannings, the monthly
	 * limit and reports (see {@link com.personal.backend_financeiro.util.CompetenceResolver}).
	 * Always server-computed, never trusted from client input.
	 */
	@Column(name = "billing_month", nullable = false)
	private Integer billingMonth;

	@Column(name = "billing_year", nullable = false)
	private Integer billingYear;

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Expense other)) {
			return false;
		}
		return id != null && id.equals(other.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

}
