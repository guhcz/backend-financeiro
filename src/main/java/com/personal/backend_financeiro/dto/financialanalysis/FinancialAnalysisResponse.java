package com.personal.backend_financeiro.dto.financialanalysis;

import com.personal.backend_financeiro.dto.planning.CategoryExpenseResponse;

import java.time.LocalDate;
import java.util.List;

public record FinancialAnalysisResponse(

		LocalDate startDate,
		LocalDate endDate,
		List<IncomeVsExpensesPointResponse> incomeVsExpenses,
		List<CategoryExpenseResponse> expensesByCategory,
		List<PaymentMethodAnalysisResponse> expensesByPaymentMethod,
		List<MonthlyBalanceResponse> monthlyBalanceEvolution,
		List<LargestExpenseResponse> largestExpenses

) {
}
