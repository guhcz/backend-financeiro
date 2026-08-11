package com.personal.backend_financeiro.entity;

import com.personal.backend_financeiro.enums.ReceiptMethod;
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
@Table(name = "incomes")
@SQLDelete(sql = "UPDATE incomes SET active = false WHERE id = ?")
@SQLRestriction("active = true")
public class Income extends Auditable {

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

	@Column(name = "income_date", nullable = false)
	private LocalDate incomeDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "receipt_method", nullable = false, length = 20)
	private ReceiptMethod receiptMethod;

	@Column(length = 500)
	private String notes;

	@Builder.Default
	@Column(nullable = false)
	private boolean active = true;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recurring_income_id")
	private RecurringIncome recurringIncome;

	@Builder.Default
	@Column(name = "generated_automatically", nullable = false)
	private boolean generatedAutomatically = false;

	@Column(name = "recurrence_reference_year")
	private Integer recurrenceReferenceYear;

	@Column(name = "recurrence_reference_month")
	private Integer recurrenceReferenceMonth;

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Income other)) {
			return false;
		}
		return id != null && id.equals(other.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

}
