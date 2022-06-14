package com.example.composeforms

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.composeforms.ui.theme.ComposeFormsTheme

class MainActivity : ComponentActivity() {

    private object Keys {
        const val password1 = "password1"
        const val password2 = "password2"
        const val email = "email"
        const val firstName = "firstName"
        const val lastName = "lastName"
    }

    private val rules = Validation.Builder()
        .isolatedRule(
            inputKeys = listOf(Keys.password1, Keys.password2, Keys.firstName, Keys.lastName),
            errorMessage = "Required",
            predicate = { state, key ->
                state[key]?.isNotEmpty()
            },
        )
        .isolatedRule(
            inputKeys = listOf(Keys.password1, Keys.password2),
            errorMessage = "Must be at least 8 characters",
            predicate = { state, key ->
                state[key]?.let {
                    it.length >= 8
                }
            },
        )
        .rule(
            inputKeys = listOf(Keys.password1, Keys.password2),
            errorKeys = listOf(Keys.password2),
            errorMessage = "Passwords must match",
            predicate = { state ->
                state[Keys.password1] == state[Keys.password2]
            }
        )
        .rule(
            inputKeys = listOf(Keys.email),
            errorMessage = "Must be a valid email address",
            predicate = { state ->
                state[Keys.email].orEmpty().let {
                    it.isEmpty() || it.matches(Regex(".+@.+\\..+"))
                }
            }
        )
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        setContent {
            ComposeFormsTheme {
                Form.Render(rules) { formController ->
                    Column(modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())) {
                        FormItemWithError(Keys.firstName) { key, _ ->
                            InputField(
                                label = "First Name*",
                                onChange = { formController.onDataChange(key, it) }
                            )
                        }
                        FormItemWithError(Keys.lastName) { key, _ ->
                            InputField(
                                label = "Last Name*",
                                onChange = { formController.onDataChange(key, it) }
                            )
                        }
                        FormItem(Keys.password1) { key, errors ->
                            Column {
                                InputField(
                                    label = "Password*",
                                    onChange = { formController.onDataChange(key, it) }
                                )
                                errors.forEach {
                                    Text(color = Color.Blue, text = "Error: $it")
                                }
                            }
                        }
                        FormItemWithError(Keys.password2) { key, _ ->
                            InputField(
                                label = "Confirm Password*",
                                onChange = { formController.onDataChange(key, it) }
                            )
                        }
                        FormItemWithError(Keys.email) { key, _ ->
                            InputField(
                                label = "Email Address",
                                onChange = { formController.onDataChange(key, it) }
                            )
                        }
                        Text(modifier = Modifier.padding(16.dp), text = "* required")
                        Button(
                            modifier = Modifier.align(Alignment.End),
                            enabled = formController.errors.entries.all { it.value.isEmpty() },
                            onClick = { formController.validate() }) {
                            Text(text = "Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InputField(label: String, onChange: (String) -> Unit) {
    var value by remember { mutableStateOf("") }

    Column(Modifier.padding(top = 16.dp)) {
        Text(text = "$label:")
        TextField(value = value, onValueChange = {
            onChange(it)
            value = it
        })
    }
}