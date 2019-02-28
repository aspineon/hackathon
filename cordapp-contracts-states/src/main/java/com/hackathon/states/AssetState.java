package com.hackathon.states;

import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

// *********
// * State *
// *********
public class AssetState implements LinearState {
    private final Party issuer;
    private final Party owner;
    private final String description;
    private final UniqueIdentifier linearId;

    // Autogenerates a unique identifier. Use when issuing the asset initially.
    public AssetState(Party issuer, Party owner, String description) {
        this.issuer = issuer;
        this.owner = owner;
        this.description = description;
        this.linearId = new UniqueIdentifier();
    }

    // Sets the unique identifier manually. Use when transferring an existing asset.
    @ConstructorForDeserialization
    public AssetState(Party issuer, Party owner, String description, UniqueIdentifier linearId) {
        this.issuer = issuer;
        this.owner = owner;
        this.description = description;
        this.linearId = linearId;
    }

    @NotNull
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
    public String getDescription() {
        return description;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }
}