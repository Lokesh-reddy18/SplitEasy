package com.split.easy.domain.upi

import com.split.easy.domain.money.PAISE_PER_RUPEE
import java.net.URLEncoder

/** What the UPI app reported after a payment attempt (FR-8, FR-9, Q23). */
sealed interface UpiResult {
    /** SUCCESS: saved as a pending settlement with its transaction ID. */
    data class Success(val transactionId: String?) : UpiResult

    /** The app reported FAILURE, or an unknown status. */
    data object Failure : UpiResult

    /** The user backed out, or the app sent nothing back. */
    data object NoResponse : UpiResult
}

object UpiPayment {

    /** UPI IDs look like name@bank: letters, digits, dot, hyphen, underscore, then @handle. */
    private val UPI_ID = Regex("""^[A-Za-z0-9._-]{2,256}@[A-Za-z][A-Za-z0-9.-]{1,63}$""")

    fun isValidUpiId(upiId: String): Boolean = UPI_ID.matches(upiId.trim())

    /**
     * The standard UPI deep link: payee address, name, amount in rupees with two decimals,
     * note and currency. Any installed UPI app can handle it.
     */
    fun buildUri(payeeUpiId: String, payeeName: String, amountPaise: Long, note: String): String {
        require(amountPaise > 0) { "Amount must be positive" }
        val rupees = amountPaise / PAISE_PER_RUPEE
        val paise = (amountPaise % PAISE_PER_RUPEE).toString().padStart(2, '0')
        val params = listOf(
            "pa" to payeeUpiId.trim(),
            "pn" to payeeName,
            "am" to "$rupees.$paise",
            "tn" to note.take(MAX_NOTE),
            "cu" to "INR",
        )
        return "upi://pay?" + params.joinToString("&") { (key, value) -> "$key=${encode(value)}" }
    }

    /**
     * Reads the "response" string a UPI app returns, such as
     * "txnId=AXI123&responseCode=00&Status=SUCCESS&txnRef=…". Keys vary in case between apps.
     */
    fun parseResponse(response: String?): UpiResult {
        if (response.isNullOrBlank()) return UpiResult.NoResponse
        val values = response.split('&').mapNotNull { pair ->
            val parts = pair.split('=', limit = 2)
            if (parts.size == 2) parts[0].trim().lowercase() to parts[1].trim() else null
        }.toMap()
        val status = values["status"]?.uppercase()
        return when {
            status == "SUCCESS" -> UpiResult.Success(
                values["txnid"]?.takeIf {
                    it.isNotBlank()
                } ?: values["approvalrefno"],
            )

            status == null -> UpiResult.NoResponse

            else -> UpiResult.Failure
        }
    }

    /** Spaces as %20, not "+": some UPI apps show a literal "+" otherwise. */
    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private const val MAX_NOTE = 50
}
