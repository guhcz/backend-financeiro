package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.category.CategoryResponse;
import com.personal.backend_financeiro.dto.dashboard.DashboardResponse;
import com.personal.backend_financeiro.dto.planning.CategoryExpenseResponse;
import com.personal.backend_financeiro.dto.planning.PlanningSummaryResponse;
import com.personal.backend_financeiro.entity.Category;
import com.personal.backend_financeiro.entity.Expense;
import com.personal.backend_financeiro.entity.RecurringExpense;
import com.personal.backend_financeiro.enums.FinancialStatusType;
import com.personal.backend_financeiro.enums.PaymentMethod;
import com.personal.backend_financeiro.enums.RecurrenceStatus;
import com.personal.backend_financeiro.exception.InvalidRequestException;
import com.personal.backend_financeiro.mapper.CategoryMapper;
import com.personal.backend_financeiro.repository.ExpenseRepository;
import com.personal.backend_financeiro.repository.IncomeRepository;
import com.personal.backend_financeiro.repository.RecurringExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

	private static final Long USER_ID = 1L;

	@Mock
	private PlanningService planningService;
	@Mock
	private ExpenseRepository expenseRepository;
	@Mock
	private IncomeRepository incomeRepository;
	@Mock
	private RecurringExpenseRepository recurringExpenseRepository;
	@Mock
	private CategoryMapper categoryMapper;

	@InjectMocks
	private DashboardService dashboardService;

	@BeforeEach
	void baseline() {
		lenient().when(expenseRepository.findTop5ByUserIdAndExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(
				anyLong(), any(), any())).thenReturn(List.of());
		lenient().when(expenseRepository.sumAmountByUserAndPeriod(anyLong(), any(), any())).thenReturn(BigDecimal.ZERO);
		lenient().when(incomeRepository.sumAmountByUserAndPeriod(anyLong(), any(), any())).thenReturn(BigDecimal.ZERO);
		lenient().when(recurringExpenseRepository.countByUserIdAndStatus(anyLong(), eq(RecurrenceStatus.ACTIVE))).thenReturn(0L);
		lenient().when(recurringExpenseRepository.countDueBetween(anyLong(), any(), any())).thenReturn(0L);
		lenient().when(recurringExpenseRepository.sumAmountDueOn(anyLong(), any())).thenReturn(BigDecimal.ZERO);
		lenient().when(planningService.expensesByCategory(anyLong(), anyInt(), anyInt())).thenReturn(List.of());
	}

	private static PlanningSummaryResponse summary(BigDecimal monthlyLimit, BigDecimal totalSpent,
			BigDecimal availableAmount, BigDecimal percentageUsed) {
		return new PlanningSummaryResponse(8, 2026, monthlyLimit, totalSpent, availableAmount, percentageUsed,
				BigDecimal.ZERO, monthlyLimit);
	}

	@Test
	void getDashboard_throwsInvalidRequestException_whenMonthOutOfRange() {
		assertThatThrownBy(() -> dashboardService.getDashboard(USER_ID, 13, 2026))
				.isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void getDashboard_throwsInvalidRequestException_whenYearBelowMinimum() {
		assertThatThrownBy(() -> dashboardService.getDashboard(USER_ID, 8, 1999))
				.isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void summary_computesTotalIncomeAndBalance_fromIncomeRepository() {
		when(planningService.summary(USER_ID, 8, 2026))
				.thenReturn(summary(new BigDecimal("5000.00"), new BigDecimal("1000.00"),
						new BigDecimal("4000.00"), new BigDecimal("20.00")));
		when(incomeRepository.sumAmountByUserAndPeriod(eq(USER_ID), any(), any()))
				.thenReturn(new BigDecimal("2500.00"));

		DashboardResponse result = dashboardService.getDashboard(USER_ID, 8, 2026);

		assertThat(result.summary().totalIncome()).isEqualByComparingTo("2500.00");
		assertThat(result.summary().balance()).isEqualByComparingTo("1500.00");
		assertThat(result.summary().totalExpenses()).isEqualByComparingTo("1000.00");
	}

	@Test
	void financialStatus_isNoLimit_whenPercentageIsNull() {
		when(planningService.summary(USER_ID, 8, 2026))
				.thenReturn(summary(null, new BigDecimal("500.00"), null, null));

		DashboardResponse result = dashboardService.getDashboard(USER_ID, 8, 2026);

		assertThat(result.financialStatus().type()).isEqualTo(FinancialStatusType.NO_LIMIT);
	}

	@Test
	void financialStatus_isWithinLimit_whenBelow80Percent() {
		when(planningService.summary(USER_ID, 8, 2026))
				.thenReturn(summary(new BigDecimal("1000.00"), new BigDecimal("500.00"),
						new BigDecimal("500.00"), new BigDecimal("50.00")));

		DashboardResponse result = dashboardService.getDashboard(USER_ID, 8, 2026);

		assertThat(result.financialStatus().type()).isEqualTo(FinancialStatusType.WITHIN_LIMIT);
	}

	@Test
	void financialStatus_isAttention_whenBetween80And99() {
		when(planningService.summary(USER_ID, 8, 2026))
				.thenReturn(summary(new BigDecimal("1000.00"), new BigDecimal("850.00"),
						new BigDecimal("150.00"), new BigDecimal("85.00")));

		DashboardResponse result = dashboardService.getDashboard(USER_ID, 8, 2026);

		assertThat(result.financialStatus().type()).isEqualTo(FinancialStatusType.ATTENTION);
	}

	@Test
	void financialStatus_isLimitReached_whenExactly100Percent() {
		when(planningService.summary(USER_ID, 8, 2026))
				.thenReturn(summary(new BigDecimal("1000.00"), new BigDecimal("1000.00"),
						BigDecimal.ZERO, new BigDecimal("100.00")));

		DashboardResponse result = dashboardService.getDashboard(USER_ID, 8, 2026);

		assertThat(result.financialStatus().type()).isEqualTo(FinancialStatusType.LIMIT_REACHED);
	}

	@Test
	void financialStatus_isOverLimit_whenAbove100Percent() {
		when(planningService.summary(USER_ID, 8, 2026))
				.thenReturn(summary(new BigDecimal("1000.00"), new BigDecimal("1100.00"),
						new BigDecimal("-100.00"), new BigDecimal("110.00")));

		DashboardResponse result = dashboardService.getDashboard(USER_ID, 8, 2026);

		assertThat(result.financialStatus().type()).isEqualTo(FinancialStatusType.OVER_LIMIT);
	}

	@Test
	void monthlyExpenseHistory_returnsSixMonthsEndingOnSelected_zeroFillingMonthsWithoutExpenses() {
		when(planningService.summary(USER_ID, 2, 2026)).thenReturn(summary(null, BigDecimal.ZERO, null, null));
		when(expenseRepository.sumAmountByUserAndPeriod(
				eq(USER_ID), eq(LocalDate.of(2026, 2, 1)), eq(LocalDate.of(2026, 2, 28))))
				.thenReturn(new BigDecimal("500.00"));

		DashboardResponse result = dashboardService.getDashboard(USER_ID, 2, 2026);

		assertThat(result.monthlyExpenseHistory()).hasSize(6);
		assertThat(result.monthlyExpenseHistory().get(0).month()).isEqualTo(9);
		assertThat(result.monthlyExpenseHistory().get(0).year()).isEqualTo(2025);
		assertThat(result.monthlyExpenseHistory().get(0).amount()).isEqualByComparingTo("0");
		assertThat(result.monthlyExpenseHistory().get(5).month()).isEqualTo(2);
		assertThat(result.monthlyExpenseHistory().get(5).year()).isEqualTo(2026);
		assertThat(result.monthlyExpenseHistory().get(5).amount()).isEqualByComparingTo("500.00");
	}

	@Test
	void recentExpenses_mapsFieldsAndRecurringFlag() {
		when(planningService.summary(USER_ID, 8, 2026)).thenReturn(summary(null, BigDecimal.ZERO, null, null));

		Category category = new Category();
		category.setId(3L);
		Expense manualExpense = Expense.builder()
				.id(10L)
				.description("Restaurante")
				.amount(new BigDecimal("42.90"))
				.expenseDate(LocalDate.of(2026, 8, 10))
				.paymentMethod(PaymentMethod.PIX)
				.category(category)
				.build();
		Expense recurringGeneratedExpense = Expense.builder()
				.id(11L)
				.description("Internet")
				.amount(new BigDecimal("119.90"))
				.expenseDate(LocalDate.of(2026, 8, 5))
				.paymentMethod(PaymentMethod.CREDIT_CARD)
				.category(category)
				.recurringExpense(new RecurringExpense())
				.build();
		when(expenseRepository.findTop5ByUserIdAndExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(
				eq(USER_ID), any(), any()))
				.thenReturn(List.of(recurringGeneratedExpense, manualExpense));
		when(categoryMapper.toResponse(category)).thenReturn(new CategoryResponse(3L, "Alimentação", "#EF4444", "utensils", true));

		DashboardResponse result = dashboardService.getDashboard(USER_ID, 8, 2026);

		assertThat(result.recentExpenses()).hasSize(2);
		assertThat(result.recentExpenses().get(0).recurring()).isTrue();
		assertThat(result.recentExpenses().get(1).recurring()).isFalse();
		assertThat(result.recentExpenses().get(1).description()).isEqualTo("Restaurante");
		assertThat(result.recentExpenses().get(1).category().name()).isEqualTo("Alimentação");
	}

	@Test
	void expensesByCategory_limitsToTop5FromPlanningService() {
		when(planningService.summary(USER_ID, 8, 2026)).thenReturn(summary(null, BigDecimal.ZERO, null, null));
		CategoryResponse categoryResponse = new CategoryResponse(1L, "Food", "#FF0000", null, true);
		List<CategoryExpenseResponse> sevenCategories = List.of(
				new CategoryExpenseResponse(categoryResponse, new BigDecimal("700"), new BigDecimal("20")),
				new CategoryExpenseResponse(categoryResponse, new BigDecimal("600"), new BigDecimal("18")),
				new CategoryExpenseResponse(categoryResponse, new BigDecimal("500"), new BigDecimal("15")),
				new CategoryExpenseResponse(categoryResponse, new BigDecimal("400"), new BigDecimal("12")),
				new CategoryExpenseResponse(categoryResponse, new BigDecimal("300"), new BigDecimal("9")),
				new CategoryExpenseResponse(categoryResponse, new BigDecimal("200"), new BigDecimal("6")),
				new CategoryExpenseResponse(categoryResponse, new BigDecimal("100"), new BigDecimal("3")));
		when(planningService.expensesByCategory(USER_ID, 8, 2026)).thenReturn(sevenCategories);

		DashboardResponse result = dashboardService.getDashboard(USER_ID, 8, 2026);

		assertThat(result.expensesByCategory()).hasSize(5);
		assertThat(result.expensesByCategory()).isEqualTo(sevenCategories.subList(0, 5));
	}

	@Test
	void recurringSummary_passesThroughRepositoryCounts() {
		when(planningService.summary(USER_ID, 8, 2026)).thenReturn(summary(null, BigDecimal.ZERO, null, null));
		when(recurringExpenseRepository.countByUserIdAndStatus(USER_ID, RecurrenceStatus.ACTIVE)).thenReturn(8L);
		when(recurringExpenseRepository.countDueBetween(eq(USER_ID), any(), any())).thenReturn(3L);
		when(recurringExpenseRepository.sumAmountDueOn(eq(USER_ID), any())).thenReturn(new BigDecimal("228.50"));

		DashboardResponse result = dashboardService.getDashboard(USER_ID, 8, 2026);

		assertThat(result.recurringExpensesSummary().activeCount()).isEqualTo(8L);
		assertThat(result.recurringExpensesSummary().dueInNext7DaysCount()).isEqualTo(3L);
		assertThat(result.recurringExpensesSummary().dueTodayAmount()).isEqualByComparingTo("228.50");
	}

	@Test
	void averageDailyExpense_isZero_forFutureMonth() {
		when(planningService.summary(USER_ID, 1, 2099)).thenReturn(summary(null, new BigDecimal("500.00"), null, null));

		DashboardResponse result = dashboardService.getDashboard(USER_ID, 1, 2099);

		assertThat(result.averageDailyExpense()).isEqualByComparingTo("0");
	}

	@Test
	void averageDailyExpense_dividesByTotalDaysInMonth_forPastMonth() {
		when(planningService.summary(USER_ID, 2, 2000)).thenReturn(summary(null, new BigDecimal("2900.00"), null, null));

		DashboardResponse result = dashboardService.getDashboard(USER_ID, 2, 2000);

		assertThat(result.averageDailyExpense()).isEqualByComparingTo("100.00");
	}

	@Test
	void averageDailyExpense_dividesByElapsedDays_forCurrentMonth() {
		YearMonth currentMonth = YearMonth.now();
		int elapsedDays = LocalDate.now().getDayOfMonth();
		BigDecimal totalExpenses = BigDecimal.valueOf(elapsedDays * 10L);
		when(planningService.summary(USER_ID, currentMonth.getMonthValue(), currentMonth.getYear()))
				.thenReturn(summary(null, totalExpenses, null, null));

		DashboardResponse result = dashboardService.getDashboard(USER_ID, currentMonth.getMonthValue(), currentMonth.getYear());

		assertThat(result.averageDailyExpense()).isEqualByComparingTo("10.00");
	}

}
