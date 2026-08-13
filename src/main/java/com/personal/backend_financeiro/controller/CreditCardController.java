package com.personal.backend_financeiro.controller;

import com.personal.backend_financeiro.dto.creditcard.CreditCardRequest;
import com.personal.backend_financeiro.dto.creditcard.CreditCardResponse;
import com.personal.backend_financeiro.security.CurrentUserProvider;
import com.personal.backend_financeiro.service.CreditCardService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/credit-cards")
@RequiredArgsConstructor
@Tag(name = "Credit Cards")
public class CreditCardController {

	private final CreditCardService creditCardService;
	private final CurrentUserProvider currentUserProvider;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CreditCardResponse create(@Valid @RequestBody CreditCardRequest request) {
		return creditCardService.create(currentUserProvider.getCurrentUserId(), request);
	}

	@GetMapping("/all")
	public List<CreditCardResponse> listAll() {
		return creditCardService.listAllByUser(currentUserProvider.getCurrentUserId());
	}

	@GetMapping
	public Page<CreditCardResponse> list(Pageable pageable) {
		return creditCardService.listByUser(currentUserProvider.getCurrentUserId(), pageable);
	}

	@PutMapping("/{id}")
	public CreditCardResponse update(@PathVariable Long id, @Valid @RequestBody CreditCardRequest request) {
		return creditCardService.update(currentUserProvider.getCurrentUserId(), id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		creditCardService.delete(currentUserProvider.getCurrentUserId(), id);
	}

}
