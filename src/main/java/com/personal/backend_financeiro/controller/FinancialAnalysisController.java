package com.personal.backend_financeiro.controller;

import com.personal.backend_financeiro.dto.financialanalysis.FinancialAnalysisResponse;
import com.personal.backend_financeiro.dto.financialanalysis.PaymentMethodAnalysisPageResponse;
import com.personal.backend_financeiro.enums.TransactionMethodType;
import com.personal.backend_financeiro.security.CurrentUserProvider;
import com.personal.backend_financeiro.service.FinancialAnalysisService;
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

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/financial-analysis")
@RequiredArgsConstructor
@Tag(name = "Financial Analysis", description = """
		Visão analítica da tela "Análise financeira". Nenhum dado é armazenado por estes \
		endpoints: todos os totais, percentuais e listas são calculados dinamicamente a partir \
		das despesas e receitas existentes, sempre respeitando a competência financeira (mês da \
		fatura para cartão de crédito, data do lançamento para os demais casos e para receitas). \
		startDate/endDate são reduzidos a um intervalo de meses (granularidade mensal): apenas o \
		mês de cada data importa, o dia é ignorado.""")
public class FinancialAnalysisController {

	private final FinancialAnalysisService financialAnalysisService;
	private final CurrentUserProvider currentUserProvider;

	@GetMapping
	@Operation(summary = "Dados consolidados da tela de Análise financeira para um período",
			description = """
					Retorna receitas x despesas mensais, gastos por categoria, gastos por forma de \
					pagamento (com modalidade de cartão), evolução do saldo mensal e as 5 maiores \
					despesas do período. Nunca retorna 404 por ausência de dados: listas vazias ou \
					meses zerados nesses casos.""")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Dados consolidados da Análise financeira"),
			@ApiResponse(responseCode = "400", description = "startDate/endDate ausentes ou startDate posterior a endDate"),
			@ApiResponse(responseCode = "401", description = "Não autenticado")
	})
	public FinancialAnalysisResponse getAnalysis(
			@Parameter(description = "Início do período (inclusive), obrigatório") @RequestParam LocalDate startDate,
			@Parameter(description = "Fim do período (inclusive), obrigatório") @RequestParam LocalDate endDate) {
		return financialAnalysisService.getAnalysis(currentUserProvider.getCurrentUserId(), startDate, endDate);
	}

	@GetMapping("/payment-methods")
	@Operation(summary = "Detalhamento paginado de gastos por forma de pagamento, para o modal de detalhes",
			description = """
					A porcentagem de cada item é sempre calculada em relação ao total de despesas do \
					período completo, e não apenas à página atual ou ao resultado filtrado por \
					search/type. totalAmount e totalTransactionCount somam todos os itens que casam \
					com search/type, não apenas os da página atual.""")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Página de gastos por forma de pagamento"),
			@ApiResponse(responseCode = "400", description = "startDate/endDate ausentes, startDate posterior a endDate, ou sort inválido"),
			@ApiResponse(responseCode = "401", description = "Não autenticado")
	})
	public PaymentMethodAnalysisPageResponse getPaymentMethods(
			@Parameter(description = "Início do período (inclusive), obrigatório") @RequestParam LocalDate startDate,
			@Parameter(description = "Fim do período (inclusive), obrigatório") @RequestParam LocalDate endDate,
			@Parameter(description = "Busca pelo nome da forma de pagamento") @RequestParam(required = false) String search,
			@Parameter(description = "Filtra por tipo de forma de pagamento") @RequestParam(required = false) TransactionMethodType type,
			@Parameter(description = "amount,desc | amount,asc | transactionCount,desc | transactionCount,asc | name,asc | name,desc")
			@RequestParam(defaultValue = "amount,desc") String sort,
			@Parameter(description = "Número da página (0-based)") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Tamanho da página") @RequestParam(defaultValue = "10") int size) {
		return financialAnalysisService.getPaymentMethodsDetail(
				currentUserProvider.getCurrentUserId(), startDate, endDate, search, type, sort, page, size);
	}

}
