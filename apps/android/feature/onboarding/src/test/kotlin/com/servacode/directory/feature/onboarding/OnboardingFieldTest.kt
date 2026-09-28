package com.servacode.directory.feature.onboarding

import com.servacode.directory.core.model.AppError
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingFieldTest {
    @Test fun `a refused WhatsApp is pointed at its own field`() {
        val error = AppError(
            AppError.Kind.VALIDATION,
            code = "VALIDATION_ERROR",
            fieldErrors = mapOf("whatsapp" to listOf("Enter a valid Syrian mobile number.")),
        )

        assertEquals(setOf(OnboardingField.WHATSAPP), OnboardingField.named(error))
    }

    @Test fun `several fields, and ones the form does not show are ignored`() {
        val error = AppError(
            AppError.Kind.VALIDATION,
            fieldErrors = mapOf("phone" to listOf("x"), "whatsapp" to listOf("y"), "cityId" to listOf("z")),
        )

        assertEquals(setOf(OnboardingField.PHONE, OnboardingField.WHATSAPP), OnboardingField.named(error))
    }

    @Test fun `no error names nothing`() {
        assertEquals(emptySet<OnboardingField>(), OnboardingField.named(null))
        assertEquals(emptySet<OnboardingField>(), OnboardingField.named(AppError(AppError.Kind.OFFLINE)))
    }
}
