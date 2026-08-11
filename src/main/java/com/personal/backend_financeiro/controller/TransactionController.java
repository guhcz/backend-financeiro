package com.personal.backend_financeiro.controller;

import com.personal.backend_financeiro.dto.transaction.TransactionFilterRequest;
import com.personal.backend_financeiro.dto.transaction.TransactionResponse;
import com.personal.backend_financeiro.security.CurrentUserProvider;
import com.personal.backend_financeiro.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = """
		Visão unificada, somente leitura, de despesas e receitas — não existe uma entidade \
		Transaction: este endpoint apenas une Expense e Income (via UNION ALL no banco) para o \
		frontend exibir uma única tabela de movimentações. type identifica a origem de cada item \
		(EXPENSE ou INCOME); categorias são compartilhadas entre os dois domínios. Limite mensal \
		e planejamento por categoria continuam considerando somente despesas — este endpoint não \
		afeta e não é afetado por essas regras. Toda escrita continua acontecendo nos endpoints \
		próprios de /expenses, /recurring-expenses, /incomes e /recurring-incomes.""")
public class TransactionController {

	private final TransactionService transactionService;
	private final CurrentUserProvider currentUserProvider;

	@GetMapping
	@Operation(summary = "Lista as movimentações (despesas e receitas) do usuário autenticado", description = """
			Quando type não é informado, retorna despesas e receitas juntas. Paginação e \
			ordenação são calculadas pelo banco sobre o conjunto unificado (não é a concatenação \
			de duas páginas separadas), garantindo totalElements e ordenação corretos mesmo \
			misturando os dois tipos. Ordenação aceita as propriedades "date" e "amount" (ex.: \
			sort=date,desc ou sort=amount,asc); o padrão é date,desc, sempre com um desempate \
			determinístico adicional. Como Expense e Income podem ter o mesmo id numérico, use \
			o campo transactionKey (não id) como identificador único de cada linha no frontend.""")
	public Page<TransactionResponse> filter(@ModelAttribute TransactionFilterRequest filter, Pageable pageable) {
		return transactionService.filter(currentUserProvider.getCurrentUserId(), filter, pageable);
	}

}
