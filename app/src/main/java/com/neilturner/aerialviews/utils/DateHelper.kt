package com.neilturner.aerialviews.utils

import android.content.Context
import com.neilturner.aerialviews.R
import com.neilturner.aerialviews.models.enums.DateType
import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateHelper {
    fun formatDate(
        context: Context,
        type: DateType,
        custom: String?,
    ): String =
        when (type) {
            DateType.FULL -> {
                DateFormat.getDateInstance(DateFormat.FULL).format(Date())
            }
            DateType.COMPACT -> {
                DateFormat.getDateInstance(DateFormat.SHORT).format(Date())
            }
            else -> {
                try {
                    val today = Calendar.getInstance().time
                    val formatter = SimpleDateFormat(custom, Locale.getDefault())
                    formatter.format(today)
                } catch (e: Exception) {
                    Timber.e(e)
                    context.resources.getString(R.string.appearance_date_custom_error)
                }
            }
        }
}
