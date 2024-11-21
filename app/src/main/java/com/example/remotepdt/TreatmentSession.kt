package com.example.remotepdt

data class TreatmentSession(
    val estimated_duration_for_drug_administration: Float?,
    val estimated_duration_for_light_administration: Float?,
    val estimated_duration_for_wash_administration: Float?
)
