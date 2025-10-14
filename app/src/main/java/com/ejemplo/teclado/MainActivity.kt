// MainActivity.kt
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences

class MainActivity : AppCompatActivity() {
    private lateinit var displayText: EditText
    private lateinit var customButtonsLayout: LinearLayout
    private val customButtons = mutableMapOf<String, ButtonConfig>()
    private val buttonIds = listOf("F1", "F2", "F3", "F4", "F5")
    private lateinit var prefs: SharedPreferences

    data class ButtonConfig(
        val label: String,
        val key: String,
        val useShift: Boolean = false,
        val useAlt: Boolean = false,
        val useCtrl: Boolean = false
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        displayText = findViewById(R.id.displayText)
        customButtonsLayout = findViewById(R.id.customButtonsLayout)
        prefs = getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE)

        loadCustomButtons()
        setupCustomButtons()
        setupNumericKeyboard()
    }

    private fun loadCustomButtons() {
        val defaultKeys = mapOf(
            "F1" to "a",
            "F2" to "b",
            "F3" to "c",
            "F4" to "Enter",
            "F5" to "Delete"
        )

        buttonIds.forEach { buttonId ->
            val savedKey = prefs.getString("${buttonId}_key", defaultKeys[buttonId] ?: "")
            val shift = prefs.getBoolean("${buttonId}_shift", false)
            val alt = prefs.getBoolean("${buttonId}_alt", false)
            val ctrl = prefs.getBoolean("${buttonId}_ctrl", false)
            
            customButtons[buttonId] = ButtonConfig(
                label = buttonId,
                key = savedKey ?: "",
                useShift = shift,
                useAlt = alt,
                useCtrl = ctrl
            )
        }
    }

    private fun setupCustomButtons() {
        customButtonsLayout.removeAllViews()
        
        buttonIds.forEach { buttonId ->
            val config = customButtons[buttonId] ?: ButtonConfig(buttonId, "")
            val btn = Button(this).apply {
                text = "${config.label}\n${config.key}"
                textSize = 10f
                layoutParams = LinearLayout.LayoutParams(0, 100, 1f).apply {
                    setMargins(4, 4, 4, 4)
                }
                setOnClickListener { showCustomButtonDialog(buttonId) }
                setOnLongClickListener {
                    insertKey(config)
                    true
                }
            }
            customButtonsLayout.addView(btn)
        }
    }

    private fun showCustomButtonDialog(buttonId: String) {
        val config = customButtons[buttonId] ?: ButtonConfig(buttonId, "")
        
        val dialogView = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        // Spinner para seleccionar tipo de tecla
        val keyTypeSpinner = Spinner(this)
        val keyTypes = arrayOf(
            "Texto",
            "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12",
            "Enter", "Delete", "Backspace", "Tab", "Escape", "Space",
            "ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight",
            "Home", "End", "PageUp", "PageDown", "Insert"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, keyTypes)
        keyTypeSpinner.adapter = adapter
        
        val selectedIndex = keyTypes.indexOf(config.key).let { if (it >= 0) it else 0 }
        keyTypeSpinner.setSelection(selectedIndex)

        // EditText para texto personalizado
        val textInput = EditText(this).apply {
            hint = "Texto personalizado"
            setText(if (config.key !in keyTypes.drop(1)) config.key else "")
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8, 0, 8) }
        }

        // Checkboxes para modificadores
        val shiftCheckBox = CheckBox(this).apply {
            text = "Shift"
            isChecked = config.useShift
        }
        val altCheckBox = CheckBox(this).apply {
            text = "Alt"
            isChecked = config.useAlt
        }
        val ctrlCheckBox = CheckBox(this).apply {
            text = "Ctrl"
            isChecked = config.useCtrl
        }

        dialogView.addView(TextView(this).apply {
            text = "Tipo de Tecla:"
            textSize = 14f
            setPadding(0, 0, 0, 8)
        })
        dialogView.addView(keyTypeSpinner)
        dialogView.addView(TextView(this).apply {
            text = "O Texto Personalizado:"
            textSize = 14f
            setPadding(0, 16, 0, 8)
        })
        dialogView.addView(textInput)
        dialogView.addView(TextView(this).apply {
            text = "Modificadores:"
            textSize = 14f
            setPadding(0, 16, 0, 8)
        })
        dialogView.addView(shiftCheckBox)
        dialogView.addView(altCheckBox)
        dialogView.addView(ctrlCheckBox)

        AlertDialog.Builder(this)
            .setTitle("Configurar $buttonId")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val selectedKey = if (keyTypeSpinner.selectedItemPosition == 0) {
                    textInput.text.toString().ifEmpty { buttonId }
                } else {
                    keyTypeSpinner.selectedItem.toString()
                }

                val newConfig = ButtonConfig(
                    label = buttonId,
                    key = selectedKey,
                    useShift = shiftCheckBox.isChecked,
                    useAlt = altCheckBox.isChecked,
                    useCtrl = ctrlCheckBox.isChecked
                )

                customButtons[buttonId] = newConfig

                // Guardar en SharedPreferences
                prefs.edit().apply {
                    putString("${buttonId}_key", selectedKey)
                    putBoolean("${buttonId}_shift", shiftCheckBox.isChecked)
                    putBoolean("${buttonId}_alt", altCheckBox.isChecked)
                    putBoolean("${buttonId}_ctrl", ctrlCheckBox.isChecked)
                    apply()
                }

                setupCustomButtons()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun insertKey(config: ButtonConfig) {
        val modifier = buildString {
            if (config.useCtrl) append("[Ctrl] ")
            if (config.useAlt) append("[Alt] ")
            if (config.useShift) append("[Shift] ")
        }
        
        displayText.append("$modifier${config.key}")
    }

    private fun setupNumericKeyboard() {
        val keyboardLayout = findViewById<LinearLayout>(R.id.keyboardLayout)
        
        val keyValues = arrayOf(
            arrayOf("1", "2", "3"),
            arrayOf("4", "5", "6"),
            arrayOf("7", "8", "9"),
            arrayOf("*", "0", "#"),
            arrayOf("DEL", "CLR", "ENTER")
        )

        keyValues.forEach { row ->
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    100
                ).apply {
                    setMargins(4, 4, 4, 4)
                }
            }

            row.forEach { keyValue ->
                val btn = Button(this).apply {
                    text = keyValue
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                        setMargins(2, 2, 2, 2)
                    }
                    setOnClickListener { onKeyPressed(keyValue) }
                }
                rowLayout.addView(btn)
            }
            keyboardLayout.addView(rowLayout)
        }
    }

    private fun onKeyPressed(key: String) {
        when (key) {
            "DEL" -> {
                val text = displayText.text.toString()
                if (text.isNotEmpty()) {
                    displayText.setText(text.substring(0, text.length - 1))
                }
            }
            "CLR" -> displayText.setText("")
            "ENTER" -> {
                // Procesar el input
            }
            else -> displayText.append(key)
        }
    }
}

/* activity_main.xml */
&lt;?xml version="1.0" encoding="utf-8"?&gt;
&lt;LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="8dp"&gt;

    &lt;EditText
        android:id="@+id/displayText"
        android:layout_width="match_parent"
        android:layout_height="80dp"
        android:textSize="18sp"
        android:inputType="text"
        android:gravity="center|right"
        android:background="@android:drawable/edit_text"
        android:padding="12dp"
        android:hint="Display" /&gt;

    &lt;LinearLayout
        android:id="@+id/customButtonsLayout"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:layout_marginTop="8dp" /&gt;

    &lt;ScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"&gt;
        &lt;LinearLayout
            android:id="@+id/keyboardLayout"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical" /&gt;
    &lt;/ScrollView&gt;

&lt;/LinearLayout&gt;
