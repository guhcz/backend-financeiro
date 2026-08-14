package com.personal.backend_financeiro.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryIntegrationTest extends AbstractApiIntegrationTest {

	@Test
	void create_returns201_andListsAfterwards() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(post("/api/v1/categories")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"name":"Food","color":"#FF0000","icon":"food"}"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Food"));

		mockMvc.perform(get("/api/v1/categories")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].name").value("Food"));
	}

	@Test
	void create_returns409_onDuplicateNameForSameUser() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		createCategory(token, "Food", "#FF0000");

		mockMvc.perform(post("/api/v1/categories")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"name":"food","color":"#123456","icon":null}"""))
				.andExpect(status().isConflict());
	}

	@Test
	void create_returns400_onInvalidColor() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(post("/api/v1/categories")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"name":"Food","color":"red","icon":null}"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void otherUser_cannotUpdate_returns404() throws Exception {
		String tokenAlice = registerAndLogin("Alice", "alice@example.com", "password123");
		String tokenBob = registerAndLogin("Bob", "bob@example.com", "password123");
		long categoryId = createCategory(tokenAlice, "Food", "#FF0000");

		mockMvc.perform(put("/api/v1/categories/" + categoryId)
						.header("Authorization", "Bearer " + tokenBob)
						.contentType(APPLICATION_JSON)
						.content("""
								{"name":"Hack","color":"#000000","icon":null}"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void delete_returns409_whenCategoryHasExpenses() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food", "#FF0000");

		mockMvc.perform(post("/api/v1/expenses")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"Lunch","amount":25.50,
								"expenseDate":"2026-07-10","transactionMethodId":%d,"cardTransactionMode":null,"notes":null}"""
								.formatted(categoryId, createTransactionMethod(token, "Pix", "PIX"))))
				.andExpect(status().isCreated());

		mockMvc.perform(delete("/api/v1/categories/" + categoryId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isConflict());
	}

	@Test
	void delete_returns204_whenCategoryHasNoExpenses() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food", "#FF0000");

		mockMvc.perform(delete("/api/v1/categories/" + categoryId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());
	}

	private long createCategory(String token, String name, String color) throws Exception {
		var result = mockMvc.perform(post("/api/v1/categories")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("{\"name\":\"%s\",\"color\":\"%s\",\"icon\":null}".formatted(name, color)))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

}
