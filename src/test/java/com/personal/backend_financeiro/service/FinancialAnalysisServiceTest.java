package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.category.CategoryResponse;
import com.personal.backend_financeiro.dto.financialanalysis.FinancialAnalysisResponse;
import com.personal.backend_financeiro.dto.financialanalysis.PaymentMethodAnalysisPageResponse;
import com.personal.backend_financeiro.dto.financialanalysis.PaymentMethodAnalysisResponse;
import com.personal.backend_financeiro.dto.planning.CategoryExpenseResponse;
import com.personal.backend_financeiro.entity.Category;
import com.personal.backend_financeiro.entity.Expense;
import com.personal.backend_financeiro.entity.TransactionMethod;
import com.personal.backend_financeiro.enums.CardTransactionMode;
import com.personal.backend_financeiro.enums.TransactionMethodType;
import com.personal.backend_financeiro.exception.InvalidRequestException;
import com.personal.backend_financeiro.mapper.CategoryMapper;
import com.personal.backend_financeiro.repository.BillingPeriodTotalProjection;
import com.personal.backend_financeiro.repository.CategoryRepository;
import com.personal.backend_financeiro.repository.CategoryTotalProjection;
import com.personal.backend_financeiro.repository.ExpenseRepository;
import com.personal.backend_financeiro.repository.IncomeRepository;
import com.personal.backend_financeiro.repository.MonthTotalProjection;
import com.personal.backend_financeiro.repository.PaymentMethodTotalProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialAnalysisServiceTest {

	@Mock
	private ExpenseRepository expenseRepository;
	@Mock
	private IncomeRepository incomeRepository;
	@Mock
	private CategoryRepository categoryRepository;
	@Mock
	private CategoryMapper categoryMapper;

	@InjectMocks
	private FinancialAnalysisService financialAnalysisService;

	private static final LocalDate START = LocalDate.of(2026, 6, 1);
	private static final LocalDate END = LocalDate.of(2026, 7, 31);

	@Test
	void getAnalysis_throwsInvalidRequest_whenStartDateAfterEndDate() {
		assertThatThrownBy(() -> financialAnalysisService.getAnalysis(1L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 7, 1)))
				.isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void getAnalysis_zeroFillsMonthsWithoutMovements_andComputesBalance() {
		when(expenseRepository.sumAmountGroupedByBillingPeriod(1L, 2026, 6, 2026, 7))
				.thenReturn(List.of(billingProjection(2026, 7, new BigDecimal("500.00"))));
		when(incomeRepository.sumAmountGroupedByMonth(1L, START, END))
				.thenReturn(List.of(monthProjection(2026, 6, new BigDecimal("1000.00"))));
		when(expenseRepository.sumAmountGroupedByCategoryInRange(1L, 2026, 6, 2026, 7)).thenReturn(List.of());
		when(expenseRepository.sumAmountGroupedByTransactionMethodInRange(1L, 2026, 6, 2026, 7)).thenReturn(List.of());
		when(expenseRepository.findTopExpensesInRange(any(), any(), any(), any(), any(), any())).thenReturn(List.of());

		FinancialAnalysisResponse response = financialAnalysisService.getAnalysis(1L, START, END);

		assertThat(response.incomeVsExpenses()).hasSize(2);
		assertThat(response.incomeVsExpenses().get(0).month()).isEqualTo(6);
		assertThat(response.incomeVsExpenses().get(0).incomeAmount()).isEqualByComparingTo("1000.00");
		assertThat(response.incomeVsExpenses().get(0).expenseAmount()).isEqualByComparingTo("0");
		assertThat(response.incomeVsExpenses().get(1).month()).isEqualTo(7);
		assertThat(response.incomeVsExpenses().get(1).incomeAmount()).isEqualByComparingTo("0");
		assertThat(response.incomeVsExpenses().get(1).expenseAmount()).isEqualByComparingTo("500.00");

		assertThat(response.monthlyBalanceEvolution().get(0).balance()).isEqualByComparingTo("1000.00");
		assertThat(response.monthlyBalanceEvolution().get(1).balance()).isEqualByComparingTo("-500.00");
	}

	@Test
	void getAnalysis_expensesByCategory_computesPercentageAgainstTotal() {
		Category food = new Category();
		food.setId(1L);

		when(expenseRepository.sumAmountGroupedByBillingPeriod(1L, 2026, 6, 2026, 7))
				.thenReturn(List.of(billingProjection(2026, 6, new BigDecimal("600.00"))));
		when(incomeRepository.sumAmountGroupedByMonth(1L, START, END)).thenReturn(List.of());
		when(expenseRepository.sumAmountGroupedByCategoryInRange(1L, 2026, 6, 2026, 7))
				.thenReturn(List.of(categoryProjection(1L, new BigDecimal("600.00"))));
		when(categoryRepository.findAllById(List.of(1L))).thenReturn(List.of(food));
		when(categoryMapper.toResponse(food)).thenReturn(new CategoryResponse(1L, "Food", "#FF0000", null, true));
		when(expenseRepository.sumAmountGroupedByTransactionMethodInRange(1L, 2026, 6, 2026, 7)).thenReturn(List.of());
		when(expenseRepository.findTopExpensesInRange(any(), any(), any(), any(), any(), any())).thenReturn(List.of());

		FinancialAnalysisResponse response = financialAnalysisService.getAnalysis(1L, START, END);

		assertThat(response.expensesByCategory()).hasSize(1);
		CategoryExpenseResponse categoryExpense = response.expensesByCategory().get(0);
		assertThat(categoryExpense.amount()).isEqualByComparingTo("600.00");
		assertThat(categoryExpense.percentage()).isEqualByComparingTo("100.00");
	}

	@Test
	void getAnalysis_expensesByPaymentMethod_sortedByAmountDesc_withPercentageAgainstTotal() {
		when(expenseRepository.sumAmountGroupedByBillingPeriod(1L, 2026, 6, 2026, 7)).thenReturn(List.of(
				billingProjection(2026, 6, new BigDecimal("300.00")),
				billingProjection(2026, 7, new BigDecimal("700.00"))));
		when(incomeRepository.sumAmountGroupedByMonth(1L, START, END)).thenReturn(List.of());
		when(expenseRepository.sumAmountGroupedByCategoryInRange(1L, 2026, 6, 2026, 7)).thenReturn(List.of());
		when(expenseRepository.sumAmountGroupedByTransactionMethodInRange(1L, 2026, 6, 2026, 7)).thenReturn(List.of(
				paymentMethodProjection(10L, "Nubank", TransactionMethodType.CARD, CardTransactionMode.CREDIT, new BigDecimal("300.00"), 3L),
				paymentMethodProjection(2L, "Pix", TransactionMethodType.PIX, null, new BigDecimal("700.00"), 7L)));
		when(expenseRepository.findTopExpensesInRange(any(), any(), any(), any(), any(), any())).thenReturn(List.of());

		FinancialAnalysisResponse response = financialAnalysisService.getAnalysis(1L, START, END);

		List<PaymentMethodAnalysisResponse> byMethod = response.expensesByPaymentMethod();
		assertThat(byMethod).hasSize(2);
		assertThat(byMethod.get(0).name()).isEqualTo("Pix");
		assertThat(byMethod.get(0).percentage()).isEqualByComparingTo("70.00");
		assertThat(byMethod.get(1).name()).isEqualTo("Nubank");
		assertThat(byMethod.get(1).cardMode()).isEqualTo(CardTransactionMode.CREDIT);
		assertThat(byMethod.get(1).percentage()).isEqualByComparingTo("30.00");
	}

	@Test
	void getAnalysis_largestExpenses_keepsRealExpenseDate_andExposesBillingPeriodSeparately() {
		Category category = new Category();
		category.setId(1L);
		TransactionMethod nubank = TransactionMethod.builder().id(10L).name("Nubank").type(TransactionMethodType.CARD).build();
		Expense expense = Expense.builder()
				.id(100L)
				.description("Aluguel")
				.category(category)
				.transactionMethod(nubank)
				.cardTransactionMode(CardTransactionMode.CREDIT)
				.expenseDate(LocalDate.of(2026, 6, 20))
				.billingMonth(7)
				.billingYear(2026)
				.amount(new BigDecimal("1800.00"))
				.build();

		when(expenseRepository.sumAmountGroupedByBillingPeriod(1L, 2026, 6, 2026, 7)).thenReturn(List.of());
		when(incomeRepository.sumAmountGroupedByMonth(1L, START, END)).thenReturn(List.of());
		when(expenseRepository.sumAmountGroupedByCategoryInRange(1L, 2026, 6, 2026, 7)).thenReturn(List.of());
		when(expenseRepository.sumAmountGroupedByTransactionMethodInRange(1L, 2026, 6, 2026, 7)).thenReturn(List.of());
		when(expenseRepository.findTopExpensesInRange(any(), any(), any(), any(), any(), any())).thenReturn(List.of(expense));
		when(categoryMapper.toResponse(category)).thenReturn(new CategoryResponse(1L, "Moradia", "#8B5CF6", null, true));

		FinancialAnalysisResponse response = financialAnalysisService.getAnalysis(1L, START, END);

		assertThat(response.largestExpenses()).hasSize(1);
		var largest = response.largestExpenses().get(0);
		assertThat(largest.date()).isEqualTo(LocalDate.of(2026, 6, 20));
		assertThat(largest.billingMonth()).isEqualTo(7);
		assertThat(largest.billingYear()).isEqualTo(2026);
		assertThat(largest.transactionMethod().name()).isEqualTo("Nubank");
		assertThat(largest.cardMode()).isEqualTo(CardTransactionMode.CREDIT);
	}

	@Test
	void getPaymentMethodsDetail_percentageUsesUnfilteredTotal_totalsCoverFilteredResultNotJustPage() {
		when(expenseRepository.sumAmountGroupedByTransactionMethodInRange(1L, 2026, 6, 2026, 7)).thenReturn(List.of(
				paymentMethodProjection(10L, "Nubank", TransactionMethodType.CARD, CardTransactionMode.CREDIT, new BigDecimal("9860.00"), 24L),
				paymentMethodProjection(11L, "Nubank", TransactionMethodType.CARD, CardTransactionMode.DEBIT, new BigDecimal("1200.00"), 5L),
				paymentMethodProjection(2L, "Pix", TransactionMethodType.PIX, null, new BigDecimal("4180.00"), 18L)));

		PaymentMethodAnalysisPageResponse page = financialAnalysisService.getPaymentMethodsDetail(
				1L, START, END, "nubank", null, "amount,desc", 0, 10);

		assertThat(page.content()).hasSize(2);
		assertThat(page.totalAmount()).isEqualByComparingTo("11060.00");
		assertThat(page.totalTransactionCount()).isEqualTo(29L);
		BigDecimal overallTotal = new BigDecimal("9860.00").add(new BigDecimal("1200.00")).add(new BigDecimal("4180.00"));
		BigDecimal expectedPercentage = new BigDecimal("9860.00")
				.divide(overallTotal, 4, java.math.RoundingMode.HALF_UP)
				.multiply(BigDecimal.valueOf(100))
				.setScale(2, java.math.RoundingMode.HALF_UP);
		assertThat(page.content().get(0).percentage()).isEqualByComparingTo(expectedPercentage);
	}

	@Test
	void getPaymentMethodsDetail_sortsByNameAscending() {
		when(expenseRepository.sumAmountGroupedByTransactionMethodInRange(1L, 2026, 6, 2026, 7)).thenReturn(List.of(
				paymentMethodProjection(2L, "Pix", TransactionMethodType.PIX, null, new BigDecimal("400.00"), 1L),
				paymentMethodProjection(10L, "Nubank", TransactionMethodType.CARD, CardTransactionMode.CREDIT, new BigDecimal("300.00"), 1L)));

		PaymentMethodAnalysisPageResponse page = financialAnalysisService.getPaymentMethodsDetail(
				1L, START, END, null, null, "name,asc", 0, 10);

		assertThat(page.content()).extracting(PaymentMethodAnalysisResponse::name).containsExactly("Nubank", "Pix");
	}

	@Test
	void getPaymentMethodsDetail_paginatesFilteredResults() {
		when(expenseRepository.sumAmountGroupedByTransactionMethodInRange(1L, 2026, 6, 2026, 7)).thenReturn(List.of(
				paymentMethodProjection(1L, "A", TransactionMethodType.PIX, null, new BigDecimal("100.00"), 1L),
				paymentMethodProjection(2L, "B", TransactionMethodType.PIX, null, new BigDecimal("200.00"), 1L),
				paymentMethodProjection(3L, "C", TransactionMethodType.PIX, null, new BigDecimal("300.00"), 1L)));

		PaymentMethodAnalysisPageResponse page = financialAnalysisService.getPaymentMethodsDetail(
				1L, START, END, null, null, "amount,desc", 1, 2);

		assertThat(page.content()).hasSize(1);
		assertThat(page.content().get(0).name()).isEqualTo("A");
		assertThat(page.totalElements()).isEqualTo(3);
		assertThat(page.totalPages()).isEqualTo(2);
		assertThat(page.totalAmount()).isEqualByComparingTo("600.00");
	}

	@Test
	void getPaymentMethodsDetail_throwsInvalidRequest_whenSortPropertyNotAllowed() {
		lenient().when(expenseRepository.sumAmountGroupedByTransactionMethodInRange(1L, 2026, 6, 2026, 7)).thenReturn(List.of());

		assertThatThrownBy(() -> financialAnalysisService.getPaymentMethodsDetail(1L, START, END, null, null, "unknown,desc", 0, 10))
				.isInstanceOf(InvalidRequestException.class);
	}

	private static BillingPeriodTotalProjection billingProjection(int year, int month, BigDecimal total) {
		return new BillingPeriodTotalProjection() {
			@Override
			public Integer getBillingYear() {
				return year;
			}

			@Override
			public Integer getBillingMonth() {
				return month;
			}

			@Override
			public BigDecimal getTotal() {
				return total;
			}
		};
	}

	private static MonthTotalProjection monthProjection(int year, int month, BigDecimal total) {
		return new MonthTotalProjection() {
			@Override
			public Integer getYear() {
				return year;
			}

			@Override
			public Integer getMonth() {
				return month;
			}

			@Override
			public BigDecimal getTotal() {
				return total;
			}
		};
	}

	private static CategoryTotalProjection categoryProjection(Long categoryId, BigDecimal total) {
		return new CategoryTotalProjection() {
			@Override
			public Long getCategoryId() {
				return categoryId;
			}

			@Override
			public BigDecimal getTotal() {
				return total;
			}
		};
	}

	private static PaymentMethodTotalProjection paymentMethodProjection(
			Long transactionMethodId, String name, TransactionMethodType type, CardTransactionMode cardMode, BigDecimal total, Long count) {
		return new PaymentMethodTotalProjection() {
			@Override
			public Long getTransactionMethodId() {
				return transactionMethodId;
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public TransactionMethodType getMethodType() {
				return type;
			}

			@Override
			public CardTransactionMode getCardTransactionMode() {
				return cardMode;
			}

			@Override
			public BigDecimal getTotal() {
				return total;
			}

			@Override
			public Long getTransactionCount() {
				return count;
			}
		};
	}

}
