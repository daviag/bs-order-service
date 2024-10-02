package com.daviag.bookshop.orderservice;

import com.daviag.bookshop.orderservice.book.Book;
import com.daviag.bookshop.orderservice.book.BookClient;
import com.daviag.bookshop.orderservice.order.domain.Order;
import com.daviag.bookshop.orderservice.order.domain.OrderStatus;
import com.daviag.bookshop.orderservice.order.web.OrderRequest;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@Import({TestcontainersConfiguration.class, TestChannelBinderConfiguration.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderServiceApplicationTests {

	private static KeycloakToken bjornTokens;
	private static KeycloakToken isabelleTokens;

	@Container
	private static final KeycloakContainer keycloakContainer =
			new KeycloakContainer("quay.io/keycloak/keycloak:25.0")
					.withRealmImportFile("test-realm-config.json");

	@DynamicPropertySource
	static void dynamicProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
				() -> keycloakContainer.getAuthServerUrl() + "/realms/Bookshop");
	}

	@BeforeAll
	static void generateAccessTokens() {
		WebClient webClient = WebClient.builder()
				.baseUrl(keycloakContainer.getAuthServerUrl()
						+ "/realms/Bookshop/protocol/openid-connect/token")
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
				.build();

		isabelleTokens = authenticateWith("isabelle", "password", webClient);
		bjornTokens = authenticateWith("bjorn", "password", webClient);
	}

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private OutputDestination output;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private BookClient bookClient;

	@Test
	void contextLoads() {
	}

	@Test
	void whenGetOrdersThenReturn() {
		String bookIsbn = "1234567893";
		Book book = new Book(bookIsbn, "Title", "Author", 9.90);
		given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.just(book));
		OrderRequest orderRequest = new OrderRequest(bookIsbn, 1);
		Order expectedOrder = webTestClient.post().uri("/orders")
				.headers(headers ->
						headers.setBearerAuth(bjornTokens.accessToken()))
				.bodyValue(orderRequest)
				.exchange()
				.expectStatus().is2xxSuccessful()
				.expectBody(Order.class).returnResult().getResponseBody();
		assertThat(expectedOrder).isNotNull();
	}

	@Test
	void whenPostRequestAndBookExistsThenOrderAccepted() {
		String bookIsbn = "1234567899";
		Book book = new Book(bookIsbn, "Title", "Author", 9.90);
		given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.just(book));
		OrderRequest orderRequest = new OrderRequest(bookIsbn, 3);

		Order createdOrder = webTestClient.post().uri("/orders")
				.headers(headers ->
						headers.setBearerAuth(isabelleTokens.accessToken()))
				.bodyValue(orderRequest)
				.exchange()
				.expectStatus().is2xxSuccessful()
				.expectBody(Order.class).returnResult().getResponseBody();

		assertThat(createdOrder).isNotNull();
		assertThat(createdOrder.bookIsbn()).isEqualTo(orderRequest.isbn());
		assertThat(createdOrder.quantity()).isEqualTo(orderRequest.quantity());
		assertThat(createdOrder.bookName()).isEqualTo(book.title() + " - " + book.author());
		assertThat(createdOrder.bookPrice()).isEqualTo(book.price());
		assertThat(createdOrder.status()).isEqualTo(OrderStatus.ACCEPTED);
	}

	@Test
	void whenPostRequestAndBookNotExistsThenOrderRejected() {
		String bookIsbn = "1234567894";
		given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.empty());
		OrderRequest orderRequest = new OrderRequest(bookIsbn, 3);

		Order createdOrder = webTestClient.post().uri("/orders")
				.headers(headers ->
						headers.setBearerAuth(bjornTokens.accessToken()))
				.bodyValue(orderRequest)
				.exchange()
				.expectStatus().is2xxSuccessful()
				.expectBody(Order.class).returnResult().getResponseBody();

		assertThat(createdOrder).isNotNull();
		assertThat(createdOrder.bookIsbn()).isEqualTo(orderRequest.isbn());
		assertThat(createdOrder.quantity()).isEqualTo(orderRequest.quantity());
		assertThat(createdOrder.status()).isEqualTo(OrderStatus.REJECTED);
	}

	@Test
	void whenGetRequestUnauthenticatedThen401() {
		webTestClient.get().uri("/orders")
				.exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	void whenPostRequestUnauthenticatedThen401() {
		String bookIsbn = "1234567894";
		OrderRequest orderRequest = new OrderRequest(bookIsbn, 3);

		webTestClient.post().uri("/orders")
				.bodyValue(orderRequest)
				.exchange()
				.expectStatus().isUnauthorized();
	}

	private static KeycloakToken authenticateWith(String username, String password, WebClient webClient) {
		return webClient
				.post()
				.body(
						BodyInserters.fromFormData("grant_type", "password")
								.with("client_id", "bookshop-test")
								.with("username", username)
								.with("password", password)
				)
				.retrieve()
				.bodyToMono(KeycloakToken.class)
				.block();
	}

	private record KeycloakToken(String accessToken) {
		@JsonCreator
		private KeycloakToken(
				@JsonProperty("access_token") final String accessToken) {
			this.accessToken = accessToken;
		}
	}

}
