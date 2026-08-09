package com.personal.backend_financeiro.controller;

import com.personal.backend_financeiro.dto.planning.CategoryExpenseResponse;
import com.personal.backend_financeiro.dto.planning.ExpenseEvolutionPointResponse;
import com.personal.backend_financeiro.dto.planning.PlanningDashboardResponse;
import com.personal.backend_financeiro.dto.planning.PlanningSummaryResponse;
import com.personal.backend_financeiro.security.CurrentUserProvider;
import com.personal.backend_financeiro.service.PlanningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/planning")
@RequiredArgsConstructor
@Tag(name = "Planning", description = """
		Endpoints consolidados da tela de Planejamento. Planejamento representa uma meta de \
		gasto por categoria e não gera lançamentos financeiros automaticamente; os valores de \
		gasto real são sempre derivados das despesas cadastradas.""")
public class PlanningController {

	private final PlanningService planningService;
	private final CurrentUserProvider currentUserProvider;

	@GetMapping("/summary")
	@Operation(summary = "Resumo do mês: limite mensal, total gasto, disponível, percentual usado, total planejado e não planejado",
			description = """
					Quando não existir limite mensal cadastrado, monthlyLimit, availableAmount e \
					percentageUsed retornam null (não é considerado erro); totalSpent e totalPlanned \
					continuam sendo retornados normalmente.""")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Resumo do período"),
			@ApiResponse(responseCode = "400", description = "month/year inválidos"),
			@ApiResponse(responseCode = "401", description = "Não autenticado")
	})
	public PlanningSummaryResponse summary(
			@Parameter(description = "Mês (1-12), obrigatório") @RequestParam Integer month,
			@Parameter(description = "Ano (>= 2000), obrigatório") @RequestParam Integer year) {
		return planningService.summary(currentUserProvider.getCurrentUserId(), month, year);
	}

	@GetMapping("/expenses-by-category")
	@Operation(summary = "Gasto real por categoria no período, para o gráfico de gastos por categoria",
			description = "Retorna lista vazia quando não há despesas no período; nunca retorna erro nesse caso.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Lista de gastos por categoria"),
			@ApiResponse(responseCode = "400", description = "month/year inválidos"),
			@ApiResponse(responseCode = "401", description = "Não autenticado")
	})
	public List<CategoryExpenseResponse> expensesByCategory(
			@Parameter(description = "Mês (1-12), obrigatório") @RequestParam Integer month,
			@Parameter(description = "Ano (>= 2000), obrigatório") @RequestParam Integer year) {
		return planningService.expensesByCategory(currentUserProvider.getCurrentUserId(), month, year);
	}

	@GetMapping("/expense-evolution")
	@Operation(summary = "Evolução diária e acumulada dos gastos no mês, para o gráfico de evolução",
			description = "Retorna somente as datas que tiveram despesas, ordenadas cronologicamente.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Pontos de evolução do gasto"),
			@ApiResponse(responseCode = "400", description = "month/year inválidos"),
			@ApiResponse(responseCode = "401", description = "Não autenticado")
	})
	public List<ExpenseEvolutionPointResponse> expenseEvolution(
			@Parameter(description = "Mês (1-12), obrigatório") @RequestParam Integer month,
			@Parameter(description = "Ano (>= 2000), obrigatório") @RequestParam Integer year) {
		return planningService.expenseEvolution(currentUserProvider.getCurrentUserId(), month, year);
	}

	@GetMapping("/dashboard")
	@Operation(summary = "Endpoint agregado com resumo, gastos por categoria e evolução diária",
			description = "Não inclui a tabela paginada de planejamento por categoria; use GET /api/v1/monthly-plannings para isso.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Dados consolidados da tela de Planejamento"),
			@ApiResponse(responseCode = "400", description = "month/year inválidos"),
			@ApiResponse(responseCode = "401", description = "Não autenticado")
	})
	public PlanningDashboardResponse dashboard(
			@Parameter(description = "Mês (1-12), obrigatório") @RequestParam Integer month,
			@Parameter(description = "Ano (>= 2000), obrigatório") @RequestParam Integer year) {
		return planningService.dashboard(currentUserProvider.getCurrentUserId(), month, year);
	}

}
