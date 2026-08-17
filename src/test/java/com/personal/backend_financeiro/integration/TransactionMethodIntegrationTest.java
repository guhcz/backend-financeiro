package com.personal.backend_financeiro.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionMethodIntegrationTest extends AbstractApiIntegrationTest {

	@Test
	void create_pix_returns201_withNullCard() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(post("/api/v1/transaction-methods")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"name":"Pix","type":"PIX","card":null}"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Pix"))
				.andExpect(jsonPath("$.type").value("PIX"))
				.andExpect(jsonPath("$.active").value(true))
				.andExpect(jsonPath("$.card").doesNotExist());
	}

	@Test
	void create_card_returns201_withNestedCardDetails() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(post("/api/v1/transaction-methods")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"name":"Nubank","type":"CARD","card":{"closingDay":20,"dueDay":10}}"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Nubank"))
				.andExpect(jsonPath("$.type").value("CARD"))
				.andExpect(jsonPath("$.card.closingDay").value(20))
				.andExpect(jsonPath("$.card.dueDay").value(10));
	}

	@Test
	void create_card_returns400_whenCardMissing() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(post("/api/v1/transaction-methods")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"name":"Nubank","type":"CARD","card":null}"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void create_pix_returns400_whenCardProvided() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(post("/api/v1/transaction-methods")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"name":"Pix","type":"PIX","card":{"closingDay":20,"dueDay":10}}"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void create_returns409_onDuplicateName() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		createTransactionMethod(token, "Nubank", "CARD", 20, 10);

		mockMvc.perform(post("/api/v1/transaction-methods")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"name":"Nubank","type":"CARD","card":{"closingDay":5,"dueDay":15}}"""))
				.andExpect(status().isConflict());
	}

	@Test
	void create_returns400_whenClosingDayOutOfRange() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(post("/api/v1/transaction-methods")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"name":"Nubank","type":"CARD","card":{"closingDay":32,"dueDay":10}}"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void listAll_isIsolatedPerUser() throws Exception {
		String tokenAlice = registerAndLogin("Alice", "alice@example.com", "password123");
		String tokenBob = registerAndLogin("Bob", "bob@example.com", "password123");
		createTransactionMethod(tokenAlice, "Nubank", "CARD", 20, 10);

		mockMvc.perform(get("/api/v1/transaction-methods/all")
						.header("Authorization", "Bearer " + tokenBob))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void update_editsNameAndCardDetails() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long id = createTransactionMethod(token, "Nubank", "CARD", 20, 10);

		mockMvc.perform(put("/api/v1/transaction-methods/" + id)
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"name":"Nubank Ultravioleta","card":{"closingDay":5,"dueDay":15}}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Nubank Ultravioleta"))
				.andExpect(jsonPath("$.card.closingDay").value(5))
				.andExpect(jsonPath("$.card.dueDay").value(15));
	}

	@Test
	void update_returns404_whenBelongsToAnotherUser() throws Exception {
		String tokenAlice = registerAndLogin("Alice", "alice@example.com", "password123");
		String tokenBob = registerAndLogin("Bob", "bob@example.com", "password123");
		long id = createTransactionMethod(tokenAlice, "Nubank", "CARD", 20, 10);

		mockMvc.perform(put("/api/v1/transaction-methods/" + id)
						.header("Authorization", "Bearer " + tokenBob)
						.contentType(APPLICATION_JSON)
						.content("""
								{"name":"Roubado","card":{"closingDay":1,"dueDay":10}}"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void delete_returns409_whenMethodHasExpenses() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long transactionMethodId = createTransactionMethod(token, "Nubank", "CARD", 20, 10);
		long categoryId = createCategory(token, "Food");
		mockMvc.perform(post("/api/v1/expenses")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"Compra","amount":50.00,
								"expenseDate":"2026-08-15","transactionMethodId":%d,"cardTransactionMode":"CREDIT","notes":null}"""
								.formatted(categoryId, transactionMethodId)))
				.andExpect(status().isCreated());

		mockMvc.perform(delete("/api/v1/transaction-methods/" + transactionMethodId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isConflict());
	}

	@Test
	void delete_thenListAll_omitsMethod() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long id = createTransactionMethod(token, "Nubank", "CARD", 20, 10);

		mockMvc.perform(delete("/api/v1/transaction-methods/" + id)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/transaction-methods/all")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void listAll_returnsCardAlongsideNonCardMethods() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		createTransactionMethod(token, "Pix", "PIX");
		createTransactionMethod(token, "Nubank", "CARD", 20, 10);

		var result = mockMvc.perform(get("/api/v1/transaction-methods/all")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andReturn();

		var methods = objectMapper.readTree(result.getResponse().getContentAsString());
		var pix = methods.get(0).get("name").asText().equals("Pix") ? methods.get(0) : methods.get(1);
		var nubank = methods.get(0).get("name").asText().equals("Nubank") ? methods.get(0) : methods.get(1);
		org.assertj.core.api.Assertions.assertThat(pix.get("card").isNull()).isTrue();
		org.assertj.core.api.Assertions.assertThat(nubank.get("card").get("closingDay").asInt()).isEqualTo(20);
	}

	private long createCategory(String token, String name) throws Exception {
		var result = mockMvc.perform(post("/api/v1/categories")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("{\"name\":\"%s\",\"color\":\"#FF0000\",\"icon\":null}".formatted(name)))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

}
