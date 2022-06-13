package com.example.composeforms

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.graphics.Color

class Form private constructor() {

    private val state = mutableMapOf<String, String>()
    private val errors = mutableStateMapOf<String, List<FormError>>()
    private val rules = mutableListOf<InternalValidationRule>()
    private var changeListener: () -> Unit = {}

    private data class InternalValidationRule(
        val id: Int,
        val inputKeys: List<String>,
        val errorKeys: List<String>,
        val predicate: (formState: Map<String, String>) -> Boolean,
        val errorMessage: String
    )

    fun onDataChange(key: String, value: String) {
        if (value.isBlank()) {
            state.remove(key)
        } else {
            state[key] = value
        }
        validateSingle(key)
        changeListener()
    }

    interface FormController {
        val errors: FormErrorBundle
        fun validate(key: String? = null)
        fun onDataChange(key: String, value: String)
    }

    private fun validateSingle(key: String) {
        errors.remove(key)

        rules.filter { rule -> rule.inputKeys.contains(key) }.forEach { rule ->
            if (!rule.predicate(state)) {
                rule.errorKeys.filter { it == key }.forEach { key ->
                    errors[key] = errors[key].orEmpty().plus(FormError(rule.id, rule.errorMessage))
                }
            } else {
                rule.errorKeys.forEach { key ->
                    errors[key] = errors[key].orEmpty().filter { it.key != rule.id }
                }
            }
        }

        changeListener()
    }

    private fun validateAll() {
        errors.clear()

        rules.forEach { rule ->
            if (!rule.predicate(state)) {
                rule.errorKeys.forEach { key ->
                    errors[key] = errors[key].orEmpty().plus(FormError(rule.id, rule.errorMessage))
                }
            }
        }

        changeListener()
    }

    fun setRules(
        vararg rules: ValidationRule
    ) {
        this.rules.clear()
        this.rules.addAll(rules.mapIndexed { index, validationRule ->
            InternalValidationRule(
                id = index,
                inputKeys = validationRule.inputKeys,
                errorKeys = validationRule.errorKeys,
                predicate = validationRule.predicate,
                errorMessage = validationRule.errorMessage
            )
        })
    }

    @Composable
    fun Render(content: @Composable (FormController) -> Unit) {
        val errorState = remember {
            errors
        }
        val formController = object : FormController {
            override val errors: FormErrorBundle
                get() = errorState

            override fun validate(key: String?) {
                if (key == null) {
                    validateAll()
                } else {
                    validateSingle(key)
                }
            }

            override fun onDataChange(key: String, value: String) {
                this@Form.onDataChange(key, value)
            }
        }

        content(formController)
    }

    override fun equals(other: Any?): Boolean {
        return false
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + rules.hashCode()
        result = 31 * result + errors.hashCode()
        result = 31 * result + changeListener.hashCode()
        return result
    }

    companion object {

        @Composable
        fun Render(rules: List<ValidationRule>, content: @Composable FormScope.(FormController) -> Unit) {
            val form = Form()
            form.setRules(*rules.toTypedArray())
            form.Render {
                content(FormScope(form.errors), it)
            }
        }
    }
}

data class ValidationRule(
    val inputKeys: List<String>,
    val errorKeys: List<String> = listOf(*inputKeys.toTypedArray()), // Use same input keys for error keys by default
    val predicate: (formState: Map<String, String>) -> Boolean,
    val errorMessage: String
)

typealias FormErrorBundle = Map<String, List<FormError>>

data class FormError(val key: Int, val errorMessage: String)

data class FormScope(val errors: SnapshotStateMap<String, List<FormError>>) {
    @Composable
    fun FormItem(key: String, content: @Composable (key: String, errors: List<String>) -> Unit) {
        content(key, errors[key].orEmpty().map { it.errorMessage })
    }

    @Composable
    fun FormItemWithError(key: String, content: @Composable (key: String, errors: List<String>) -> Unit) {
        Column {
            content(key, errors[key].orEmpty().map { it.errorMessage })
            errors[key]?.firstOrNull()?.also {
                Text(color = Color.Red, text = "Error: ${it.errorMessage}")
            }
        }
    }
}