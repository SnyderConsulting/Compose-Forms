package com.example.composeforms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.composeforms.ui.theme.ComposeFormsTheme

class MainActivity : ComponentActivity() {

    class Keys {
        val password1 = "password1"
        val password2 = "password2"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val keys = Keys()
        var form by mutableStateOf(Form())

        form.setRules(
            ValidationRule(
                inputKeys = listOf(
                    keys.password1,
                ),
                errorKeys = listOf(
                    keys.password1
                ),
                predicate = {
                    form.state[keys.password1]?.let {
                        it.length >= 8
                    } ?: false
                },
                errorMessage = "Must be at least 8 characters"
            ),
            ValidationRule(
                inputKeys = listOf(
                    keys.password2,
                ),
                errorKeys = listOf(
                    keys.password2
                ),
                predicate = {
                    form.state[keys.password2]?.let {
                        it.length >= 8
                    } ?: false
                },
                errorMessage = "Must be at least 8 characters"
            ),
            ValidationRule(
                inputKeys = listOf(
                    keys.password1,
                    keys.password2
                ),
                errorKeys = listOf(
                    keys.password2
                ),
                predicate = {
                    form.state[keys.password1] == form.state[keys.password2]
                },
                errorMessage = "Passwords must match."
            ),
        )

        setContent {
            //Needed to trigger recomposition
            form.setChangeListener {
                form = form
            }

            val password1 = FormPassword("Password", keys.password1, form)
            val password2 = FormPassword("Confirm Password", keys.password2, form)

            ComposeFormsTheme {
                Column {
                    password1.Compose()
                    password2.Compose()
                    Button(onClick = { form.validate() }) {
                        Text(text = "Save")
                    }
                }
            }
        }
    }
}

@Composable
fun Password(label: String, onChange: (String) -> Unit) {
    var value by remember { mutableStateOf("") }

    Column(Modifier.padding(top = 16.dp)) {
        Text(text = "$label:")
        TextField(value = value, onValueChange = {
            onChange(it)
            value = it
        })
    }
}

class FormPassword(private val label: String, key: String, formState: Form) :
    FormComponent(key, formState) {
    @Composable
    override fun GetComposable() {
        Password(
            label = label,
            onChange = { formState.onDataChange(key, it) }
        )
    }
}


//Form Boilerplate

data class FormConfig(val key: String, val formState: Form)

data class ValidationRule(
    val inputKeys: List<String>,
    val errorKeys: List<String>,
    val predicate: () -> Boolean,
    val errorMessage: String
)

data class FormError(val key: Int, val errorMessage: String)

class Form {

    val state = mutableMapOf<String, String>()
    val errors = mutableMapOf<String, List<FormError>>()
    private val rules = mutableListOf<InternalValidationRule>()
    private var changeListener: () -> Unit = {}

    private data class InternalValidationRule(
        val id: Int,
        val inputKeys: List<String>,
        val errorKeys: List<String>,
        val predicate: () -> Boolean,
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

    private fun validateSingle(key: String) {
        errors.remove(key)

        rules.filter { rule -> rule.inputKeys.contains(key) }.forEach { rule ->
            if (!rule.predicate()) {
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
            if (!rule.predicate()) {
                rule.errorKeys.forEach { key ->
                    errors[key] = errors[key].orEmpty().plus(FormError(rule.id, rule.errorMessage))
                }
            }
        }

        changeListener()
    }

    fun validate(key: String? = null) {
        if (key == null) {
            validateAll()
        } else {
            validateSingle(key)
        }
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

    fun setChangeListener(block: () -> Unit) {
        changeListener = block
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
}

abstract class FormComponent(val key: String, val formState: Form) {

    @Composable
    protected abstract fun GetComposable()

    @Composable
    fun Compose() {
        Column {
            GetComposable()
            formState.errors[key]?.firstOrNull()?.also {
                Text(color = Color.Red, text = "Error: ${it.errorMessage}")
            }
        }
    }
}