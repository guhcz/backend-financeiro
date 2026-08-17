package com.personal.backend_financeiro.integration;

import com.personal.backend_financeiro.AbstractIntegrationTest;
import com.personal.backend_financeiro.dto.auth.LoginRequest;
import com.personal.backend_financeiro.dto.user.CreateUserRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
abstract class AbstractApiIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected ObjectMapper objectMapper;

	protected Long registerUser(String name, String email, String password) throws Exception {
		String body = objectMapper.writeValueAsString(new CreateUserRequest(name, email, password));

		MvcResult result = mockMvc.perform(post("/api/v1/users/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andReturn();

		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

	protected String login(String email, String password) throws Exception {
		String body = objectMapper.writeValueAsString(new LoginRequest(email, password));

		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andReturn();

		return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
	}

	protected String registerAndLogin(String name, String email, String password) throws Exception {
		registerUser(name, email, password);
		return login(email, password);
	}

	protected long createTransactionMethod(String token, String name, String type) throws Exception {
		return createTransactionMethod(token, name, type, null, null);
	}

	protected long createTransactionMethod(String token, String name, String type, Integer closingDay, Integer dueDay) throws Exception {
		String card = closingDay == null ? "null" : "{\"closingDay\":%d,\"dueDay\":%d}".formatted(closingDay, dueDay);
		MvcResult result = mockMvc.perform(post("/api/v1/transaction-methods")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"%s","type":"%s","card":%s}""".formatted(name, type, card)))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

}
