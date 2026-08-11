package com.personal.backend_financeiro.controller;

import com.personal.backend_financeiro.dto.income.IncomeFilterRequest;
import com.personal.backend_financeiro.dto.income.IncomeRequest;
import com.personal.backend_financeiro.dto.income.IncomeResponse;
import com.personal.backend_financeiro.enums.RecurringUpdateScope;
import com.personal.backend_financeiro.security.CurrentUserProvider;
import com.personal.backend_financeiro.service.IncomeService;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/incomes")
@RequiredArgsConstructor
@Tag(name = "Incomes", description = """
		Receitas do usuário autenticado. Income é um recurso independente de Expense — as duas \
		entidades nunca se misturam no backend; a visão unificada de movimentações fica em \
		GET /api/v1/transactions, que é somente leitura.""")
public class IncomeController {

	private final IncomeService incomeService;
	private final CurrentUserProvider currentUserProvider;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public IncomeResponse create(@Valid @RequestBody IncomeRequest request) {
		return incomeService.create(currentUserProvider.getCurrentUserId(), request);
	}

	@GetMapping("/{id}")
	public IncomeResponse getOne(@PathVariable Long id) {
		return incomeService.getOne(currentUserProvider.getCurrentUserId(), id);
	}

	@GetMapping
	public Page<IncomeResponse> filter(@ModelAttribute IncomeFilterRequest filter, Pageable pageable) {
		return incomeService.filter(currentUserProvider.getCurrentUserId(), filter, pageable);
	}

	@PutMapping("/{id}")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Receita atualizada"),
			@ApiResponse(responseCode = "400", description = "Dados inválidos"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "404", description = "Receita ou categoria não encontrada, ou não pertence ao usuário autenticado")
	})
	public IncomeResponse update(@PathVariable Long id, @Valid @RequestBody IncomeRequest request,
			@Parameter(description = """
					ONLY_THIS (padrão) atualiza somente esta receita. THIS_AND_FUTURE atualiza \
					esta receita e também os dados da regra recorrente vinculada, para que as \
					próximas gerações automáticas já usem os novos valores. Em receitas manuais \
					(sem regra recorrente vinculada), THIS_AND_FUTURE é tratado como ONLY_THIS.""")
			@RequestParam(name = "scope", required = false, defaultValue = "ONLY_THIS") RecurringUpdateScope scope) {
		return incomeService.update(currentUserProvider.getCurrentUserId(), id, request, scope);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Receita excluída"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "404", description = "Receita não encontrada ou não pertence ao usuário autenticado")
	})
	public void delete(@PathVariable Long id,
			@Parameter(description = """
					ONLY_THIS (padrão) exclui somente esta receita. THIS_AND_FUTURE exclui esta \
					receita e encerra a regra recorrente vinculada (nenhuma receita futura será \
					gerada), preservando as receitas passadas. Em receitas manuais, THIS_AND_FUTURE \
					é tratado como ONLY_THIS.""")
			@RequestParam(name = "scope", required = false, defaultValue = "ONLY_THIS") RecurringUpdateScope scope) {
		incomeService.delete(currentUserProvider.getCurrentUserId(), id, scope);
	}

}
