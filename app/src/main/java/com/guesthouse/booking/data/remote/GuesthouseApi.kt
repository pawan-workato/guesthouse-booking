package com.guesthouse.booking.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface GuesthouseApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/properties")
    suspend fun getProperties(): List<PropertyDto>

    @GET("api/rooms")
    suspend fun getRooms(@Query("propertyId") propertyId: Long? = null): List<RoomDto>

    @GET("api/guests")
    suspend fun getGuests(): List<GuestDto>

    @POST("api/guests/sync")
    suspend fun syncGuests(@Body request: GuestSyncRequest): GuestSyncResponse

    @GET("api/bookings")
    suspend fun getBookings(@Query("propertyId") propertyId: Long? = null): List<BookingDto>

    @POST("api/bookings/sync")
    suspend fun syncBookings(@Body request: BookingSyncRequest): BookingSyncResponse
}
