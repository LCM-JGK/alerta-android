package com.alertaturistica.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AccountScreen(
    state: AuthUiState,
    biometricAvailable: Boolean,
    onShow: (AuthScreen) -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onReset: (String, String, String) -> Unit,
    onRecoveryCodeSaved: () -> Unit,
    onEnableBiometric: () -> Unit,
    onDisableBiometric: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AccountHeader(state.user != null)
        state.message?.let { StatusMessage(it) }
        state.recoveryCodeToSave?.let { code ->
            RecoveryCodeCard(code, onRecoveryCodeSaved)
        }
        if (state.user != null) {
            SignedInContent(
                state = state,
                biometricAvailable = biometricAvailable,
                onEnableBiometric = onEnableBiometric,
                onDisableBiometric = onDisableBiometric,
                onLogout = onLogout,
            )
        } else {
            when (state.screen) {
                AuthScreen.SIGN_IN -> LoginForm(state.isLoading, onLogin, onShow)
                AuthScreen.REGISTER -> RegisterForm(state.isLoading, onRegister, onShow)
                AuthScreen.RESET_PASSWORD -> ResetForm(
                    loading = state.isLoading,
                    onReset = onReset,
                    onBack = { onShow(AuthScreen.SIGN_IN) },
                )
            }
        }
    }
}

@Composable
fun BiometricLockScreen(
    onUnlock: () -> Unit,
    onUsePassword: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.Fingerprint, contentDescription = null)
        Spacer(Modifier.height(16.dp))
        Text("Alerta Local está protegida", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Confirma tu huella o rostro registrado en Android para recuperar la sesión.",
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onUnlock, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Fingerprint, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Usar biometría")
        }
        TextButton(onClick = onUsePassword) { Text("Iniciar sesión con contraseña") }
    }
}

@Composable
fun SignInRequiredScreen(onOpenAccount: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null)
        Spacer(Modifier.height(14.dp))
        Text("Necesitas una cuenta verificada", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Puedes consultar el mapa sin registrarte. Para publicar avisos debes crear una cuenta e iniciar sesión.",
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(22.dp))
        Button(onClick = onOpenAccount, modifier = Modifier.fillMaxWidth()) { Text("Abrir mi cuenta") }
    }
}

@Composable
private fun RecoveryCodeCard(code: String, onSaved: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Guarda tu clave de recuperación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text("Se mostrará una sola vez. Guárdala en un gestor de contraseñas o anótala en un lugar privado. Sin ella no podremos recuperar tu cuenta.")
            SelectionContainer {
                Text(code, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Button(onClick = onSaved, modifier = Modifier.fillMaxWidth()) {
                Text("Ya guardé mi clave")
            }
        }
    }
}

@Composable
private fun AccountHeader(signedIn: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AccountCircle, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(if (signedIn) "Tu cuenta" else "Acceso seguro", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(if (signedIn) "Administra tu sesión y biometría" else "Crea una cuenta para publicar avisos")
            }
        }
    }
}

