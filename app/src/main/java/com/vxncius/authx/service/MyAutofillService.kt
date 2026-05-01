package com.vxncius.authx.service

import android.app.assist.AssistStructure
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.text.InputType
import android.util.Log
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.vxncius.authx.data.AppDatabase
import com.vxncius.authx.data.VaultItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.security.SecureRandom

class MyAutofillService : AutofillService() {
    private val dbPassphrase = "default_passphrase_for_demo".toByteArray()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure ?: run {
            callback.onSuccess(null)
            return
        }
        val packageName = structure.activityComponent?.packageName.orEmpty()

        if (shouldIgnorePackage(packageName)) {
            callback.onSuccess(null)
            return
        }

        val parser = AutofillStructureParser(structure)
        val detectedType = parser.detectedType
        Log.d(TAG, "onFillRequest package=$packageName domain=${parser.webDomain} type=$detectedType confidence=${parser.confidence}")

        if (!parser.canFill()) {
            callback.onSuccess(null)
            return
        }

        serviceScope.launch {
            try {
                if (cancellationSignal.isCanceled) return@launch

                val db = AppDatabase.getDatabase(this@MyAutofillService, dbPassphrase)
                val allItems = withTimeoutOrNull(DATABASE_TIMEOUT_MS) {
                    db.vaultDao().getAllItems().first()
                }.orEmpty()

                val responseBuilder = FillResponse.Builder()
                var datasetCount = 0

                filterItemsByContext(allItems, detectedType, packageName, parser.webDomain)
                    .mapNotNull { buildDatasetForItem(parser, it) }
                    .forEach { dataset ->
                        responseBuilder.addDataset(dataset)
                        datasetCount++
                    }

                buildGeneratedPasswordDataset(parser)?.let { dataset ->
                    responseBuilder.addDataset(dataset)
                    datasetCount++
                }

                parser.buildSaveInfo()?.let(responseBuilder::setSaveInfo)

                if (cancellationSignal.isCanceled) return@launch
                callback.onSuccess(if (datasetCount > 0 || parser.canSave()) responseBuilder.build() else null)
            } catch (e: Exception) {
                Log.e(TAG, "Fill request failed", e)
                callback.onSuccess(null)
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val structure = request.fillContexts.lastOrNull()?.structure ?: run {
            callback.onSuccess()
            return
        }
        val packageName = structure.activityComponent?.packageName.orEmpty()
        val parser = AutofillStructureParser(structure)

        if (shouldIgnorePackage(packageName) || !parser.canSave()) {
            callback.onSuccess()
            return
        }

        serviceScope.launch {
            try {
                val newItem = buildItemFromParsedData(parser, packageName)
                if (newItem != null && !isDuplicate(newItem)) {
                    val db = AppDatabase.getDatabase(this@MyAutofillService, dbPassphrase)
                    db.vaultDao().insertItem(newItem)
                }
                callback.onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Save request failed", e)
                callback.onSuccess()
            }
        }
    }

    private suspend fun isDuplicate(candidate: VaultItem): Boolean {
        val db = AppDatabase.getDatabase(this, dbPassphrase)
        return withTimeoutOrNull(DATABASE_TIMEOUT_MS) {
            db.vaultDao().getAllItems().first().any { existing ->
                existing.type == candidate.type &&
                    existing.username.equals(candidate.username, ignoreCase = true) &&
                    existing.password == candidate.password &&
                    normalizeComparableContext(existing.websiteUrl) == normalizeComparableContext(candidate.websiteUrl)
            }
        } ?: false
    }

    private fun filterItemsByContext(
        items: List<VaultItem>,
        detectedType: AutofillDataType,
        contextPackage: String,
        webDomain: String?
    ): List<VaultItem> {
        if (items.isEmpty()) return emptyList()

        if (detectedType == AutofillDataType.CARD || detectedType == AutofillDataType.ADDRESS) {
            return items
                .asSequence()
                .filter { it.type == detectedType.storageType }
                .sortedBy { it.title.lowercase() }
                .take(MAX_DATASETS)
                .toList()
        }

        return items
            .asSequence()
            .filter { it.type == detectedType.storageType }
            .map { it to scoreItemForContext(it, contextPackage, webDomain) }
            .filter { (_, score) -> score >= MIN_CONTEXT_SCORE }
            .sortedByDescending { (_, score) -> score }
            .map { (item, _) -> item }
            .take(MAX_DATASETS)
            .toList()
    }

    private fun scoreItemForContext(item: VaultItem, contextPackage: String, webDomain: String?): Int {
        val packageLower = contextPackage.lowercase()
        val packageTokens = tokenizeContext(contextPackage)
        val domainLower = webDomain?.lowercase().orEmpty()
        val itemHost = extractHost(item.websiteUrl)
        val itemText = listOf(item.websiteUrl, item.title, item.username)
            .joinToString(" ")
            .lowercase()

        var score = 0
        if (itemHost.isNotBlank()) {
            if (itemHost == domainLower || itemHost.endsWith(".$domainLower")) score += 120
            if (domainLower.isNotBlank() && domainLower.endsWith(".$itemHost")) score += 120
            if (packageLower.contains(itemHost) || itemHost.contains(packageLower)) score += 80
        }
        if (domainLower.isNotBlank() && itemText.contains(domainLower)) score += 90

        packageTokens.forEach { token ->
            if (itemText.contains(token)) score += 25
        }
        webDomain?.let { domain ->
            tokenizeContext(domain).forEach { token ->
                if (itemText.contains(token)) score += 35
            }
        }

        return score
    }

    private fun buildDatasetForItem(parser: AutofillStructureParser, item: VaultItem): Dataset? {
        val datasetBuilder = Dataset.Builder()
        val presentation = buildPresentation("AuthX: ${item.title}")
        var values = 0

        fun setText(id: AutofillId?, value: String) {
            if (id != null && value.isNotBlank()) {
                datasetBuilder.setValue(id, AutofillValue.forText(value), presentation)
                values++
            }
        }

        when (parser.detectedType) {
            AutofillDataType.CARD -> {
                val metadata = parseCardMetadata(item.websiteUrl)
                setText(parser.cardHolderId, item.username)
                setText(parser.cardNumberId, item.password)
                setText(parser.cardExpiryId, metadata.expiry)
                setText(parser.cardCvvId, metadata.cvv)
            }
            AutofillDataType.ADDRESS -> {
                val metadata = parseAddressMetadata(item)
                setText(parser.addressId, metadata.streetAddress)
                setText(parser.addressLine2Id, metadata.addressLine2)
                setText(parser.cityId, metadata.city)
                setText(parser.stateId, metadata.state)
                setText(parser.postalCodeId, metadata.postalCode)
                setText(parser.recipientId, item.title)
            }
            AutofillDataType.LOGIN -> {
                setText(parser.usernameId, item.username)
                setText(parser.passwordId, item.password)
            }
            AutofillDataType.UNKNOWN -> Unit
        }

        return if (values > 0) {
            runCatching { datasetBuilder.build() }
                .onFailure { Log.e(TAG, "Error building dataset", it) }
                .getOrNull()
        } else {
            null
        }
    }

    private fun buildGeneratedPasswordDataset(parser: AutofillStructureParser): Dataset? {
        val passwordId = parser.passwordId ?: return null
        if (parser.detectedType != AutofillDataType.LOGIN || parser.passwordValue.orEmpty().isNotBlank()) return null

        return runCatching {
            Dataset.Builder()
                .setValue(
                    passwordId,
                    AutofillValue.forText(generatePassword()),
                    buildPresentation("AuthX: gerar senha forte")
                )
                .build()
        }.getOrNull()
    }

    private fun buildItemFromParsedData(parser: AutofillStructureParser, packageName: String): VaultItem? {
        val contextName = parser.webDomain ?: packageName
        val title = extractMainPart(contextName).replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }

        return when (parser.detectedType) {
            AutofillDataType.CARD -> {
                val cardNumber = parser.cardNumberValue.orEmpty().filter { it.isDigit() }
                if (cardNumber.length < MIN_CARD_DIGITS) return null
                VaultItem(
                    title = title,
                    username = parser.cardHolderValue.orEmpty().trim(),
                    password = parser.cardNumberValue.orEmpty().trim(),
                    websiteUrl = buildCardMetadata(
                        parser.cardExpiryValue.orEmpty().trim(),
                        parser.cardCvvValue.orEmpty().trim()
                    ),
                    type = AutofillDataType.CARD.storageType
                )
            }
            AutofillDataType.ADDRESS -> {
                val street = parser.addressValue.orEmpty().trim()
                val line2 = parser.addressLine2Value.orEmpty().trim()
                val city = parser.cityValue.orEmpty().trim()
                val state = parser.stateValue.orEmpty().trim()
                val zip = parser.postalCodeValue.orEmpty().trim()
                val fullAddress = formatAddressForStorage(street, line2, city, state, zip)

                if (fullAddress.isBlank()) return null
                VaultItem(
                    title = parser.recipientValue.orEmpty().trim().ifBlank { title },
                    username = fullAddress,
                    password = "",
                    websiteUrl = buildAddressMapUrl(street, line2, city, state, zip),
                    type = AutofillDataType.ADDRESS.storageType
                )
            }
            AutofillDataType.LOGIN -> {
                val username = parser.usernameValue.orEmpty().trim()
                val password = parser.passwordValue.orEmpty()
                if (password.isBlank()) return null
                VaultItem(
                    title = title,
                    username = username,
                    password = password,
                    websiteUrl = normalizeWebsiteUrl(contextName),
                    type = AutofillDataType.LOGIN.storageType
                )
            }
            AutofillDataType.UNKNOWN -> null
        }
    }

