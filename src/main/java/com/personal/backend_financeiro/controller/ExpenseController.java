package com.personal.backend_financeiro.controller;

import com.personal.backend_financeiro.dto.expense.ExpenseFilterRequest;
import com.personal.backend_financeiro.dto.expense.ExpenseRequest;
import com.personal.backend_financeiro.dto.expense.ExpenseResponse;
import com.personal.backend_financeiro.security.CurrentUserProvider;
import com.personal.backend_financeiro.service.ExpenseService;
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
	public ExpenseResponse update(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
		return expenseService.update(currentUserProvider.getCurrentUserId(), id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		expenseService.delete(currentUserProvider.getCurrentUserId(), id);
	}

}
