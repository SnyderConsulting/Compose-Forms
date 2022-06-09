package com.example.composeforms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import com.example.composeforms.ui.theme.ComposeFormsTheme

class MainActivity : ComponentActivity() {

    class Keys {
        val password1 = "password1"
        val password2 = "password2"
        val error = "error"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val keys = Keys()
            val form by remember { mutableStateOf(Form(keys)) }
            form.addRule(
                ValidationRule(
                    inputKeys = listOf(
                        keys.password1,
                        keys.password2
                    ),
                    errorKeys = listOf(
                        keys.error
                    ),
                    predicate = {
                        form.state[keys.password1] == form.state[keys.password2]
                    },
                    errorMessage = "Passwords must match."
                )
            )

            val password1 = FormPassword(form.keys.password1, form)
            val password2 = FormPassword(form.keys.password2, form)
            val error = FormError(form.keys.error, form)

            ComposeFormsTheme {
                Column {
                    password1.Compose()
                    password2.Compose()
                    error.Compose()
                    Button(onClick = form::validate) {
                        Text(text = "Save")
                    }
                }
            }
        }
    }
}

data class ValidationRule(
    val inputKeys: List<String>,
    val errorKeys: List<String>,
    val predicate: () -> Boolean,
    val errorMessage: String
)

class Form<T>(val keys: T) {

    val state = mutableMapOf<String, String>()
    val rules = mutableListOf<ValidationRule>()
    val errors = mutableMapOf<String, String>()

    fun onDataChange(key: String, value: String) {
        state[key] = value
        validate()
    }

    fun validate() {
        rules.forEach { rule ->
            if (!rule.predicate()) {
                errors.putAll(rule.errorKeys.associateWith { rule.errorMessage })
            } else {
                rule.errorKeys.forEach {
                    errors.remove(it)
                }
            }
        }
    }

    fun addRule(
        rule: ValidationRule
    ) {
        rules.add(rule)
    }
}

@Composable
fun Password(onChange: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    TextField(value = value, onValueChange = {
        onChange(it)
        value = it
    })
}

//@Composable
//fun <T>Form(content: @Composable (FormState<T>) -> Unit) {
//    content()
//}

open class FormComponent() {
    fun validate(formBundle: Map<String, String>): Boolean {
        return false
    }

    fun onDataChanged() {

    }
}

class FormPassword<T>(val key: String, val formState: Form<T>): FormComponent() {
    @Composable
    fun Compose() {
        Password(onChange = {formState.onDataChange(key, it)})
    }
}

class FormError<T>(val key: String, val formState: Form<T>): FormComponent() {
    @Composable
    fun Compose() {
        formState.errors[key]?.also {
            Text("ERROR: ${formState.errors[key]}")
        }
    }
}