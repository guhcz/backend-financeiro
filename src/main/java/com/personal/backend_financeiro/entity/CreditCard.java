package com.personal.backend_financeiro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pure card metadata (closing/due day), never created or edited directly by the user -- it only
 * exists attached 1:1 to a {@link TransactionMethod} of type CARD, which is the actual
 * user-facing registration. closingDay/dueDay are informational only: financial competence for
 * card purchases does not derive an invoice cycle from them (see
 * com.personal.backend_financeiro.util.CompetenceResolver).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "credit_cards")
public class CreditCard extends Auditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "closing_day")
	private Integer closingDay;

	@Column(name = "due_day")
	private Integer dueDay;

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CreditCard other)) {
			return false;
		}
		return id != null && id.equals(other.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

}
