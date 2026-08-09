package com.personal.backend_financeiro.controller;

import com.personal.backend_financeiro.dto.monthlyplanning.MonthlyPlanningItemResponse;
import com.personal.backend_financeiro.dto.monthlyplanning.MonthlyPlanningRequest;
import com.personal.backend_financeiro.dto.monthlyplanning.MonthlyPlanningResponse;
import com.personal.backend_financeiro.security.CurrentUserProvider;
import com.personal.backend_financeiro.service.MonthlyPlanningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/monthly-plannings")
@RequiredArgsConstructor
@Tag(name = "Monthly Plannings", description = """
		Planejamento representa uma meta de gasto por categoria e não gera lançamentos \
		financeiros automaticamente. Os gastos reais continuam vindo exclusivamente da API de despesas.""")
public class MonthlyPlanningController {

	private final MonthlyPlanningService monthlyPlanningService;
	private final CurrentUserProvider currentUserProvider;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Cria um planejamento mensal por categoria",
			description = "Define uma meta de gasto para uma categoria em um mês/ano. Não cria despesas.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Planejamento criado"),
			@ApiResponse(responseCode = "400", description = "Dados inválidos"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "404", description = "Categoria não encontrada ou não pertence ao usuário autenticado"),
			@ApiResponse(responseCode = "409", description = "Já existe um planejamento para esta categoria no período informado")
	})
	public MonthlyPlanningResponse create(@Valid @RequestBody MonthlyPlanningRequest request) {
		return monthlyPlanningService.create(currentUserProvider.getCurrentUserId(), request);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca um planejamento pelo id")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Planejamento encontrado"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "404", description = "Planejamento não encontrado ou não pertence ao usuário autenticado")
	})
	public MonthlyPlanningResponse getOne(@PathVariable Long id) {
		return monthlyPlanningService.getOne(currentUserProvider.getCurrentUserId(), id);
	}

	@GetMapping
	@Operation(summary = "Lista paginada dos planejamentos de um mês/ano, com gasto real, restante e percentual consumido por categoria")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Página de planejamentos"),
			@ApiResponse(responseCode = "400", description = "month/year inválidos"),
			@ApiResponse(responseCode = "401", description = "Não autenticado")
	})
	public Page<MonthlyPlanningItemResponse> list(
			@Parameter(description = "Mês (1-12), obrigatório") @RequestParam Integer month,
			@Parameter(description = "Ano (>= 2000), obrigatório") @RequestParam Integer year,
			@Parameter(description = "Paginação padrão do Spring: page, size, sort (ex.: sort=category.name,asc)") Pageable pageable) {
		return monthlyPlanningService.list(currentUserProvider.getCurrentUserId(), month, year, pageable);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualiza um planejamento mensal por categoria")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Planejamento atualizado"),
			@ApiResponse(responseCode = "400", description = "Dados inválidos"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "404", description = "Planejamento ou categoria não encontrada, ou não pertence ao usuário autenticado"),
			@ApiResponse(responseCode = "409", description = "Já existe um planejamento para esta categoria no período informado")
	})
	public MonthlyPlanningResponse update(@PathVariable Long id, @Valid @RequestBody MonthlyPlanningRequest request) {
		return monthlyPlanningService.update(currentUserProvider.getCurrentUserId(), id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Exclui um planejamento mensal por categoria (não afeta despesas já registradas)")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Planejamento excluído"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "404", description = "Planejamento não encontrado ou não pertence ao usuário autenticado")
	})
	public void delete(@PathVariable Long id) {
		monthlyPlanningService.delete(currentUserProvider.getCurrentUserId(), id);
	}

}
