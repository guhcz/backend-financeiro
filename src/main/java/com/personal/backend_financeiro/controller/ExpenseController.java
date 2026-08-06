package com.personal.backend_financeiro.controller;

import com.personal.backend_financeiro.dto.expense.ExpenseFilterRequest;
import com.personal.backend_financeiro.dto.expense.ExpenseRequest;
import com.personal.backend_financeiro.dto.expense.ExpenseResponse;
import com.personal.backend_financeiro.enums.RecurringUpdateScope;
import com.personal.backend_financeiro.security.CurrentUserProvider;
import com.personal.backend_financeiro.service.ExpenseService;
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
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
@Tag(name = "Expenses")
public class ExpenseController {

	private final ExpenseService expenseService;
	private final CurrentUserProvider currentUserProvider;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ExpenseResponse create(@Valid @RequestBody ExpenseRequest request) {
		return expenseService.create(currentUserProvider.getCurrentUserId(), request);
	}

	@GetMapping("/{id}")
	public ExpenseResponse getOne(@PathVariable Long id) {
		return expenseService.getOne(currentUserProvider.getCurrentUserId(), id);
	}

	@GetMapping
	public Page<ExpenseResponse> filter(@ModelAttribute ExpenseFilterRequest filter, Pageable pageable) {
		return expenseService.filter(currentUserProvider.getCurrentUserId(), filter, pageable);
	}

	@PutMapping("/{id}")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Despesa atualizada"),
			@ApiResponse(responseCode = "400", description = "Dados inválidos"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "404", description = "Despesa ou categoria não encontrada, ou não pertence ao usuário autenticado")
	})
	public ExpenseResponse update(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request,
			@Parameter(description = """
					ONLY_THIS (padrão) atualiza somente esta despesa. THIS_AND_FUTURE atualiza \
					esta despesa e também os dados da regra recorrente vinculada, para que as \
					próximas gerações automáticas já usem os novos valores. Em despesas manuais \
					(sem regra recorrente vinculada), THIS_AND_FUTURE é tratado como ONLY_THIS.""")
			@RequestParam(name = "scope", required = false, defaultValue = "ONLY_THIS") RecurringUpdateScope scope) {
		return expenseService.update(currentUserProvider.getCurrentUserId(), id, request, scope);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Despesa excluída"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "404", description = "Despesa não encontrada ou não pertence ao usuário autenticado")
	})
	public void delete(@PathVariable Long id,
			@Parameter(description = """
					ONLY_THIS (padrão) exclui somente esta despesa. THIS_AND_FUTURE exclui esta \
					despesa e encerra a regra recorrente vinculada (nenhuma despesa futura será \
					gerada), preservando as despesas passadas. Em despesas manuais, THIS_AND_FUTURE \
					é tratado como ONLY_THIS.""")
			@RequestParam(name = "scope", required = false, defaultValue = "ONLY_THIS") RecurringUpdateScope scope) {
		expenseService.delete(currentUserProvider.getCurrentUserId(), id, scope);
	}

}
