package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.transaction.TransactionFilterRequest;
import com.personal.backend_financeiro.dto.transaction.TransactionResponse;
import com.personal.backend_financeiro.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

	private final TransactionRepository transactionRepository;

	public Page<TransactionResponse> filter(Long userId, TransactionFilterRequest filter, Pageable pageable) {
		return transactionRepository.search(userId, filter, pageable);
	}

}
