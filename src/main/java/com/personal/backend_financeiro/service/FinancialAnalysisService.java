package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.financialanalysis.FinancialAnalysisResponse;
import com.personal.backend_financeiro.dto.financialanalysis.IncomeVsExpensesPointResponse;
import com.personal.backend_financeiro.dto.financialanalysis.LargestExpenseResponse;
import com.personal.backend_financeiro.dto.financialanalysis.MonthlyBalanceResponse;
import com.personal.backend_financeiro.dto.financialanalysis.PaymentMethodAnalysisPageResponse;
import com.personal.backend_financeiro.dto.financialanalysis.PaymentMethodAnalysisResponse;
import com.personal.backend_financeiro.dto.financialanalysis.PaymentMethodSummaryResponse;
import com.personal.backend_financeiro.dto.planning.CategoryExpenseResponse;
import com.personal.backend_financeiro.entity.Category;
import com.personal.backend_financeiro.entity.Expense;
import com.personal.backend_financeiro.entity.TransactionMethod;
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
import com.personal.backend_financeiro.util.DateRangeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Backs the "Análise financeira" screen: a read-only, purely analytical view derived from
 * existing Expense/Income rows -- no data is persisted here. All amounts follow financial
 * competence (see CompetenceResolver): expenses are bucketed by billingMonth/billingYear, incomes
 * by their own incomeDate's month. startDate/endDate are reduced to a YearMonth range (monthly
 * granularity, see spec section 5), so only the month of each boundary matters.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialAnalysisService {

	private static final int PERCENTAGE_SCALE = 2;
	private static final int LARGEST_EXPENSES_LIMIT = 5;
	private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("amount", "transactionCount", "name");

	private final ExpenseRepository expenseRepository;
	private final IncomeRepository incomeRepository;
	private final CategoryRepository categoryRepository;
	private final CategoryMapper categoryMapper;

	public FinancialAnalysisResponse getAnalysis(Long userId, LocalDate startDate, LocalDate endDate) {
		DateRangeUtils.assertValid(startDate, endDate);
		YearMonth fromMonth = YearMonth.from(startDate);
		YearMonth toMonth = YearMonth.from(endDate);

		Map<YearMonth, BigDecimal> expenseByMonth = expenseRepository
				.sumAmountGroupedByBillingPeriod(userId, fromMonth.getYear(), fromMonth.getMonthValue(), toMonth.getYear(), toMonth.getMonthValue())
				.stream()
				.collect(Collectors.toMap(
						p -> YearMonth.of(p.getBillingYear(), p.getBillingMonth()), BillingPeriodTotalProjection::getTotal));

		Map<YearMonth, BigDecimal> incomeByMonth = incomeRepository
				.sumAmountGroupedByMonth(userId, fromMonth.atDay(1), toMonth.atEndOfMonth())
				.stream()
				.collect(Collectors.toMap(p -> YearMonth.of(p.getYear(), p.getMonth()), MonthTotalProjection::getTotal));

		List<IncomeVsExpensesPointResponse> incomeVsExpenses = new ArrayList<>();
		List<MonthlyBalanceResponse> monthlyBalanceEvolution = new ArrayList<>();
		BigDecimal totalExpenseAmount = BigDecimal.ZERO;
		for (YearMonth ym = fromMonth; !ym.isAfter(toMonth); ym = ym.plusMonths(1)) {
			BigDecimal income = incomeByMonth.getOrDefault(ym, BigDecimal.ZERO);
			BigDecimal expense = expenseByMonth.getOrDefault(ym, BigDecimal.ZERO);
			totalExpenseAmount = totalExpenseAmount.add(expense);
			incomeVsExpenses.add(new IncomeVsExpensesPointResponse(ym.getMonthValue(), ym.getYear(), income, expense));
			monthlyBalanceEvolution.add(new MonthlyBalanceResponse(
					ym.getMonthValue(), ym.getYear(), income, expense, income.subtract(expense)));
		}

		BigDecimal totalExpenseAmountFinal = totalExpenseAmount;
		List<CategoryExpenseResponse> expensesByCategory = expensesByCategory(userId, fromMonth, toMonth, totalExpenseAmountFinal);

		List<PaymentMethodAnalysisResponse> expensesByPaymentMethod = paymentMethodTotals(userId, fromMonth, toMonth).stream()
				.map(p -> toPaymentMethodAnalysisResponse(p, totalExpenseAmountFinal))
				.sorted(Comparator.comparing(PaymentMethodAnalysisResponse::amount).reversed())
				.toList();

		List<LargestExpenseResponse> largestExpenses = expenseRepository.findTopExpensesInRange(
						userId, fromMonth.getYear(), fromMonth.getMonthValue(), toMonth.getYear(), toMonth.getMonthValue(),
						PageRequest.of(0, LARGEST_EXPENSES_LIMIT))
				.stream()
				.map(this::toLargestExpenseResponse)
				.toList();

		return new FinancialAnalysisResponse(
				startDate, endDate, incomeVsExpenses, expensesByCategory, expensesByPaymentMethod,
				monthlyBalanceEvolution, largestExpenses);
	}

	public PaymentMethodAnalysisPageResponse getPaymentMethodsDetail(
			Long userId, LocalDate startDate, LocalDate endDate,
			String search, TransactionMethodType type, String sort, int page, int size) {
		DateRangeUtils.assertValid(startDate, endDate);
		YearMonth fromMonth = YearMonth.from(startDate);
		YearMonth toMonth = YearMonth.from(endDate);

		List<PaymentMethodTotalProjection> allTotals = paymentMethodTotals(userId, fromMonth, toMonth);
		BigDecimal grandTotal = allTotals.stream().map(PaymentMethodTotalProjection::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

		String searchLower = search == null || search.isBlank() ? null : search.toLowerCase();
		List<PaymentMethodAnalysisResponse> filtered = allTotals.stream()
				.filter(p -> searchLower == null || p.getName().toLowerCase().contains(searchLower))
				.filter(p -> type == null || p.getMethodType() == type)
				.map(p -> toPaymentMethodAnalysisResponse(p, grandTotal))
				.sorted(buildComparator(sort))
				.toList();

		BigDecimal totalAmount = filtered.stream().map(PaymentMethodAnalysisResponse::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
		long totalTransactionCount = filtered.stream().mapToLong(PaymentMethodAnalysisResponse::transactionCount).sum();

		int totalElements = filtered.size();
		int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
		int fromIndex = Math.min(page * size, totalElements);
		int toIndex = Math.min(fromIndex + size, totalElements);
		List<PaymentMethodAnalysisResponse> content = filtered.subList(fromIndex, toIndex);

		return new PaymentMethodAnalysisPageResponse(content, page, size, totalElements, totalPages, totalAmount, totalTransactionCount);
	}

	private List<CategoryExpenseResponse> expensesByCategory(Long userId, YearMonth fromMonth, YearMonth toMonth, BigDecimal grandTotal) {
		List<CategoryTotalProjection> totals = expenseRepository.sumAmountGroupedByCategoryInRange(
				userId, fromMonth.getYear(), fromMonth.getMonthValue(), toMonth.getYear(), toMonth.getMonthValue());
		if (totals.isEmpty()) {
			return List.of();
		}

		Map<Long, Category> categoriesById = categoryRepository
				.findAllById(totals.stream().map(CategoryTotalProjection::getCategoryId).toList())
				.stream()
				.collect(Collectors.toMap(Category::getId, c -> c));

		return totals.stream()
				.sorted(Comparator.comparing(CategoryTotalProjection::getTotal).reversed())
				.map(projection -> new CategoryExpenseResponse(
						categoryMapper.toResponse(categoriesById.get(projection.getCategoryId())),
						projection.getTotal(),
						percentageOf(projection.getTotal(), grandTotal)))
				.toList();
	}

	private List<PaymentMethodTotalProjection> paymentMethodTotals(Long userId, YearMonth fromMonth, YearMonth toMonth) {
		return expenseRepository.sumAmountGroupedByTransactionMethodInRange(
				userId, fromMonth.getYear(), fromMonth.getMonthValue(), toMonth.getYear(), toMonth.getMonthValue());
	}

	private PaymentMethodAnalysisResponse toPaymentMethodAnalysisResponse(PaymentMethodTotalProjection projection, BigDecimal grandTotal) {
		return new PaymentMethodAnalysisResponse(
				projection.getTransactionMethodId(),
				projection.getName(),
				projection.getMethodType(),
				projection.getCardTransactionMode(),
				projection.getTotal(),
				percentageOf(projection.getTotal(), grandTotal),
				projection.getTransactionCount());
	}

	private LargestExpenseResponse toLargestExpenseResponse(Expense expense) {
		TransactionMethod transactionMethod = expense.getTransactionMethod();
		return new LargestExpenseResponse(
				expense.getId(),
				expense.getDescription(),
				categoryMapper.toResponse(expense.getCategory()),
				new PaymentMethodSummaryResponse(transactionMethod.getId(), transactionMethod.getName(), transactionMethod.getType()),
				expense.getCardTransactionMode(),
				expense.getExpenseDate(),
				expense.getBillingMonth(),
				expense.getBillingYear(),
				expense.getAmount());
	}

	private Comparator<PaymentMethodAnalysisResponse> buildComparator(String sort) {
		String property = "amount";
		boolean ascending = false;
		if (sort != null && !sort.isBlank()) {
			String[] parts = sort.split(",");
			property = parts[0].trim();
			ascending = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim());
		}
		if (!ALLOWED_SORT_PROPERTIES.contains(property)) {
			throw new InvalidRequestException(
					"Ordenação não suportada: " + property + ". Use 'amount', 'transactionCount' ou 'name'.");
		}

		Comparator<PaymentMethodAnalysisResponse> comparator = switch (property) {
			case "transactionCount" -> Comparator.comparing(PaymentMethodAnalysisResponse::transactionCount);
			case "name" -> Comparator.comparing(PaymentMethodAnalysisResponse::name, String.CASE_INSENSITIVE_ORDER);
			default -> Comparator.comparing(PaymentMethodAnalysisResponse::amount);
		};
		return ascending ? comparator : comparator.reversed();
	}

	private BigDecimal percentageOf(BigDecimal part, BigDecimal total) {
		if (total.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}
		return part.divide(total, 4, RoundingMode.HALF_UP)
				.multiply(BigDecimal.valueOf(100))
				.setScale(PERCENTAGE_SCALE, RoundingMode.HALF_UP);
	}

}
