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

    private object Keys {
        const val password1 = "password1"
        const val password2 = "password2"
    }

    private val rules = Validation.Builder()
        .isolatedRule(
            inputKeys = listOf(Keys.password1, Keys.password2),
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
            errorMessage = "Passwords must match.",
            predicate = { state ->
                state[Keys.password1] == state[Keys.password2]
            }
        )
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ComposeFormsTheme {
                Form.Render(rules) { formController ->
                    Column {
                        FormItem(Keys.password1) { key, errors ->
                            Column {
                                Password(
                                    label = "Password",
                                    onChange = { formController.onDataChange(key, it) }
                                )
                                errors.firstOrNull()?.also {
                                    Text(color = Color.Magenta, text = "Error: $it")
                                }
                            }
                        }
                        FormItemWithError(Keys.password2) { key, _ ->
                            Password(
                                label = "Confirm Password",
                                onChange = { formController.onDataChange(key, it) }
                            )
                        }
                        Button(
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