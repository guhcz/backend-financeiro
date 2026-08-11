package com.personal.backend_financeiro.controller;

import com.personal.backend_financeiro.dto.recurringincome.RecurringIncomeCreateRequest;
import com.personal.backend_financeiro.dto.recurringincome.RecurringIncomeFilterRequest;
import com.personal.backend_financeiro.dto.recurringincome.RecurringIncomeResponse;
import com.personal.backend_financeiro.dto.recurringincome.RecurringIncomeUpdateRequest;
import com.personal.backend_financeiro.security.CurrentUserProvider;
import com.personal.backend_financeiro.service.RecurringIncomeService;
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
@RequestMapping("/api/v1/recurring-incomes")
@RequiredArgsConstructor
@Tag(name = "Recurring Incomes", description = """
		Regras de receita recorrente (salário, aluguel recebido, renda variável mensal etc). \
		Cada regra gera automaticamente uma receita por mês, via job diário, respeitando o dia \
		de recebimento (receiptDay) informado — caso o mês não tenha esse dia, usa-se o último \
		dia válido. receiptDay é opcional: quando ausente, a geração ainda ocorre todo mês (no \
		dia 1, como data técnica de referência), mas nenhuma "data de recebimento prevista" é \
		exibida. A geração é idempotente: nunca é criada mais de uma receita para o mesmo \
		mês/ano de uma mesma regra.""")
public class RecurringIncomeController {

	private final RecurringIncomeService recurringIncomeService;
	private final CurrentUserProvider currentUserProvider;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Cria uma regra de receita recorrente", description = """
			Se startDate for igual ou anterior à data atual, a primeira receita (do mês de \
			startDate) é gerada imediatamente na resposta; se for futura, nenhuma receita é \
			gerada agora e o campo nextGenerationDate indica quando a primeira será criada. \
			Não há geração retroativa de múltiplos meses.""")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Regra criada"),
			@ApiResponse(responseCode = "400", description = "Dados inválidos (ex.: frequência diferente de MONTHLY, receiptDay fora de 1-31, endDate anterior a startDate)"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "404", description = "Categoria não encontrada ou não pertence ao usuário autenticado")
	})
	public RecurringIncomeResponse create(@Valid @RequestBody RecurringIncomeCreateRequest request) {
		return recurringIncomeService.create(currentUserProvider.getCurrentUserId(), request);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consulta uma regra de receita recorrente pelo id")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Regra encontrada"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "404", description = "Regra não encontrada ou não pertence ao usuário autenticado")
	})
	public RecurringIncomeResponse getOne(@PathVariable Long id) {
		return recurringIncomeService.getOne(currentUserProvider.getCurrentUserId(), id);
	}

	@GetMapping
	@Operation(summary = "Lista as regras de receita recorrente do usuário autenticado", description = """
			O filtro "active" aceita true (só regras ativas), false (pausadas ou encerradas) \
			ou ausência do parâmetro (todas). referenceMonth e referenceYear (opcionais, mas devem \
			ser informados juntos) filtram somente as regras vigentes naquele mês/ano, isto é, cujo \
			período (startDate–endDate) sobrepõe o mês informado.""")
	public Page<RecurringIncomeResponse> filter(@ModelAttribute RecurringIncomeFilterRequest filter, Pageable pageable) {
		return recurringIncomeService.filter(currentUserProvider.getCurrentUserId(), filter, pageable);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualiza uma regra de receita recorrente", description = """
			startDate e frequency são imutáveis após a criação e não fazem parte deste payload. \
			Alterar receiptDay numa regra ativa recalcula nextGenerationDate a partir de hoje, sem \
			gerar nenhuma receita de forma síncrona.""")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Regra atualizada"),
			@ApiResponse(responseCode = "400", description = "Dados inválidos, ou regra já encerrada (status ENDED) não pode ser editada"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "404", description = "Regra ou categoria não encontrada, ou não pertence ao usuário autenticado")
	})
	public RecurringIncomeResponse update(@PathVariable Long id, @Valid @RequestBody RecurringIncomeUpdateRequest request) {
		return recurringIncomeService.update(currentUserProvider.getCurrentUserId(), id, request);
	}

	@PatchMapping("/{id}/pause")
	@Operation(summary = "Pausa a geração das próximas receitas", description = """
			Idempotente: pausar uma regra já pausada não tem efeito. Receitas já geradas não \
			são afetadas.""")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Regra pausada (ou já estava pausada)"),
			@ApiResponse(responseCode = "400", description = "Regra já encerrada (status ENDED) não pode ser pausada"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "404", description = "Regra não encontrada ou não pertence ao usuário autenticado")
	})
	public RecurringIncomeResponse pause(@PathVariable Long id) {
		return recurringIncomeService.pause(currentUserProvider.getCurrentUserId(), id);
	}

	@PatchMapping("/{id}/resume")
	@Operation(summary = "Reativa a geração de uma regra pausada", description = """
			Recalcula nextGenerationDate a partir de hoje (nunca deixa a próxima geração no \
			passado). Não gera nenhuma receita de forma síncrona — quem gera é sempre o job \
			diário. Idempotente para regras já ativas.""")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Regra reativada (ou já estava ativa)"),
			@ApiResponse(responseCode = "400", description = "Regra já encerrada (status ENDED), ou com endDate já vencida"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "404", description = "Regra não encontrada ou não pertence ao usuário autenticado")
	})
	public RecurringIncomeResponse resume(@PathVariable Long id) {
		return recurringIncomeService.resume(currentUserProvider.getCurrentUserId(), id);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Exclui ou encerra uma regra de receita recorrente", description = """
			Se a regra nunca gerou nenhuma receita, é removida definitivamente. Se já gerou \
			histórico, é encerrada de forma irreversível (não pode ser reativada via resume) \
			e as receitas já geradas são preservadas.""")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Regra excluída ou encerrada"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "404", description = "Regra não encontrada ou não pertence ao usuário autenticado")
	})
	public void delete(@PathVariable Long id) {
		recurringIncomeService.delete(currentUserProvider.getCurrentUserId(), id);
	}

}
