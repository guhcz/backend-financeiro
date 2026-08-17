package com.personal.backend_financeiro.controller;

import com.personal.backend_financeiro.dto.recurringexpense.RecurringExpenseCreateRequest;
import com.personal.backend_financeiro.dto.recurringexpense.RecurringExpenseFilterRequest;
import com.personal.backend_financeiro.dto.recurringexpense.RecurringExpenseResponse;
import com.personal.backend_financeiro.dto.recurringexpense.RecurringExpenseUpdateRequest;
import com.personal.backend_financeiro.security.CurrentUserProvider;
import com.personal.backend_financeiro.service.RecurringExpenseService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recurring-expenses")
@RequiredArgsConstructor
@Tag(name = "Recurring Expenses", description = """
		Regras de despesas recorrentes (aluguel, internet, assinaturas etc). Cada regra gera \
		automaticamente uma despesa por mês, com antecedência de até \
		app.recurring-expense.lookahead-months meses (padrão 12) — a criação já gera todas as \
		ocorrências dentro desse horizonte, e um job diário mantém o horizonte rolando à medida \
		que os meses passam. Editar ou encerrar a regra atualiza/remove as ocorrências futuras já \
		geradas; as passadas nunca são tocadas. A geração respeita o dia de vencimento (dueDay) \
		informado — caso o mês não tenha esse dia, usa-se o último dia válido (ex.: dueDay 31 em \
		fevereiro cai em 28 ou 29, conforme o ano). É idempotente: nunca é criada mais de uma \
		despesa para o mesmo mês/ano de uma mesma regra.""")
public class RecurringExpenseController {

	private final RecurringExpenseService recurringExpenseService;
	private final CurrentUserProvider currentUserProvider;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Cria uma regra de despesa recorrente", description = """
			Gera imediatamente, de forma síncrona, todas as ocorrências da regra desde a primeira \
			(a partir de startDate) até o horizonte de antecedência configurado \
			(app.recurring-expense.lookahead-months, padrão 12 meses) — por isso a despesa já \
			aparece em Movimentações dos próximos meses assim que a regra é criada, sem esperar a \
			data de vencimento chegar. O campo nextGenerationDate indica a partir de qual data o \
			job diário retoma a geração para manter esse horizonte rolando.""")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Regra criada"),
			@ApiResponse(responseCode = "400", description = "Dados inválidos (ex.: frequência diferente de MONTHLY, dueDay fora de 1-31, endDate anterior a startDate)"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "404", description = "Categoria não encontrada ou não pertence ao usuário autenticado")
	})
	public RecurringExpenseResponse create(@Valid @RequestBody RecurringExpenseCreateRequest request) {
		return recurringExpenseService.create(currentUserProvider.getCurrentUserId(), request);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consulta uma regra de despesa recorrente pelo id")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Regra encontrada"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "404", description = "Regra não encontrada ou não pertence ao usuário autenticado")
	})
	public RecurringExpenseResponse getOne(@PathVariable Long id) {
		return recurringExpenseService.getOne(currentUserProvider.getCurrentUserId(), id);
	}

	@GetMapping
	@Operation(summary = "Lista as regras de despesa recorrente do usuário autenticado", description = """
			O filtro "active" aceita true (só regras ativas), false (pausadas ou encerradas) \
			ou ausência do parâmetro (todas). referenceMonth e referenceYear (opcionais, mas devem \
			ser informados juntos) filtram somente as regras vigentes naquele mês/ano, isto é, cujo \
			período (startDate–endDate) sobrepõe o mês informado.""")
	public Page<RecurringExpenseResponse> filter(@ModelAttribute RecurringExpenseFilterRequest filter, Pageable pageable) {
		return recurringExpenseService.filter(currentUserProvider.getCurrentUserId(), filter, pageable);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualiza uma regra de despesa recorrente", description = """
			startDate e frequency são imutáveis após a criação e não fazem parte deste payload. \
			Alterar dueDay numa regra ativa recalcula nextGenerationDate a partir de hoje, sem \
			gerar nenhuma despesa de forma síncrona.""")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Regra atualizada"),
			@ApiResponse(responseCode = "400", description = "Dados inválidos, ou regra já encerrada (status ENDED) não pode ser editada"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "404", description = "Regra ou categoria não encontrada, ou não pertence ao usuário autenticado")
	})
	public RecurringExpenseResponse update(@PathVariable Long id, @Valid @RequestBody RecurringExpenseUpdateRequest request) {
		return recurringExpenseService.update(currentUserProvider.getCurrentUserId(), id, request);
	}

	@PatchMapping("/{id}/pause")
	@Operation(summary = "Pausa a geração das próximas despesas", description = """
			Idempotente: pausar uma regra já pausada não tem efeito. Despesas já geradas não \
			são afetadas.""")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Regra pausada (ou já estava pausada)"),
			@ApiResponse(responseCode = "400", description = "Regra já encerrada (status ENDED) não pode ser pausada"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "404", description = "Regra não encontrada ou não pertence ao usuário autenticado")
	})
	public RecurringExpenseResponse pause(@PathVariable Long id) {
		return recurringExpenseService.pause(currentUserProvider.getCurrentUserId(), id);
	}

	@PatchMapping("/{id}/resume")
	@Operation(summary = "Reativa a geração de uma regra pausada", description = """
			Recalcula nextGenerationDate a partir de hoje (nunca deixa a próxima geração no \
			passado). Não gera nenhuma despesa de forma síncrona — quem gera é sempre o job \
			diário. Idempotente para regras já ativas.""")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Regra reativada (ou já estava ativa)"),
			@ApiResponse(responseCode = "400", description = "Regra já encerrada (status ENDED), ou com endDate já vencida"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "404", description = "Regra não encontrada ou não pertence ao usuário autenticado")
	})
	public RecurringExpenseResponse resume(@PathVariable Long id) {
		return recurringExpenseService.resume(currentUserProvider.getCurrentUserId(), id);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Exclui ou encerra uma regra de despesa recorrente", description = """
			Se a regra nunca gerou nenhuma despesa, é removida definitivamente. Se já gerou \
			histórico, é encerrada de forma irreversível (não pode ser reativada via resume): \
			as despesas passadas/atuais são preservadas, mas as ocorrências futuras já \
			pré-geradas (dentro do horizonte de antecedência) são removidas, já que não vão mais \
			acontecer.""")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Regra excluída ou encerrada"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "404", description = "Regra não encontrada ou não pertence ao usuário autenticado")
	})
	public void delete(@PathVariable Long id) {
		recurringExpenseService.delete(currentUserProvider.getCurrentUserId(), id);
	}

}
