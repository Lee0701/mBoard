package io.github.lee0701.mboard.module.candidates

data class DefaultHanjaCandidate(
    override val text: String,
    val composing: String,
    val extra: String,
    override val score: Float = 0f,
): Candidate