package com.personal.backend_financeiro.controller;

import com.personal.backend_financeiro.dto.dashboard.DashboardResponse;
import com.personal.backend_financeiro.security.CurrentUserProvider;
import com.personal.backend_financeiro.service.DashboardService;
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

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = """
		Visão consolidada de dados já existentes no sistema (despesas, despesas fixas, limite \
		mensal e planejamento por categoria). Nenhum dado é armazenado por este endpoint: totais, \
		percentuais, médias e status financeiro são sempre calculados dinamicamente a partir das \
		entidades existentes.""")
public class DashboardController {

	private final DashboardService dashboardService;
	private final CurrentUserProvider currentUserProvider;

	@GetMapping
	@Operation(summary = "Retorna os dados consolidados da tela de Dashboard para um mês/ano",
			description = """
					Nunca retorna 404 por ausência de dados (sem despesas, sem limite, sem \
					recorrências, etc.) — nesses casos os campos vêm como 0, null ou lista vazia, \
					conforme o tipo. averageDailyExpense é 0 para meses futuros. financialStatus é \
					sempre calculado a partir do percentual do limite mensal, nunca persistido.""")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Dados consolidados do Dashboard"),
			@ApiResponse(responseCode = "400", description = "month/year inválidos"),
			@ApiResponse(responseCode = "401", description = "Não autenticado")
	})
	public DashboardResponse getDashboard(
			@Parameter(description = "Mês (1-12), obrigatório") @RequestParam Integer month,
			@Parameter(description = "Ano (>= 2000), obrigatório") @RequestParam Integer year) {
		return dashboardService.getDashboard(currentUserProvider.getCurrentUserId(), month, year);
	}

}
