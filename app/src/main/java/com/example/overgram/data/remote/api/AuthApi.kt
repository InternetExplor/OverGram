package com.example.overgram.data.remote.api

import com.example.overgram.core.network.AuthInterceptor
import com.example.overgram.data.remote.dto.OtpRequestDto
import com.example.overgram.data.remote.dto.OtpVerifyRequestDto
import com.example.overgram.data.remote.dto.RefreshRequestDto
import com.example.overgram.data.remote.dto.TokenPairDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AuthApi {

    @Headers(AuthInterceptor.NO_AUTH_HEADER)
    @POST("v1/auth/otp/request")
    suspend fun requestOtp(@Body body: OtpRequestDto): Response<Unit>

    @Headers(AuthInterceptor.NO_AUTH_HEADER)
    @POST("v1/auth/otp/verify")
    suspend fun verifyOtp(@Body body: OtpVerifyRequestDto): Response<TokenPairDto>

    @Headers(AuthInterceptor.NO_AUTH_HEADER)
    @POST("v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): Response<TokenPairDto>

    /** Bearer token is attached by [AuthInterceptor]. */
    @POST("v1/auth/logout")
    suspend fun logout(): Response<Unit>
}
