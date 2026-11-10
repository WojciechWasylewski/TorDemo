package org.example.tordemo.exitnode.api;

import org.example.tordemo.exitnode.client.TorExitNodeClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TorNodeCheckerController {

    TorExitNodeClient torExitNodeClient;

    public TorNodeCheckerController(TorExitNodeClient torExitNodeClient) {
        this.torExitNodeClient = torExitNodeClient;
    }

    @RequestMapping(path = "/{ip}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> checkTorExitNode(@PathVariable String ip) {
        torExitNodeClient.fetchExitNodes();

        return ResponseEntity.ok().build();
    }
}
