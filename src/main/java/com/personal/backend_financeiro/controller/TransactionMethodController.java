package com.personal.backend_financeiro.controller;

import com.personal.backend_financeiro.dto.transactionmethod.TransactionMethodCreateRequest;
import com.personal.backend_financeiro.dto.transactionmethod.TransactionMethodResponse;
import com.personal.backend_financeiro.dto.transactionmethod.TransactionMethodUpdateRequest;
import com.personal.backend_financeiro.security.CurrentUserProvider;
import com.personal.backend_financeiro.service.TransactionMethodService;
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
@RequestMapping("/api/v1/transaction-methods")
@RequiredArgsConstructor
@Tag(name = "Transaction Methods", description = """
		Formas de pagamento do usuário -- cadastro único da Central de Cadastros. Um cartão \
		(ex.: Nubank) é uma forma de pagamento como qualquer outra, do tipo CARD, com os \
		detalhes de fechamento/vencimento aninhados em "card"; não existe um cadastro de \
		Cartões separado. A modalidade crédito/débito é escolhida no lançamento da despesa \
		(cardTransactionMode), não aqui.""")
public class TransactionMethodController {

	private final TransactionMethodService transactionMethodService;
	private final CurrentUserProvider currentUserProvider;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TransactionMethodResponse create(@Valid @RequestBody TransactionMethodCreateRequest request) {
		return transactionMethodService.create(currentUserProvider.getCurrentUserId(), request);
	}

	@GetMapping("/all")
	public List<TransactionMethodResponse> listAll() {
		return transactionMethodService.listAllByUser(currentUserProvider.getCurrentUserId());
	}

	@GetMapping
	public Page<TransactionMethodResponse> list(Pageable pageable) {
		return transactionMethodService.listByUser(currentUserProvider.getCurrentUserId(), pageable);
	}

	@PutMapping("/{id}")
	public TransactionMethodResponse update(@PathVariable Long id, @Valid @RequestBody TransactionMethodUpdateRequest request) {
		return transactionMethodService.update(currentUserProvider.getCurrentUserId(), id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		transactionMethodService.delete(currentUserProvider.getCurrentUserId(), id);
	}

}
