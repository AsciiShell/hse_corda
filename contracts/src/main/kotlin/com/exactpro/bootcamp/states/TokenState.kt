package com.exactpro.bootcamp.states

import com.exactpro.bootcamp.contracts.TokenContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

// *********
// * State *
// *********

// Our state, defining a shared fact on the ledger.
@BelongsToContract(TokenContract::class)
data class TokenState(
    val issuer: Party,
    val owner: Party,
    val amount: Double,
    val currencyType: CurrencyType
) : ContractState {
    override val participants: List<AbstractParty>
        get() = listOf(issuer, owner)
}

@CordaSerializable
enum class CurrencyType {
    RICK, MORTY
}
