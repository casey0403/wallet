package com.wannabe.wallet.presentation.model.response

data class CommonResponse<T>(
    val code: String,
    val status: Int,
    val data: T? = null,
) {
    companion object {
        fun <T> success(data: T): CommonResponse<T> {
            return CommonResponse(
                code = "SUCCESS",
                status = 200,
                data = data,
            )
        }

        fun failed(status: Int, data: ErrorDetailResponse): CommonResponse<ErrorDetailResponse> {
            return CommonResponse(
                code = "FAILED",
                status = status,
                data = data,
            )
        }
    }
}

data class ErrorDetailResponse(
    val errorCode: String,
    val message: String,
)
