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
import androidx.compose.ui.unit.dp
import com.example.composeforms.ui.theme.ComposeFormsTheme

class MainActivity : ComponentActivity() {

    private object Keys {
        const val password1 = "password1"
        const val password2 = "password2"
    }

    private val rules = listOf(
        ValidationRule(
            inputKeys = listOf(
                Keys.password1,
            ),
            errorKeys = listOf(
                Keys.password1
            ),
            predicate = { state ->
                state[Keys.password1]?.let {
                    it.length >= 8
                } ?: false
            },
            errorMessage = "Must be at least 8 characters"
        ),
        ValidationRule(
            inputKeys = listOf(
                Keys.password2,
            ),
            errorKeys = listOf(
                Keys.password2
            ),
            predicate = { state ->
                state[Keys.password2]?.let {
                    it.length >= 8
                } ?: false
            },
            errorMessage = "Must be at least 8 characters"
        ),
        ValidationRule(
            inputKeys = listOf(
                Keys.password1,
                Keys.password2
            ),
            errorKeys = listOf(
                Keys.password2
            ),
            predicate = { state ->
                state[Keys.password1] == state[Keys.password2]
            },
            errorMessage = "Passwords must match."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Form.Render(rules) { formController ->
                val password1 = FormPassword("Password", Keys.password1, formController)
                val password2 = FormPassword("Confirm Password", Keys.password2, formController)

                ComposeFormsTheme {
                    Column {
                        password1.Compose()
                        password2.Compose()
                        Button(onClick = { formController.validate() }) {
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

class FormPassword(private val label: String, key: String, private val controller: Form.FormController) :
    FormComponent(key, controller.errors) {
    @Composable
    override fun GetComposable() {
        Password(
            label = label,
            onChange = { controller.onDataChange(key, it) }
        )
    }
}