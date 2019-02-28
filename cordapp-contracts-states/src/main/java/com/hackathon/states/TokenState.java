package com.hackathon.states;

import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.Arrays;
import java.util.List;

// *********
// * State *
// *********
public class TokenState implements ContractState {
    private final Party issuer;
    private final Party owner;
    private final int amount;

    public TokenState(Party issuer, Party owner, int amount) {
        this.issuer = issuer;
        this.owner = owner;
        this.amount = amount;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(owner);
    }

    public Party getIssuer() {
        return issuer;
    }
    public Party getOwner() {
        return owner;
    }
    public int getAmount() {
        return amount;
    }
}