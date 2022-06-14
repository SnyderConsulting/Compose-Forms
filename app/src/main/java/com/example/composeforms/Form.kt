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
    private val rules = mutableListOf<Rule>()
    private var changeListener: () -> Unit = {}

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
        fun resetErrors(key: String? = null)
        fun onDataChange(key: String, value: String)
    }

    private fun validateSingle(key: String) {
        errors.remove(key)

        rules.filter { rule -> rule.inputKeys.contains(key) }.forEach { rule ->
            if (rule.isolated) {
                if (rule.isolatedPredicate(state, key) != true) {
                    errors[key] = errors[key].orEmpty().plus(FormError(rule.id, rule.errorMessage))
                } else {
                    errors[key] = errors[key].orEmpty().filter { it.key != rule.id }
                }
            } else {
                if (rule.predicate(state) != true) {
                    rule.errorKeys.filter { it == key }.forEach { key ->
                        errors[key] =
                            errors[key].orEmpty().plus(FormError(rule.id, rule.errorMessage))
                    }
                } else {
                    rule.errorKeys.forEach { key ->
                        errors[key] = errors[key].orEmpty().filter { it.key != rule.id }
                    }
                }
            }
        }

        changeListener()
    }

    private fun validateAll() {
        errors.clear()

        rules.forEach { rule ->
            if (rule.isolated) {
                rule.inputKeys.forEach { key ->
                    if (rule.isolatedPredicate(state, key) != true) {
                        errors[key] =
                            errors[key].orEmpty().plus(FormError(rule.id, rule.errorMessage))
                    }
                }
            } else {
                if (rule.predicate(state) != true) {
                    rule.errorKeys.forEach { key ->
                        errors[key] =
                            errors[key].orEmpty().plus(FormError(rule.id, rule.errorMessage))
                    }
                }
            }
        }

        changeListener()
    }

    fun setRules(
        rules: List<Rule>
    ) {
        this.rules.clear()
        this.rules.addAll(rules)
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

            override fun resetErrors(key: String?) {
                if (key == null) {
                    state.clear()
                    errorState.clear()
                } else {
                    state.remove(key)
                    errorState.remove(key)
                }
            }

            override fun onDataChange(key: String, value: String) {
                this@Form.onDataChange(key, value)
            }
        }

        content(formController)
    }

    companion object {

        @Composable
        fun Render(
            config: FormConfig,
            content: @Composable FormScope.(FormController) -> Unit
        ) {
            val form = Form()
            form.setRules(config.rules)
            form.Render {
                content(FormScope(config.errorComposable, form.errors), it)
            }
        }
    }
}

class Rule(
    val id: Int,
    val inputKeys: List<String>,
    val errorMessage: String,
    val predicate: (formState: Map<String, String>) -> Boolean?,
    val isolatedPredicate: (formState: Map<String, String>, key: String) -> Boolean?,
    val errorKeys: List<String> = listOf(*inputKeys.toTypedArray()), // Use same input keys for error keys by default
    val isolated: Boolean = false, //This indicates that each input key will not be affected by other fields
)

class FormConfig private constructor(
    val rules: List<Rule>,
    val errorComposable: @Composable (String) -> Unit
) {
    class Builder {

        private val rules = mutableListOf<Rule>()
        private var errorComposable: @Composable (errorMessage: String) -> Unit = { errorMessage ->
            Text(color = Color.Red, text = "Error: $errorMessage")
        }

        fun addRule(
            inputKeys: List<String>,
            errorMessage: String,
            predicate: (formState: Map<String, String>) -> Boolean?,
            errorKeys: List<String> = listOf(*inputKeys.toTypedArray()), // Use same input keys for error keys by default
        ): Builder {
            rules.add(
                Rule(
                    rules.count(),
                    inputKeys,
                    errorMessage,
                    predicate,
                    { _, _ -> null },
                    errorKeys,
                    isolated = false
                )
            )

            return this
        }

        fun addIsolatedRule(
            inputKeys: List<String>,
            errorMessage: String,
            predicate: (formState: Map<String, String>, key: String) -> Boolean?
        ): Builder {
            rules.add(
                Rule(
                    rules.count(),
                    inputKeys,
                    errorMessage,
                    { null },
                    predicate,
                    emptyList(),
                    isolated = true
                )
            )

            return this
        }

        fun setErrorComposable(content: @Composable (String) -> Unit): Builder {
            errorComposable = content
            return this
        }

        fun build(): FormConfig {
            return FormConfig(
                rules,
                errorComposable
            )
        }
    }
}

typealias FormErrorBundle = Map<String, List<FormError>>

data class FormError(val key: Int, val errorMessage: String)

data class FormScope(
    private val errorComposable: @Composable (String) -> Unit,
    val errors: SnapshotStateMap<String, List<FormError>>
) {
    @Composable
    fun FormItem(key: String, content: @Composable (key: String, errors: List<String>) -> Unit) {
        content(key, errors[key].orEmpty().map { it.errorMessage })
    }

    @Composable
    fun FormItemWithError(
        key: String,
        content: @Composable (key: String, errors: List<String>) -> Unit
    ) {
        Column {
            content(key, errors[key].orEmpty().map { it.errorMessage })
            errors[key]?.firstOrNull()?.also {
                errorComposable(it.errorMessage)
            }
        }
    }
}