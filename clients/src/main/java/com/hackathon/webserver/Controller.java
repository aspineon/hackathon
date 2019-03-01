package com.hackathon.webserver;

import com.hackathon.flows.IssueAsset;
import com.hackathon.flows.IssueToken;
import com.hackathon.flows.exchangeassetfortoken.ExchangeAssetForTokenInitiator;
import com.hackathon.states.AssetState;
import com.hackathon.states.TokenState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
public class Controller {
    private final CordaRPCOps proxy;
    private final static Logger logger = LoggerFactory.getLogger(Controller.class);

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
    }

    @PostMapping(path = "issue-token", produces = "text/plain")
    private ResponseEntity<String> issueToken(HttpServletRequest request) throws ExecutionException, InterruptedException {
        String recipientName = request.getParameter("recipient");
        Integer amount = Integer.valueOf(request.getParameter("amount"));

        Party recipient = proxy.partiesFromName(recipientName, true).iterator().next();

        proxy.startFlowDynamic(IssueToken.class, recipient, amount).getReturnValue().get();
        return ResponseEntity.status(HttpStatus.CREATED).body("Token issued.\n");
    }

    @PostMapping(path = "issue-asset", produces = "text/plain")
    private ResponseEntity<String> issueAsset(HttpServletRequest request) throws ExecutionException, InterruptedException {
        String recipientName = request.getParameter("recipient");
        String description = request.getParameter("description");

        Party recipient = proxy.partiesFromName(recipientName, true).iterator().next();

        proxy.startFlowDynamic(IssueAsset.class, recipient, description).getReturnValue().get();
        return ResponseEntity.status(HttpStatus.CREATED).body("Asset issued.\n");
    }

    @PostMapping(path = "exchange-asset-for-token", produces = "text/plain")
    private ResponseEntity<String> exchangeAssetForToken(HttpServletRequest request) throws ExecutionException, InterruptedException {
        String assetIdString = request.getParameter("assetId");
        String counterpartyName = request.getParameter("counterparty");
        int amount = Integer.valueOf(request.getParameter("amount"));

        UUID assetId = UUID.fromString(assetIdString);
        Party counterparty = proxy.partiesFromName(counterpartyName, true).iterator().next();

        proxy.startFlowDynamic(ExchangeAssetForTokenInitiator.class, assetId, counterparty, amount).getReturnValue().get();
        return ResponseEntity.status(HttpStatus.CREATED).body("Asset exchanged for tokens.\n");
    }

    @GetMapping(value = "tokens", produces = "application/json")
    private List<HashMap<String, String>> tokens() {
        List<StateAndRef<TokenState>> states = proxy.vaultQuery(TokenState.class).getStates();

        return states.stream().map(stateAndRef -> {
            TokenState token = stateAndRef.getState().getData();

            HashMap<String, String> map = new HashMap<>();
            map.put("issuer", token.getIssuer().getName().getOrganisation());
            map.put("owner", token.getOwner().getName().getOrganisation());
            map.put("amount", String.valueOf(token.getAmount()));

            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping(value = "assets", produces = "application/json")
    private List<HashMap<String, String>> assets() {
        List<StateAndRef<AssetState>> states = proxy.vaultQuery(AssetState.class).getStates();

        return states.stream().map(stateAndRef -> {
            AssetState asset = stateAndRef.getState().getData();

            HashMap<String, String> map = new HashMap<>();
            map.put("issuer", asset.getIssuer().getName().getOrganisation());
            map.put("owner", asset.getOwner().getName().getOrganisation());
            map.put("description", asset.getDescription());
            map.put("assetid", asset.getLinearId().getId().toString());

            return map;
        }).collect(Collectors.toList());
    }
}