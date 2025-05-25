package com.example

import kotlinx.serialization.Serializable
import kotlinx.datetime.*

@Serializable
data class RegisterRequest(
    val uid: String,
    val email: String,
    val first_name: String,
    val last_name: String,
    val phone: String
)

@Serializable
data class ApiResponse(
    val Code: Int,
    val Message: String
)

@Serializable
data class UserProfileResponse(
    val email: String,
    val first_name: String,
    val last_name: String,
    val phone: String
)

@Serializable
data class UserProfileUpdateRequest(
    val email: String?,
    val first_name: String?,
    val last_name: String?,
    val phone: String?
)

@Serializable
data class Trailer(
    val url: String
)

@Serializable
data class TheatersForMovieResponse(
    val code: Int,
    val theaters: List<TheaterMovieShowingDetails>
)

@Serializable
data class TheaterMovieShowingDetails(
    val theaterId: String,
    val name: String,
    val location: String,
    val showTimings: List<String>,
    val movieExpiryDateInTheater: String,
    val seatPrices: String
)

@Serializable
data class MovieItem(
    val movieId: String,
    val movieName: String,
    val posterUrl: String,
    val genre: String
)

@Serializable
data class MoviesByLocationResponse(
    val code: Int,
    val movies: List<MovieItem>
)

@Serializable
data class MovieDetail(
    val movieId: String,
    val title: String,
    val rating: Double,
    val language: String,
    val description: String,
    val runtime: String,
    val coverpicture: String,
    val genre: String,
    val format: String
)

@Serializable
data class MovieDetailResponse(
    val code: Int,
    val movie: MovieDetail
)

@Serializable
data class UserBookingItem(
    val bookingId: String,
    val movieTitle: String,
    val theaterName: String,
    val showDate: LocalDate,
    val showTimings: String,
    val seats: String,
    val totalAmount: Double,
    val bookingTime: String,
)

@Serializable
data class UserBookingsResponse(
    val code: Int,
    val bookings: List<UserBookingItem>
)
@Serializable
data class BookingRequest(
    val uid: String,
    val movietitle: String,
    val showdate: String,
    val showtime: String,
    val seats: List<String>,
    val totalAmount: Double,
    val theaterName: String,
    val showid: String,
    val paymentId: String? = null
)

@Serializable
data class BookingConfirmationResponse(
    val code: Int,
    val message: String,
    val bookingId: String,
    val totalAmount: Double
)


@Serializable
data class BookedSeatsResponse(
    val code: Int,
    val showId: String,
    val bookedSeats: List<String>
)
