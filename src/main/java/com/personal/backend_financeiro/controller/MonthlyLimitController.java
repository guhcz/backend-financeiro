package com.personal.backend_financeiro.controller;

import com.personal.backend_financeiro.dto.monthlylimit.MonthlyLimitRequest;
import com.personal.backend_financeiro.dto.monthlylimit.MonthlyLimitResponse;
import com.personal.backend_financeiro.security.CurrentUserProvider;
import com.personal.backend_financeiro.service.MonthlyLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/monthly-limits")
@RequiredArgsConstructor
@Tag(name = "Monthly Limits")
public class MonthlyLimitController {

	private final MonthlyLimitService monthlyLimitService;
	private final CurrentUserProvider currentUserProvider;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public MonthlyLimitResponse create(@Valid @RequestBody MonthlyLimitRequest request) {
		return monthlyLimitService.create(currentUserProvider.getCurrentUserId(), request);
	}

	@GetMapping
	public List<MonthlyLimitResponse> list() {
		return monthlyLimitService.listByUser(currentUserProvider.getCurrentUserId());
	}

	@GetMapping("/by-period")
	@Operation(summary = "Busca o limite mensal de um período específico",
			description = "Retorna 204 sem corpo quando não existir limite cadastrado para o período, evitando que o cliente precise carregar todos os limites e filtrar.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Limite mensal do período"),
			@ApiResponse(responseCode = "204", description = "Não existe limite mensal cadastrado para o período"),
			@ApiResponse(responseCode = "401", description = "Não autenticado")
	})
	public ResponseEntity<MonthlyLimitResponse> getByPeriod(
			@Parameter(description = "Mês (1-12), obrigatório") @RequestParam Integer month,
			@Parameter(description = "Ano (>= 2000), obrigatório") @RequestParam Integer year) {
		return monthlyLimitService.findByPeriod(currentUserProvider.getCurrentUserId(), month, year)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@PutMapping("/{id}")
	public MonthlyLimitResponse update(@PathVariable Long id, @Valid @RequestBody MonthlyLimitRequest request) {
		return monthlyLimitService.update(currentUserProvider.getCurrentUserId(), id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		monthlyLimitService.delete(currentUserProvider.getCurrentUserId(), id);
	}

}
