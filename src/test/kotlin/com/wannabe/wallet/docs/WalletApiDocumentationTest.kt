package com.wannabe.wallet.docs

import com.epages.restdocs.apispec.ResourceDocumentation.resource
import com.epages.restdocs.apispec.ResourceSnippetParameters
import com.fasterxml.jackson.databind.ObjectMapper
import com.wannabe.wallet.TestcontainersConfiguration
import com.wannabe.wallet.infrastructure.jpa.IdempotencyRequestJpaRepository
import com.wannabe.wallet.infrastructure.jpa.WalletJpaRepository
import com.wannabe.wallet.infrastructure.jpa.WalletTransactionJpaRepository
import com.wannabe.wallet.infrastructure.jpa.entity.WalletJPAEntity
import com.wannabe.wallet.presentation.model.request.WithdrawalRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@Import(TestcontainersConfiguration::class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@SpringBootTest
class WalletApiDocumentationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val walletRepository: WalletJpaRepository,
    private val idempotencyRequestRepository: IdempotencyRequestJpaRepository,
    private val walletTransactionRepository: WalletTransactionJpaRepository,
) {
    private val walletId = "docs-wallet"

    @BeforeEach
    fun setUp() {
        walletTransactionRepository.deleteAll()
        idempotencyRequestRepository.deleteAll()
        walletRepository.deleteAll()
        walletRepository.save(
            WalletJPAEntity(
                walletId = walletId,
                balance = 100000.toBigDecimal(),
            ),
        )
    }

    @Test
    fun `document wallet withdrawal API`() {
        val request = WithdrawalRequest(
            amount = 10000L,
            transactionId = "DOCS-TXN-001",
        )

        mockMvc.perform(
            post("/api/v1/wallets/{walletId}/withdrawals", walletId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andDo(
                document(
                    "wallet-withdrawal",
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("Wallet")
                            .summary("월렛 출금")
                            .description(
                                "walletId 기준으로 Redis 분산락을 획득한 뒤, version과 balance 조건을 포함한 atomic update로 출금을 처리합니다. transactionId는 멱등성 키로 사용합니다.",
                            )
                            .pathParameters(
                                parameterWithName("walletId").description("월렛 ID"),
                            )
                            .requestFields(
                                fieldWithPath("amount")
                                    .type(JsonFieldType.NUMBER)
                                    .description("출금 금액"),
                                fieldWithPath("transactionId")
                                    .type(JsonFieldType.STRING)
                                    .description("멱등성 보장을 위한 거래 ID"),
                                fieldWithPath("currency")
                                    .type(JsonFieldType.STRING)
                                    .description("ISO 4217 통화 코드"),
                            )
                            .responseFields(
                                *commonSuccessResponseFields(
                                    dataDescription = "출금 처리 결과",
                                    dataFields = transactionResponseFields("data."),
                                ).toTypedArray(),
                            )
                            .build(),
                    ),
                ),
            )
    }

    @Test
    fun `document wallet transaction history API`() {
        val request = WithdrawalRequest(
            amount = 10000L,
            transactionId = "DOCS-TXN-002",
        )

        mockMvc.perform(
            post("/api/v1/wallets/{walletId}/withdrawals", walletId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)

        mockMvc.perform(
            get("/api/v1/wallets/{walletId}/transactions", walletId)
                .queryParam("transactionType", "withdraw")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andDo(
                document(
                    "wallet-transaction-history",
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("Wallet")
                            .summary("월렛 거래내역 조회")
                            .description("walletId에 해당하는 월렛의 거래내역을 최신 처리 일시 기준으로 조회합니다.")
                            .pathParameters(
                                parameterWithName("walletId").description("월렛 ID"),
                            )
                            .queryParameters(
                                parameterWithName("transactionType")
                                    .optional()
                                    .description("거래 유형 필터. 생략 시 전체 조회, deposit은 입금, withdraw는 출금"),
                            )
                            .responseFields(
                                fieldWithPath("code")
                                    .type(JsonFieldType.STRING)
                                    .description("공통 응답 코드. 성공 시 SUCCESS"),
                                fieldWithPath("status")
                                    .type(JsonFieldType.NUMBER)
                                    .description("HTTP status code"),
                                fieldWithPath("data")
                                    .type(JsonFieldType.OBJECT)
                                    .description("응답 데이터"),
                                fieldWithPath("data.transactions")
                                    .type(JsonFieldType.ARRAY)
                                    .description("거래내역 목록"),
                                *transactionResponseFields("data.transactions[].").toTypedArray(),
                            )
                            .build(),
                    ),
                ),
            )
    }

    private fun commonSuccessResponseFields(
        dataDescription: String,
        dataFields: List<org.springframework.restdocs.payload.FieldDescriptor>,
    ) = listOf(
        fieldWithPath("code")
            .type(JsonFieldType.STRING)
            .description("공통 응답 코드. 성공 시 SUCCESS"),
        fieldWithPath("status")
            .type(JsonFieldType.NUMBER)
            .description("HTTP status code"),
        fieldWithPath("data")
            .type(JsonFieldType.OBJECT)
            .description(dataDescription),
    ) + dataFields

    private fun transactionResponseFields(prefix: String = "") = listOf(
        fieldWithPath("${prefix}transactionId")
            .type(JsonFieldType.STRING)
            .description("거래 ID"),
        fieldWithPath("${prefix}walletId")
            .type(JsonFieldType.STRING)
            .description("월렛 ID"),
        fieldWithPath("${prefix}type")
            .type(JsonFieldType.STRING)
            .description("거래 유형"),
        fieldWithPath("${prefix}status")
            .type(JsonFieldType.STRING)
            .description("거래 처리 상태"),
        fieldWithPath("${prefix}amount")
            .type(JsonFieldType.NUMBER)
            .description("거래 금액"),
        fieldWithPath("${prefix}currency")
            .type(JsonFieldType.STRING)
            .description("통화 코드"),
        fieldWithPath("${prefix}balance")
            .type(JsonFieldType.NUMBER)
            .description("거래 처리 후 잔액"),
        fieldWithPath("${prefix}version")
            .type(JsonFieldType.NUMBER)
            .description("거래 처리 후 월렛 version"),
        fieldWithPath("${prefix}processedAt")
            .type(JsonFieldType.STRING)
            .description("거래 처리 일시"),
    )
}
