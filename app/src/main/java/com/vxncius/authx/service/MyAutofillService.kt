package com.vxncius.authx.service
import android.app.assist.AssistStructure
import android.service.autofill.*
import android.util.Log
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.vxncius.authx.data.AppDatabase
import com.vxncius.authx.data.VaultItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
class MyAutofillService : AutofillService() {
    private val dbPassphrase = "default_passphrase_for_demo".toByteArray()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    override fun onFillRequest(request: FillRequest, cancellationSignal: android.os.CancellationSignal, callback: FillCallback) {
        val structure = request.fillContexts.last().structure
        val parser = AutofillStructureParser(structure)
        val packageName = structure.activityComponent.packageName
        Log.d("AuthXAutofill", "onFillRequest for package: $packageName")
        serviceScope.launch {
            try {
                if (cancellationSignal.isCanceled) return@launch
                val db = AppDatabase.getDatabase(this@MyAutofillService, dbPassphrase)
                val allItems = withTimeout(2000) { db.vaultDao().getAllItems().first() }
                val detectedType = parser.getDetectedType()
                val itemsByType = allItems.filter { it.type == detectedType }
                val items = filterItemsByContext(itemsByType, packageName, parser.webDomain)
                Log.d("AuthXAutofill", "Package: $packageName, Domain: ${parser.webDomain}, Type: $detectedType, showing ${items.size} items (from ${itemsByType.size} total type matches)")
                Log.d("AuthXAutofill", "Parser state: user=${parser.usernameId != null}, pass=${parser.passwordId != null}, card=${parser.cardNumberId != null}")
                val responseBuilder = FillResponse.Builder()
                if (parser.usernameId != null || parser.passwordId != null) {
                    val saveInfoBuilder = SaveInfo.Builder(
                        SaveInfo.SAVE_DATA_TYPE_PASSWORD or SaveInfo.SAVE_DATA_TYPE_USERNAME,
                        listOfNotNull(parser.usernameId, parser.passwordId).toTypedArray()
                    )
                    responseBuilder.setSaveInfo(saveInfoBuilder.build())
                    items.forEach { item ->
                        val datasetBuilder = Dataset.Builder()
                        val presentation = RemoteViews(this@MyAutofillService.packageName, android.R.layout.simple_list_item_1).apply {
                            setTextViewText(android.R.id.text1, "AuthX: ${item.title}")
                        }
                        if (parser.usernameId != null && item.username.isNotEmpty()) {
                            datasetBuilder.setValue(parser.usernameId!!, AutofillValue.forText(item.username), presentation)
                        }
                        if (parser.passwordId != null && item.password.isNotEmpty()) {
                            datasetBuilder.setValue(parser.passwordId!!, AutofillValue.forText(item.password), presentation)
                        }
                        try {
                            responseBuilder.addDataset(datasetBuilder.build())
                        } catch (e: Exception) {
                            Log.e("AuthXAutofill", "Error building dataset", e)
                        }
                    }
                } else {
                    Log.d("AuthXAutofill", "No username or password fields detected by parser")
                }
                val response = responseBuilder.build()
                Log.d("AuthXAutofill", "Returning FillResponse with ${items.size} potential items")
                callback.onSuccess(response)
            } catch (e: Exception) {
                Log.e("AuthXAutofill", "Fill request failed", e)
                callback.onSuccess(null)
            }
        }
    }
    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val contexts = request.fillContexts
        val structure = contexts.last().structure
        val parser = AutofillStructureParser(structure)
        serviceScope.launch {
            try {
                if (parser.usernameValue != null || parser.passwordValue != null) {
                    val db = AppDatabase.getDatabase(this@MyAutofillService, dbPassphrase)
                    val newItem = VaultItem(
                        title = structure.activityComponent.packageName.substringAfterLast("."),
                        username = parser.usernameValue ?: "",
                        password = parser.passwordValue ?: "",
                        websiteUrl = structure.activityComponent.packageName
                    )
                    db.vaultDao().insertItem(newItem)
                }
                callback.onSuccess()
            } catch (e: Exception) {
                Log.e("AuthXAutofill", "Save request failed", e)
                callback.onSuccess() 
            }
        }
    }
    private fun filterItemsByContext(items: List<VaultItem>, contextPackage: String, webDomain: String?): List<VaultItem> {
        if (items.isEmpty()) return emptyList()
        val mainContextPart = webDomain ?: extractMainPart(contextPackage)
        Log.d("AuthXAutofill", "Filtering items for context: $contextPackage, domain: $webDomain, main part: $mainContextPart")
        val filtered = items.filter { item ->
            val website = item.websiteUrl.lowercase()
            val title = item.title.lowercase()
            website.contains(contextPackage.lowercase()) || 
            contextPackage.lowercase().contains(website) ||
            (webDomain != null && website.contains(webDomain.lowercase())) ||
            website.contains(mainContextPart.lowercase()) ||
            title.contains(mainContextPart.lowercase())
        }
        return if (filtered.isNotEmpty()) filtered else items.take(8)
    }
    private fun extractMainPart(input: String): String {
        val parts = input.lowercase().split(".", "/", ":")
        val ignored = setOf("com", "android", "www", "https", "http", "org", "net", "app", "io")
        return parts.filter { it.length > 3 && it !in ignored }
            .sortedByDescending { it.length }
            .firstOrNull() ?: input
    }
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
    private class AutofillStructureParser(structure: AssistStructure) {
        var usernameId: AutofillId? = null
        var passwordId: AutofillId? = null
        var usernameValue: String? = null
        var passwordValue: String? = null
        var cardNumberId: AutofillId? = null
        var cardCvvId: AutofillId? = null
        var cardExpiryId: AutofillId? = null
        var addressId: AutofillId? = null
        var cityId: AutofillId? = null
        var postalCodeId: AutofillId? = null
        var webDomain: String? = null
        init {
            val nodes = structure.windowNodeCount
            for (i in 0 until nodes) {
                traverse(structure.getWindowNodeAt(i).rootViewNode)
            }
        }
        fun getDetectedType(): String {
            return when {
                cardNumberId != null || cardCvvId != null || cardExpiryId != null -> "CARD"
                addressId != null || cityId != null || postalCodeId != null -> "ADDRESS"
                usernameId != null || passwordId != null -> "LOGIN"
                else -> "LOGIN"
            }
        }
        private fun traverse(node: AssistStructure.ViewNode) {
            val hints = node.autofillHints
            if (hints != null || node.idEntry != null) {
                Log.v("AuthXAutofill", "Traversing node: id=${node.idEntry}, hints=${hints?.joinToString(",")}")
            }
            if (hints != null) {
                for (hint in hints) {
                    if (hint.contains("username", ignoreCase = true) || hint.contains("email", ignoreCase = true)) {
                        usernameId = node.autofillId
                        usernameValue = node.autofillValue?.textValue?.toString()
                    }
                    if (hint.contains("password", ignoreCase = true)) {
                        passwordId = node.autofillId
                        passwordValue = node.autofillValue?.textValue?.toString()
                    }
                    if (hint.contains("creditCard", ignoreCase = true) || hint.contains("cardNumber", ignoreCase = true)) {
                        cardNumberId = node.autofillId
                    }
                    if (hint.contains("cvv", ignoreCase = true) || hint.contains("cvc", ignoreCase = true) || hint.contains("securityCode", ignoreCase = true)) {
                        cardCvvId = node.autofillId
                    }
                    if (hint.contains("expiryDate", ignoreCase = true) || hint.contains("expiry", ignoreCase = true)) {
                        cardExpiryId = node.autofillId
                    }
                    if (hint.contains("postalAddress", ignoreCase = true) || hint.contains("streetAddress", ignoreCase = true)) {
                        addressId = node.autofillId
                    }
                    if (hint.contains("addressLocality", ignoreCase = true) || hint.contains("city", ignoreCase = true)) {
                        cityId = node.autofillId
                    }
                    if (hint.contains("postalCode", ignoreCase = true) || hint.contains("zipCode", ignoreCase = true)) {
                        postalCodeId = node.autofillId
                    }
                }
            }
            val nodeHint = node.hint?.toString()
            if (usernameId == null && nodeHint != null) {
                if (nodeHint.contains("user", ignoreCase = true) || nodeHint.contains("email", ignoreCase = true) || nodeHint.contains("login", ignoreCase = true)) {
                    usernameId = node.autofillId
                }
            }
            if (passwordId == null && nodeHint != null) {
                if (nodeHint.contains("pass", ignoreCase = true) || nodeHint.contains("pwd", ignoreCase = true)) {
                    passwordId = node.autofillId
                }
            }
            if (usernameId == null && (
                node.idEntry?.contains("user", ignoreCase = true) == true || 
                node.idEntry?.contains("email", ignoreCase = true) == true ||
                node.idEntry?.contains("login", ignoreCase = true) == true ||
                node.idEntry?.contains("account", ignoreCase = true) == true ||
                node.idEntry?.contains("identifier", ignoreCase = true) == true ||
                node.idEntry?.contains("phone", ignoreCase = true) == true
            )) {
                usernameId = node.autofillId
            }
            if (passwordId == null && (node.idEntry?.contains("pass", ignoreCase = true) == true || node.idEntry?.contains("pwd", ignoreCase = true) == true)) {
                passwordId = node.autofillId
            }
            if (usernameId == null && passwordId == null) {
                if (cardNumberId == null && node.idEntry?.contains("cardnumber", ignoreCase = true) == true) {
                    cardNumberId = node.autofillId
                }
                if (cardCvvId == null && (node.idEntry?.contains("cvv", ignoreCase = true) == true || node.idEntry?.contains("cvc", ignoreCase = true) == true)) {
                    cardCvvId = node.autofillId
                }
                if (addressId == null && (node.idEntry?.contains("address", ignoreCase = true) == true || node.idEntry?.contains("street", ignoreCase = true) == true)) {
                    addressId = node.autofillId
                }
                if (postalCodeId == null && (node.idEntry?.contains("postal", ignoreCase = true) == true || node.idEntry?.contains("zip", ignoreCase = true) == true)) {
                    postalCodeId = node.autofillId
                }
            }
            if (node.webDomain != null) {
                webDomain = node.webDomain
            }
            for (i in 0 until node.childCount) {
                traverse(node.getChildAt(i))
            }
        }
    }
}