    private fun buildPresentation(text: String): RemoteViews {
        return RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
            setTextViewText(android.R.id.text1, text)
        }
    }

    private fun shouldIgnorePackage(packageName: String): Boolean {
        return packageName.isBlank() || packageName in IGNORED_PACKAGES || packageName == this.packageName
    }

    private fun extractMainPart(input: String): String {
        return tokenizeContext(input).maxByOrNull { it.length } ?: input
    }

    private fun tokenizeContext(input: String): List<String> {
        val ignored = setOf("com", "android", "www", "https", "http", "org", "net", "app", "io", "br", "co")
        return input.lowercase()
            .split(".", "/", ":", "-", "_", "?", "&", "=")
            .map { it.trim() }
            .filter { it.length > 2 && it !in ignored }
    }

    private fun normalizeWebsiteUrl(input: String): String {
        val cleaned = input.trim()
        return when {
            cleaned.startsWith("http://", ignoreCase = true) || cleaned.startsWith("https://", ignoreCase = true) -> cleaned
            cleaned.contains(".") -> "https://$cleaned"
            else -> cleaned
        }
    }

    private fun normalizeComparableContext(input: String): String {
        return extractHost(input).ifBlank { input.trim().lowercase() }
    }

    private fun extractHost(input: String): String {
        val cleaned = input.trim().lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore("/")
            .substringBefore(":")
            .removePrefix("www.")
        return cleaned.takeIf { it.contains(".") }.orEmpty()
    }

    private fun buildCardMetadata(expiry: String, cvv: String): String {
        val normalizedExpiry = expiry.ifBlank { "-" }
        val normalizedCvv = cvv.ifBlank { "-" }
        return "Val: $normalizedExpiry | CVV: $normalizedCvv"
    }

    private fun parseCardMetadata(value: String): CardMetadata {
        val expiry = Regex("""Val:\s*([^|]+)""").find(value)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        val cvv = Regex("""CVV:\s*(.+)$""").find(value)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        return CardMetadata(
            expiry = if (expiry == "-") "" else expiry,
            cvv = if (cvv == "-") "" else cvv
        )
    }

    private fun buildAddressMapUrl(street: String, line2: String, city: String, state: String, zip: String): String {
        val query = listOf(street, line2, city, state, zip)
            .filter { it.isNotBlank() }
            .joinToString(", ")
            .replace(" ", "+")
        return if (query.isBlank()) "" else "https://maps.google.com/?q=$query"
    }

    private fun formatAddressForStorage(street: String, line2: String, city: String, state: String, zip: String): String {
        val firstLine = listOf(street, line2).filter { it.isNotBlank() }.joinToString(", ")
        val secondLine = buildString {
            if (city.isNotBlank()) append(city)
            if (state.isNotBlank()) {
                if (isNotBlank()) append("/")
                append(state)
            }
        }
        return listOf(firstLine, secondLine, zip)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
    }

    private fun parseAddressMetadata(item: VaultItem): AddressMetadata {
        val raw = item.username.trim()
        val parts = raw.split("|").map { it.trim() }.filter { it.isNotBlank() }
        if (parts.size >= 5) {
            return AddressMetadata(
                streetAddress = parts.getOrNull(0).orEmpty(),
                addressLine2 = parts.getOrNull(1).orEmpty(),
                city = parts.getOrNull(2).orEmpty(),
                state = parts.getOrNull(3).orEmpty(),
                postalCode = parts.getOrNull(4).orEmpty()
            )
        }

        val blocks = raw.split(" - ").map { it.trim() }
        val firstLineParts = blocks.getOrNull(0).orEmpty().split(",").map { it.trim() }.filter { it.isNotBlank() }
        val cityState = blocks.getOrNull(1).orEmpty()
        val city = cityState.substringBefore("/", "").trim()
        val state = cityState.substringAfter("/", "").takeIf { it != cityState }?.trim().orEmpty()
        return AddressMetadata(
            streetAddress = firstLineParts.getOrNull(0).orEmpty(),
            addressLine2 = firstLineParts.drop(1).joinToString(", "),
            city = city,
            state = state,
            postalCode = blocks.getOrNull(2).orEmpty()
        )
    }

    private fun generatePassword(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%&*_-+="
        val random = SecureRandom()
        return buildString {
            repeat(GENERATED_PASSWORD_LENGTH) {
                append(chars[random.nextInt(chars.length)])
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private enum class AutofillDataType(val storageType: String) {
        LOGIN("LOGIN"),
        CARD("CARD"),
        ADDRESS("ADDRESS"),
        UNKNOWN("UNKNOWN")
    }

    private data class CardMetadata(val expiry: String, val cvv: String)

    private data class AddressMetadata(
        val streetAddress: String,
        val addressLine2: String,
        val city: String,
        val state: String,
        val postalCode: String
    )

    private class AutofillStructureParser(structure: AssistStructure) {
        var usernameId: AutofillId? = null
        var passwordId: AutofillId? = null
        var usernameValue: String? = null
        var passwordValue: String? = null
        var cardHolderId: AutofillId? = null
        var cardHolderValue: String? = null
        var cardNumberId: AutofillId? = null
        var cardNumberValue: String? = null
        var cardCvvId: AutofillId? = null
        var cardCvvValue: String? = null
        var cardExpiryId: AutofillId? = null
        var cardExpiryValue: String? = null
        var addressId: AutofillId? = null
        var addressValue: String? = null
        var addressLine2Id: AutofillId? = null
        var addressLine2Value: String? = null
        var cityId: AutofillId? = null
        var cityValue: String? = null
        var stateId: AutofillId? = null
        var stateValue: String? = null
        var postalCodeId: AutofillId? = null
        var postalCodeValue: String? = null
        var recipientId: AutofillId? = null
        var recipientValue: String? = null
        var webDomain: String? = null
        var confidence: Int = 0
            private set

        val detectedType: AutofillDataType
            get() = when {
                cardNumberId != null || cardCvvId != null || cardExpiryId != null -> AutofillDataType.CARD
                addressId != null && (cityId != null || postalCodeId != null || stateId != null) -> AutofillDataType.ADDRESS
                passwordId != null || (usernameId != null && confidence >= LOGIN_CONFIDENCE_THRESHOLD) -> AutofillDataType.LOGIN
                else -> AutofillDataType.UNKNOWN
            }

        init {
            for (i in 0 until structure.windowNodeCount) {
                traverse(structure.getWindowNodeAt(i).rootViewNode)
            }
        }

        fun canFill(): Boolean {
            return when (detectedType) {
                AutofillDataType.LOGIN -> passwordId != null || usernameId != null
                AutofillDataType.CARD -> cardNumberId != null || cardExpiryId != null || cardCvvId != null
                AutofillDataType.ADDRESS -> addressId != null || cityId != null || postalCodeId != null
                AutofillDataType.UNKNOWN -> false
            }
        }

        fun canSave(): Boolean {
            return when (detectedType) {
                AutofillDataType.LOGIN -> passwordId != null && passwordValue.orEmpty().isNotBlank()
                AutofillDataType.CARD -> cardNumberId != null && cardNumberValue.orEmpty().filter { it.isDigit() }.length >= MIN_CARD_DIGITS
                AutofillDataType.ADDRESS -> listOf(addressValue, cityValue, stateValue, postalCodeValue).any { it.orEmpty().isNotBlank() }
                AutofillDataType.UNKNOWN -> false
            }
        }

        fun buildSaveInfo(): SaveInfo? {
            if (!canSavePrompt()) return null
            val ids = when (detectedType) {
                AutofillDataType.CARD -> listOfNotNull(cardHolderId, cardNumberId, cardExpiryId, cardCvvId)
                AutofillDataType.ADDRESS -> listOfNotNull(addressId, addressLine2Id, cityId, stateId, postalCodeId, recipientId)
                AutofillDataType.LOGIN -> listOfNotNull(usernameId, passwordId)
                AutofillDataType.UNKNOWN -> emptyList()
            }
            if (ids.isEmpty()) return null

            val type = when (detectedType) {
                AutofillDataType.CARD -> SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD
                AutofillDataType.ADDRESS -> SaveInfo.SAVE_DATA_TYPE_ADDRESS
                AutofillDataType.LOGIN -> SaveInfo.SAVE_DATA_TYPE_PASSWORD or SaveInfo.SAVE_DATA_TYPE_USERNAME
                AutofillDataType.UNKNOWN -> return null
            }
            return SaveInfo.Builder(type, ids.toTypedArray()).build()
        }

        private fun canSavePrompt(): Boolean {
            return when (detectedType) {
                AutofillDataType.LOGIN -> passwordId != null
                AutofillDataType.CARD -> cardNumberId != null
                AutofillDataType.ADDRESS -> addressId != null || postalCodeId != null
                AutofillDataType.UNKNOWN -> false
            }
        }

        private fun traverse(node: AssistStructure.ViewNode) {
            val hints = node.autofillHints.orEmpty()
            val nodeHint = node.hint?.toString().orEmpty()
            val idEntry = node.idEntry.orEmpty()
            val htmlName = node.htmlInfo?.attributes?.firstOrNull { it.first.equals("name", true) }?.second.orEmpty()
            val text = listOf(idEntry, nodeHint, htmlName, hints.joinToString(" ")).joinToString(" ")
            val value = node.autofillValue?.takeIf { it.isText }?.textValue?.toString().orEmpty()

            node.webDomain?.takeIf { it.isNotBlank() }?.let { webDomain = it }

            when {
                hints.any { it.contains("password", true) } || looksLikePassword(text) || isPasswordInput(node) -> assignPassword(node.autofillId, value)
                hints.any { it.contains("username", true) || it.contains("email", true) } || looksLikeLoginUser(text) -> assignLoginUser(node.autofillId, value)
                hints.any { it.contains("creditCardNumber", true) || it.contains("cardNumber", true) } || looksLikeCardNumber(text) -> assignCardNumber(node.autofillId, value)
                hints.any { it.contains("creditCardSecurityCode", true) || it.contains("securityCode", true) } || looksLikeCardCvv(text) -> assignCardCvv(node.autofillId, value)
                hints.any { it.contains("creditCardExpiration", true) || it.contains("expiry", true) } || looksLikeCardExpiry(text) -> assignCardExpiry(node.autofillId, value)
                hints.any { it.contains("creditCardName", true) || it.contains("nameOnCard", true) } || looksLikeCardHolder(text) -> assignCardHolder(node.autofillId, value)
                hints.any { it.contains("postalAddress", true) || it.contains("streetAddress", true) } || looksLikeAddress(text) -> assignAddress(node.autofillId, value)
                hints.any { it.contains("extendedAddress", true) || it.contains("addressLine2", true) } || looksLikeAddressLine2(text) -> assignAddressLine2(node.autofillId, value)
                hints.any { it.contains("addressLocality", true) || it.contains("city", true) } || looksLikeCity(text) -> assignCity(node.autofillId, value)
                hints.any { it.contains("addressRegion", true) || it.contains("state", true) } || looksLikeState(text) -> assignState(node.autofillId, value)
                hints.any { it.contains("postalCode", true) || it.contains("zip", true) } || looksLikePostalCode(text) -> assignPostalCode(node.autofillId, value)
                hints.any { it.contains("personName", true) || it.contains("fullName", true) } || looksLikeRecipient(text) -> assignRecipient(node.autofillId, value)
            }

            for (i in 0 until node.childCount) {
                traverse(node.getChildAt(i))
            }
        }

        private fun assignLoginUser(id: AutofillId?, value: String) {
            if (id == null) return
            if (usernameId == null) usernameId = id
            if (value.isNotBlank()) usernameValue = value
            confidence += 12
        }

        private fun assignPassword(id: AutofillId?, value: String) {
            if (id == null) return
            if (passwordId == null) passwordId = id
            if (value.isNotBlank()) passwordValue = value
            confidence += 40
        }

        private fun assignCardHolder(id: AutofillId?, value: String) {
            if (id == null) return
            if (cardHolderId == null) cardHolderId = id
            if (value.isNotBlank()) cardHolderValue = value
            confidence += 10
        }

        private fun assignCardNumber(id: AutofillId?, value: String) {
            if (id == null) return
            if (cardNumberId == null) cardNumberId = id
            if (value.isNotBlank()) cardNumberValue = value
            confidence += 45
        }

        private fun assignCardCvv(id: AutofillId?, value: String) {
            if (id == null) return
            if (cardCvvId == null) cardCvvId = id
            if (value.isNotBlank()) cardCvvValue = value
            confidence += 25
        }

        private fun assignCardExpiry(id: AutofillId?, value: String) {
            if (id == null) return
            if (cardExpiryId == null) cardExpiryId = id
            if (value.isNotBlank()) cardExpiryValue = value
            confidence += 25
        }

        private fun assignAddress(id: AutofillId?, value: String) {
            if (id == null) return
            if (addressId == null) addressId = id
            if (value.isNotBlank()) addressValue = value
            confidence += 25
        }

        private fun assignAddressLine2(id: AutofillId?, value: String) {
            if (id == null) return
            if (addressLine2Id == null) addressLine2Id = id
            if (value.isNotBlank()) addressLine2Value = value
            confidence += 10
        }

        private fun assignCity(id: AutofillId?, value: String) {
            if (id == null) return
            if (cityId == null) cityId = id
            if (value.isNotBlank()) cityValue = value
            confidence += 15
        }

        private fun assignState(id: AutofillId?, value: String) {
            if (id == null) return
            if (stateId == null) stateId = id
            if (value.isNotBlank()) stateValue = value
            confidence += 15
        }

        private fun assignPostalCode(id: AutofillId?, value: String) {
            if (id == null) return
            if (postalCodeId == null) postalCodeId = id
            if (value.isNotBlank()) postalCodeValue = value
            confidence += 15
        }

        private fun assignRecipient(id: AutofillId?, value: String) {
            if (id == null) return
            if (recipientId == null) recipientId = id
            if (value.isNotBlank()) recipientValue = value
            confidence += 5
        }

        private fun looksLikeLoginUser(text: String) =
            containsAny(text, "user", "email", "login", "account", "identifier", "telefone", "phone")

        private fun looksLikePassword(text: String) =
            containsAny(text, "password", "passwd", "pass", "pwd", "senha")

        private fun looksLikeCardHolder(text: String) =
            containsAny(text, "cardholder", "holder", "nameoncard", "card_name", "nome_cartao")

        private fun looksLikeCardNumber(text: String) =
            containsAny(text, "cardnumber", "cc-number", "creditcard", "numero_cartao", "card_number")

        private fun looksLikeCardExpiry(text: String) =
            containsAny(text, "expiry", "expdate", "expiration", "validade")

        private fun looksLikeCardCvv(text: String) =
            containsAny(text, "cvv", "cvc", "securitycode", "security_code")

        private fun looksLikeAddress(text: String) =
            containsAny(text, "address", "street", "logradouro", "endereco", "endereço")

        private fun looksLikeAddressLine2(text: String) =
            containsAny(text, "address2", "line2", "complement", "complemento", "bairro", "neighborhood")

        private fun looksLikeCity(text: String) =
            containsAny(text, "city", "cidade", "locality")

        private fun looksLikeState(text: String) =
            containsAny(text, "state", "province", "region", "uf", "estado")

        private fun looksLikePostalCode(text: String) =
            containsAny(text, "postal", "zip", "cep")

        private fun looksLikeRecipient(text: String) =
            containsAny(text, "fullname", "recipient", "destinatario", "destinatário", "nome")

        private fun containsAny(text: String, vararg terms: String): Boolean {
            val lower = text.lowercase()
            return terms.any { lower.contains(it) }
        }

        private fun isPasswordInput(node: AssistStructure.ViewNode): Boolean {
            val inputType = node.inputType
            val variation = inputType and InputType.TYPE_MASK_VARIATION
            return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
    }

    private companion object {
        private const val TAG = "AuthXAutofill"
        private const val DATABASE_TIMEOUT_MS = 2_000L
        private const val MAX_DATASETS = 6
        private const val MIN_CONTEXT_SCORE = 35
        private const val LOGIN_CONFIDENCE_THRESHOLD = 24
        private const val MIN_CARD_DIGITS = 12
        private const val GENERATED_PASSWORD_LENGTH = 18

        private val IGNORED_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.google.android.inputmethod.latin",
            "com.samsung.android.honeyboard"
        )
    }
}
