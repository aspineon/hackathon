package com.hackathon.flows;

import com.google.common.collect.ImmutableList;
import com.hackathon.flows.exchangeassetfortoken.ExchangeAssetForTokenInitiator;
import com.hackathon.flows.exchangeassetfortoken.ExchangeAssetForTokenResponder;
import com.hackathon.states.AssetState;
import com.hackathon.states.TokenState;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.StartedMockNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;

public class FlowTests {
    private final MockNetwork network = new MockNetwork(ImmutableList.of("com.hackathon.contracts"));
    private final StartedMockNode a = network.createNode();
    private final StartedMockNode b = network.createNode();
    private final StartedMockNode c = network.createNode();

    public FlowTests() {
        b.registerInitiatedFlow(ExchangeAssetForTokenResponder.class);
    }

    @Before
    public void setup() {
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    // Issues a token.
    private void issueToken(StartedMockNode issuer, StartedMockNode recipient, int amount) throws Exception {
        IssueToken flow = new IssueToken(recipient.getInfo().getLegalIdentities().get(0), amount);
        CordaFuture<Void> future = issuer.startFlow(flow);
        network.runNetwork();
        future.get();
    }

    // Issues an asset.
    private UUID issueAsset(StartedMockNode issuer, StartedMockNode recipient, String description) throws Exception {
        IssueAsset flow = new IssueAsset(recipient.getInfo().getLegalIdentities().get(0), description);
        CordaFuture<UUID> future = issuer.startFlow(flow);
        network.runNetwork();
        return future.get();
    }

    // Transfers an asset.
    private void transferAsset(StartedMockNode initialAssetOwner, StartedMockNode initialTokenOwner, UUID assetID, int amount) throws Exception {
        ExchangeAssetForTokenInitiator flow = new ExchangeAssetForTokenInitiator(assetID, initialTokenOwner.getInfo().getLegalIdentities().get(0), amount);
        CordaFuture<Void> future = initialAssetOwner.startFlow(flow);
        network.runNetwork();
        future.get();
    }

    @Test
    public void tokenIssuanceRecordsTheCorrectTokenInRecipientsVaultOnly() throws Exception {
        int amount = 100;
        issueToken(a, b, amount);

        // We check nothing is recorded in the issuer's vault.
        a.transaction(() -> {
            List<StateAndRef<TokenState>> tokens = a.getServices().getVaultService().queryBy(TokenState.class).getStates();
            assertEquals(0, tokens.size());
            return null;
        });

        // We check the correct token is recorded in the recipient's vault.
        b.transaction(() -> {
            List<StateAndRef<TokenState>> tokens = b.getServices().getVaultService().queryBy(TokenState.class).getStates();
            assertEquals(1, tokens.size());
            TokenState token = tokens.get(0).getState().getData();
            assertEquals(token.getIssuer(), a.getInfo().getLegalIdentities().get(0));
            assertEquals(token.getOwner(), b.getInfo().getLegalIdentities().get(0));
            assertEquals(token.getAmount(), amount);
            return null;
        });
    }

    @Test
    public void assetIssuanceRecordsTheCorrectAssetInRecipientsVaultOnly() throws Exception {
        String description = "asset description";
        UUID assetID = issueAsset(a, b, description);

        // We check nothing is recorded in the issuer's vault.
        a.transaction(() -> {
            List<StateAndRef<AssetState>> assets = a.getServices().getVaultService().queryBy(AssetState.class).getStates();
            assertEquals(0, assets.size());
            return null;
        });

        // We check the correct token is recorded in the recipient's vault.
        b.transaction(() -> {
            List<StateAndRef<AssetState>> assets = b.getServices().getVaultService().queryBy(AssetState.class).getStates();
            assertEquals(1, assets.size());
            AssetState asset = assets.get(0).getState().getData();
            assertEquals(asset.getIssuer(), a.getInfo().getLegalIdentities().get(0));
            assertEquals(asset.getOwner(), b.getInfo().getLegalIdentities().get(0));
            assertEquals(asset.getDescription(), description);
            assertEquals(asset.getLinearId().getId(), assetID);
            return null;
        });
    }

    @Test
    public void partiesCanTradeAssetsForTokens() throws Exception {
        int amount = 100;
        issueToken(a, b, amount);

        String description = "asset description";
        UUID assetID = issueAsset(a, c, description);

        transferAsset(c, b, assetID, amount);

        // We check the correct asset is recorded in B's vault.
        b.transaction(() -> {
            List<StateAndRef<TokenState>> tokens = c.getServices().getVaultService().queryBy(TokenState.class).getStates();
            assertEquals(0, tokens.size());

            List<StateAndRef<AssetState>> assets = b.getServices().getVaultService().queryBy(AssetState.class).getStates();
            assertEquals(1, assets.size());
            AssetState asset = assets.get(0).getState().getData();
            assertEquals(asset.getIssuer(), a.getInfo().getLegalIdentities().get(0));
            assertEquals(asset.getOwner(), b.getInfo().getLegalIdentities().get(0));
            assertEquals(asset.getDescription(), description);
            assertEquals(asset.getLinearId().getId(), assetID);
            return null;
        });

        // We check the correct token is recorded in C's vault.
        c.transaction(() -> {
            List<StateAndRef<AssetState>> assets = c.getServices().getVaultService().queryBy(AssetState.class).getStates();
            assertEquals(0, assets.size());

            List<StateAndRef<TokenState>> tokens = c.getServices().getVaultService().queryBy(TokenState.class).getStates();
            assertEquals(1, tokens.size());
            TokenState token = tokens.get(0).getState().getData();
            assertEquals(token.getIssuer(), a.getInfo().getLegalIdentities().get(0));
            assertEquals(token.getOwner(), c.getInfo().getLegalIdentities().get(0));
            assertEquals(token.getAmount(), amount);
            return null;
        });
    }

    @Test
    public void partiesCannotTradeIfTheAssetDoesntExist() throws Exception {
        int amount = 100;
        issueToken(a, b, amount);

        String description = "asset description";
        UUID assetID = issueAsset(a, c, description);

        exception.expectCause(instanceOf(IllegalArgumentException.class));
        transferAsset(c, b, UUID.randomUUID(), amount);
    }

    @Test
    public void partiesCannotTradeIfATokenForCorrectAmountDoesntExist() throws Exception {
        int amount = 100;
        issueToken(a, b, amount);

        String description = "asset description";
        UUID assetID = issueAsset(a, c, description);

        exception.expectCause(instanceOf(FlowException.class));
        transferAsset(c, b, assetID, amount - 1);
    }
}
