package com.personal.backend_financeiro.entity;

import com.personal.backend_financeiro.enums.CardTransactionMode;
import com.personal.backend_financeiro.enums.RecurrenceFrequency;
import com.personal.backend_financeiro.enums.RecurrenceStatus;
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

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "recurring_expenses")
public class RecurringExpense extends Auditable {

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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RecurrenceFrequency frequency;

	@Column(name = "due_day")
	private Integer dueDay;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date")
	private LocalDate endDate;

	@Column(name = "installment_count")
	private Integer installmentCount;

	@Column(name = "next_generation_date", nullable = false)
	private LocalDate nextGenerationDate;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RecurrenceStatus status = RecurrenceStatus.ACTIVE;

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof RecurringExpense other)) {
			return false;
		}
		return id != null && id.equals(other.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

}