@Composable
private fun LoginForm(loading: Boolean, onLogin: (String, String) -> Unit, onShow: (AuthScreen) -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    Text("Iniciar sesión", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    UsernameField(username) { username = it }
    PasswordField(password, "Contraseña") { password = it }
    PrimaryAction("Ingresar", loading, username.isNotBlank() && password.isNotBlank()) { onLogin(username, password) }
    TextButton(onClick = { onShow(AuthScreen.RESET_PASSWORD) }, modifier = Modifier.fillMaxWidth()) {
        Text("Olvidé mi contraseña")
    }
    HorizontalDivider()
    OutlinedButton(onClick = { onShow(AuthScreen.REGISTER) }, modifier = Modifier.fillMaxWidth()) {
        Text("Crear una cuenta")
    }
}

@Composable
private fun RegisterForm(
    loading: Boolean,
    onRegister: (String, String, String) -> Unit,
    onShow: (AuthScreen) -> Unit,
) {
    var alias by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var accepted by rememberSaveable { mutableStateOf(false) }
    Text("Crear cuenta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    UsernameField(username) { username = it }
    OutlinedTextField(
        value = alias,
        onValueChange = { if (it.length <= 40) alias = it },
        label = { Text("Alias público") },
        supportingText = { Text("No uses tu nombre completo ni datos personales.") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    PasswordField(password, "Contraseña de 12 caracteres o más") { password = it }
    PasswordField(confirmation, "Confirmar contraseña") { confirmation = it }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Checkbox(checked = accepted, onCheckedChange = { accepted = it })
            Text(
                "Acepto que se almacenen mi usuario, alias y registros de seguridad para administrar la cuenta. La aplicación no recibe ni guarda mi rostro o huella.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    val valid = username.matches(Regex("[A-Za-z0-9_]{4,24}")) && alias.trim().length >= 2 && password.length >= 12 && password == confirmation && accepted
    PrimaryAction("Crear cuenta", loading, valid) { onRegister(username, alias, password) }
    TextButton(onClick = { onShow(AuthScreen.SIGN_IN) }, modifier = Modifier.fillMaxWidth()) {
        Text("Ya tengo una cuenta")
    }
}

@Composable
private fun ResetForm(
    loading: Boolean,
    onReset: (String, String, String) -> Unit,
    onBack: () -> Unit,
) {
    var username by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    Text("Nueva contraseña", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    Text("Introduce tu clave de recuperación guardada al crear la cuenta. Después recibirás una clave nueva.")
    UsernameField(username) { username = it }
    RecoveryCodeField(code) { code = it.uppercase().take(24) }
    PasswordField(password, "Nueva contraseña") { password = it }
    PasswordField(confirmation, "Confirmar contraseña") { confirmation = it }
    val validCode = code.filter(Char::isLetterOrDigit).length == 20
    PrimaryAction("Cambiar contraseña", loading, username.length >= 4 && validCode && password.length >= 12 && password == confirmation) {
        onReset(username, code, password)
    }
    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
}

@Composable
private fun SignedInContent(
    state: AuthUiState,
    biometricAvailable: Boolean,
    onEnableBiometric: () -> Unit,
    onDisableBiometric: () -> Unit,
    onLogout: () -> Unit,
) {
    val user = state.user ?: return
    Text(user.alias, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text("@${user.username}", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AccountCircle, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text("Cuenta autenticada")
        }
    }
    Text("Seguridad del dispositivo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    if (!biometricAvailable) {
        Text("Este dispositivo no tiene una huella o reconocimiento facial fuerte configurado. Puedes seguir usando tu contraseña.")
    } else if (state.biometricEnabled) {
        OutlinedButton(onClick = onDisableBiometric, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Fingerprint, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Desactivar acceso biométrico")
        }
    } else {
        Button(onClick = onEnableBiometric, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Fingerprint, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Activar huella o rostro")
        }
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.PrivacyTip, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text(
                "La plantilla biométrica permanece dentro del sistema Android. Alerta Local solo recibe el resultado de la comprobación.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.Logout, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Cerrar sesión")
    }
}

@Composable
private fun UsernameField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { character -> character.isLetterOrDigit() || character == '_' }.take(24)) },
        label = { Text("Nombre de usuario") },
        supportingText = { Text("De 4 a 24 caracteres: letras, números o guion bajo.") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    )
}

@Composable
private fun PasswordField(value: String, label: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 128) onChange(it) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    )
}

@Composable
private fun RecoveryCodeField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text("Clave de recuperación") },
        placeholder = { Text("XXXXX-XXXXX-XXXXX-XXXXX") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
    )
}

@Composable
private fun PrimaryAction(label: String, loading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled && !loading, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        if (loading) CircularProgressIndicator() else Text(label)
    }
}

@Composable
private fun StatusMessage(message: String) {
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.medium) {
        Text(message, modifier = Modifier.fillMaxWidth().padding(12.dp))
    }
}
