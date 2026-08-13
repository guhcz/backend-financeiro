package com.personal.backend_financeiro.controller;

import com.personal.backend_financeiro.dto.monthlycardplanning.MonthlyCardPlanningItemResponse;
import com.personal.backend_financeiro.dto.monthlycardplanning.MonthlyCardPlanningRequest;
import com.personal.backend_financeiro.dto.monthlycardplanning.MonthlyCardPlanningResponse;
import com.personal.backend_financeiro.security.CurrentUserProvider;
import com.personal.backend_financeiro.service.MonthlyCardPlanningService;
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
@RequestMapping("/api/v1/monthly-card-plannings")
@RequiredArgsConstructor
@Tag(name = "Monthly Card Plannings", description = """
		Planejamento representa uma meta de gasto por cartão de crédito e não gera lançamentos \
		financeiros automaticamente. Os gastos reais continuam vindo exclusivamente da API de despesas, \
		considerando a competência (mês da fatura) de cada despesa em cartão.""")
public class MonthlyCardPlanningController {

	private final MonthlyCardPlanningService monthlyCardPlanningService;
	private final CurrentUserProvider currentUserProvider;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Cria um planejamento mensal por cartão de crédito",
			description = "Define uma meta de gasto para um cartão em um mês/ano. Não cria despesas.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Planejamento criado"),
			@ApiResponse(responseCode = "400", description = "Dados inválidos"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "404", description = "Cartão não encontrado ou não pertence ao usuário autenticado"),
			@ApiResponse(responseCode = "409", description = "Já existe um planejamento para este cartão no período informado")
	})
	public MonthlyCardPlanningResponse create(@Valid @RequestBody MonthlyCardPlanningRequest request) {
		return monthlyCardPlanningService.create(currentUserProvider.getCurrentUserId(), request);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca um planejamento de cartão pelo id")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Planejamento encontrado"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "404", description = "Planejamento não encontrado ou não pertence ao usuário autenticado")
	})
	public MonthlyCardPlanningResponse getOne(@PathVariable Long id) {
		return monthlyCardPlanningService.getOne(currentUserProvider.getCurrentUserId(), id);
	}

	@GetMapping
	@Operation(summary = "Lista paginada dos planejamentos de cartão de um mês/ano, com valor utilizado, restante e percentual consumido")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Página de planejamentos"),
			@ApiResponse(responseCode = "400", description = "month/year inválidos"),
			@ApiResponse(responseCode = "401", description = "Não autenticado")
	})
	public Page<MonthlyCardPlanningItemResponse> list(
			@Parameter(description = "Mês (1-12), obrigatório") @RequestParam Integer month,
			@Parameter(description = "Ano (>= 2000), obrigatório") @RequestParam Integer year,
			@Parameter(description = "Paginação padrão do Spring: page, size, sort (ex.: sort=creditCard.name,asc)") Pageable pageable) {
		return monthlyCardPlanningService.list(currentUserProvider.getCurrentUserId(), month, year, pageable);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualiza um planejamento mensal por cartão de crédito")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Planejamento atualizado"),
			@ApiResponse(responseCode = "400", description = "Dados inválidos"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "404", description = "Planejamento ou cartão não encontrado, ou não pertence ao usuário autenticado"),
			@ApiResponse(responseCode = "409", description = "Já existe um planejamento para este cartão no período informado")
	})
	public MonthlyCardPlanningResponse update(@PathVariable Long id, @Valid @RequestBody MonthlyCardPlanningRequest request) {
		return monthlyCardPlanningService.update(currentUserProvider.getCurrentUserId(), id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Exclui um planejamento mensal por cartão (não afeta despesas já registradas)")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Planejamento excluído"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "404", description = "Planejamento não encontrado ou não pertence ao usuário autenticado")
	})
	public void delete(@PathVariable Long id) {
		monthlyCardPlanningService.delete(currentUserProvider.getCurrentUserId(), id);
	}

}
