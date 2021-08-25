package nl.rijksoverheid.ctr.shared.models

data class AppErrorResult(val step: Step, val e: Exception) : ErrorResult {
    override fun getCurrentStep(): Step {
        return step
    }

    override fun getException(): Exception {
        return e
    }
}